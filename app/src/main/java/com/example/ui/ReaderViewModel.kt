package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Book
import com.example.data.BookRepository
import com.example.data.Highlight
import com.example.data.PageState
import com.example.network.GeminiApiRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.arabic.ArabicTextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream

class ReaderViewModel(private val repository: BookRepository) : ViewModel() {

    private val renderMutex = Mutex()

    // Document & navigation states
    val books: StateFlow<List<Book>> = repository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentBook = MutableStateFlow<Book?>(null)
    val currentBook: StateFlow<Book?> = _currentBook.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    // Page visual visual rendering
    private val _currentPageBitmap = MutableStateFlow<Bitmap?>(null)
    val currentPageBitmap: StateFlow<Bitmap?> = _currentPageBitmap.asStateFlow()

    // Reactive page data from DB
    private val _currentPageState = MutableStateFlow<PageState?>(null)
    val currentPageState: StateFlow<PageState?> = _currentPageState.asStateFlow()

    private val _currentHighlights = MutableStateFlow<List<Highlight>>(emptyList())
    val currentHighlights: StateFlow<List<Highlight>> = _currentHighlights.asStateFlow()

    // AI Operation states
    private val _isOcrLoading = MutableStateFlow(false)
    val isOcrLoading: StateFlow<Boolean> = _isOcrLoading.asStateFlow()

    private val _ocrError = MutableStateFlow<String?>(null)
    val ocrError: StateFlow<String?> = _ocrError.asStateFlow()

    private val _aiExplanation = MutableStateFlow<String?>(null)
    val aiExplanation: StateFlow<String?> = _aiExplanation.asStateFlow()

    private val _aiTranslation = MutableStateFlow<String?>(null)
    val aiTranslation: StateFlow<String?> = _aiTranslation.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Native PDF reader internals
    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pageStateCollectorJob: Job? = null
    private var highlightsCollectorJob: Job? = null
    private var autoOcrJob: Job? = null

    /**
     * Set up default sample book if first run
     */
    fun setupSampleBook(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            if (books.value.none { it.uri == "sample" }) {
                try {
                    val file = PdfSampleGenerator.getSamplePdfFile(context)
                    // Obtain page count
                    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val tempRenderer = PdfRenderer(pfd)
                    val count = tempRenderer.pageCount
                    tempRenderer.close()
                    pfd.close()

                    repository.insertBook(
                        Book(
                            uri = "sample",
                            title = "العِلمُ النَّافِع (Scanned Arabic Primer)",
                            totalPages = count
                        )
                    )
                } catch (e: Exception) {
                    Log.e("ReaderViewModel", "Failed to generate sample: ${e.message}")
                }
            }
        }
    }

    /**
     * Copy user-imported PDF safely to local file system and register it in database
     */
    fun importUserPdf(uri: Uri, filename: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val outputName = "imported_${System.currentTimeMillis()}_${filename.replace(" ", "_")}"
                val file = File(context.filesDir, outputName)
                
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }

                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val tempRenderer = PdfRenderer(pfd)
                val count = tempRenderer.pageCount
                tempRenderer.close()
                pfd.close()

                val newBook = Book(
                    uri = file.absolutePath,
                    title = filename,
                    totalPages = count
                )
                repository.insertBook(newBook)
            } catch (e: Exception) {
                Log.e("ReaderViewModel", "Error importing PDF: ${e.message}")
            }
        }
    }

    /**
     * Delete a book and clean up its file
     */
    fun deleteBook(book: Book) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBook(book.id)
            if (book.uri != "sample") {
                try {
                    val file = File(book.uri)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e("ReaderViewModel", "Error deleting physical file: ${e.message}")
                }
            }
            if (_currentBook.value?.id == book.id) {
                closeCurrentBook()
            }
        }
    }

    /**
     * Close the currently open book and release file resources
     */
    fun closeCurrentBook() {
        cleanupRenderer()
        _currentBook.value = null
        _currentPageIndex.value = 0
        _currentPageBitmap.value = null
        _currentPageState.value = null
        _currentHighlights.value = emptyList()
        _aiExplanation.value = null
        _aiTranslation.value = null
    }

    /**
     * Switch current book
     */
    fun openBook(book: Book, context: Context) {
        cleanupRenderer()
        _currentBook.value = book
        _currentPageIndex.value = 0
        _currentPageBitmap.value = null
        _aiExplanation.value = null
        _aiTranslation.value = null
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = if (book.uri == "sample") {
                    PdfSampleGenerator.getSamplePdfFile(context)
                } else {
                    File(book.uri)
                }

                if (!file.exists()) {
                    Log.e("ReaderViewModel", "PDF file does not exist: ${file.path}")
                    return@launch
                }

                fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                fileDescriptor?.let { fd ->
                    pdfRenderer = PdfRenderer(fd)
                    loadPageAndData(0, context)
                }
            } catch (e: Exception) {
                Log.e("ReaderViewModel", "Failed to load book renderer: ${e.message}")
            }
        }
    }

    fun nextPage(context: Context) {
        val renderer = pdfRenderer ?: return
        val current = _currentPageIndex.value
        if (current + 1 < renderer.pageCount) {
            loadPageAndData(current + 1, context)
        }
    }

    fun prevPage(context: Context) {
        val current = _currentPageIndex.value
        if (current - 1 >= 0) {
            loadPageAndData(current - 1, context)
        }
    }

    fun goToPage(index: Int, context: Context) {
        val renderer = pdfRenderer ?: return
        if (index in 0 until renderer.pageCount) {
            loadPageAndData(index, context)
        }
    }

    private fun loadPageAndData(pageIndex: Int, context: Context) {
        _currentPageIndex.value = pageIndex
        _currentPageBitmap.value = null
        _aiExplanation.value = null
        _aiTranslation.value = null
        _ocrError.value = null

        // Render PDF page Bitmap asynchronously
        renderBitmap(pageIndex)

        // Bind Live Database flows for page states (Notes/Highlights)
        val bookId = _currentBook.value?.id ?: return
        
        pageStateCollectorJob?.cancel()
        pageStateCollectorJob = viewModelScope.launch(Dispatchers.IO) {
            repository.getPageStateFlow(bookId, pageIndex).collect { state ->
                _currentPageState.value = state
            }
        }

        highlightsCollectorJob?.cancel()
        highlightsCollectorJob = viewModelScope.launch(Dispatchers.IO) {
            repository.getHighlightsForPage(bookId, pageIndex).collect { items ->
                _currentHighlights.value = items
            }
        }

        // Automatic OCR scan trigger with active debounce and cancellation on page change
        autoOcrJob?.cancel()
        autoOcrJob = viewModelScope.launch(Dispatchers.Main) {
            // Debounce for 1200ms to prevent spamming Gemini API with 429 rate limit during page flipping
            kotlinx.coroutines.delay(1200)
            if (_currentPageIndex.value != pageIndex) return@launch // page index changed, abort

            val state = withContext(Dispatchers.IO) { repository.getPageState(bookId, pageIndex) }
            if (state == null || state.extractedText.isNullOrBlank()) {
                // Wait for the page bitmap to be generated/ready
                for (i in 0..10) { // check for up to 500ms
                    if (_currentPageIndex.value != pageIndex) return@launch
                    if (_currentPageBitmap.value != null) {
                        runOcrOnCurrentPage()
                        break
                    }
                    kotlinx.coroutines.delay(50)
                }
            }
        }
    }

    private fun renderBitmap(pageIndex: Int) {
        val renderer = pdfRenderer ?: return
        viewModelScope.launch(Dispatchers.IO) {
            renderMutex.withLock {
                var page: PdfRenderer.Page? = null
                try {
                    page = renderer.openPage(pageIndex)
                    // Safe 1.5f scaling to optimize memory and keep rendering pristine
                    val scaleFactor = 1.5f
                    val width = (page.width * scaleFactor).toInt().coerceAtLeast(1)
                    val height = (page.height * scaleFactor).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    
                    _currentPageBitmap.value = bitmap
                } catch (t: Throwable) {
                    Log.e("ReaderViewModel", "PDF render block failed: ${t.message}", t)
                } finally {
                    try {
                        page?.close()
                    } catch (e: Exception) {
                        Log.e("ReaderViewModel", "Error closing pdf page: ${e.message}")
                    }
                }
            }
        }
    }

    private fun getSamplePageOcrJson(pageNum: Int): String {
        return when (pageNum) {
            0 -> """
                {
                  "lines": [
                    { "text": "كِتَابُ العِلْمِ وَالأَدَبِ", "box_2d": [280, 200, 312, 800] },
                    { "text": "مَطْبُوعَةُ المَدْرَسَةِ لِلطَّلَبَةِ النُّجَبَاءِ", "box_2d": [365, 150, 395, 850] },
                    { "text": "[ Arabic Scanned-Style PDF Book ]", "box_2d": [575, 100, 605, 900] },
                    { "text": "(Tap 'Extract Text' to run Gemini OCR)", "box_2d": [635, 100, 665, 900] }
                  ]
                }
            """.trimIndent()
            1 -> """
                {
                  "lines": [
                    { "text": "بَابُ مَعْرِفَةِ عَلَامَاتِ الإِعْرَابِ", "box_2d": [105, 100, 140, 900] },
                    { "text": "لِلرَّفْعِ أَرْبَعُ عَلَامَاتٍ: الضَّمَّةُ، وَالوَاوُ، وَالأَلِفُ، وَالنُّونُ.", "box_2d": [188, 50, 222, 950] },
                    { "text": "فَأَمَّا الضَّمَّةُ فَتَكُونُ عَلَامَةً لِلرَّفْعِ فِي أَرْبَعَةِ مَوَاضِعَ:", "box_2d": [241, 50, 275, 950] },
                    { "text": "الِاسْمِ المُفْرَدِ، وَجَمْعِ التَّكْسِيرِ، وَجَمْعِ المُؤَنَّثِ السَّالِمِ،", "box_2d": [294, 50, 328, 950] },
                    { "text": "وَالفِعْلِ المُضَارِعِ الَّذِي لَمْ يَتَّصِلْ بِآخِرِهِ شَيْءٌ.", "box_2d": [347, 50, 381, 950] },
                    { "text": "وَأَمَّا الوَاوُ فَتَكُونُ عَلَامَةً لِلرَّفْعِ فِي مَوْضِعَيْنِ:", "box_2d": [400, 50, 434, 950] },
                    { "text": "فِي جَمْعِ المُذَكَّرِ السَّالِمِ، وَفِي الأَسْمَاءِ الخَمْسَةِ.", "box_2d": [453, 50, 487, 950] },
                    { "text": "وَهِيَ: أَبُوكَ وَأَخُوكَ وَحَمُوكَ وَفُوكَ وَذُو مَالٍ.", "box_2d": [506, 50, 540, 950] }
                  ]
                }
            """.trimIndent()
            2 -> """
                {
                  "lines": [
                    { "text": "حِكْمَة ومَوْعِظَة", "box_2d": [118, 100, 150, 900] },
                    { "text": "العِلْمُ صَيْدٌ وَالكِتَابَةُ قَيْدُهُ، قَيِّدْ صُيُودَكَ بِالحِبَالِ الوَاثِقَةِ.", "box_2d": [212, 30, 246, 970] },
                    { "text": "مَنْ لَمْ يَذُقْ مُرَّ التَّعَلُّمِ سَاعَةً، تَجَرَّعَ ذُلَّ الجَهْلِ طُولَ حَيَاتِهِ.", "box_2d": [265, 30, 299, 970] },
                    { "text": "تَعَلَّمْ فَلَيْسَ المَرْءُ يُولَدُ عَالِمًا، وَلَيْسَ أَخُو عِلْمٍ كَمَنْ هُوَ جَاهِلٌ.", "box_2d": [318, 30, 352, 970] },
                    { "text": "ترجمہ اردو (Urdu Translation Example):", "box_2d": [424, 150, 458, 850] },
                    { "text": "علم ایک شکار ہے اور لکھنا اس کی قید ہے۔ اپنے شکار کو مضبوط رسیوں سے باندھ لو۔", "box_2d": [477, 30, 511, 970] },
                    { "text": "جس نے ایک لمحہ کے لیے سیکھنے کی کڑواہٹ کا مزہ نہیں چکھا، وہ ساری زندگی جہالت کی ذلت پیتا رہے گا۔", "box_2d": [530, 30, 564, 970] }
                  ]
                }
            """.trimIndent()
            else -> ""
        }
    }

    /**
     * Extracts text using ML Kit on-device OCR (Arabic + Latin script, no API key needed)
     */
    fun runOcrOnCurrentPage() {
        val bitmap = _currentPageBitmap.value ?: return
        val book = _currentBook.value ?: return
        val bookId = book.id
        val pageNum = _currentPageIndex.value

        viewModelScope.launch {
            _isOcrLoading.value = true
            _ocrError.value = null
            try {
                if (book.uri == "sample") {
                    val sampleOcr = getSamplePageOcrJson(pageNum)
                    if (sampleOcr.isNotEmpty()) {
                        repository.saveExtractedText(bookId, pageNum, sampleOcr)
                        return@launch
                    }
                }

                val resultJson = withContext(Dispatchers.IO) {
                    runMlKitOcr(bitmap)
                }
                repository.saveExtractedText(bookId, pageNum, resultJson)
            } catch (e: Exception) {
                _ocrError.value = "OCR Failed: ${e.localizedMessage}"
            } finally {
                _isOcrLoading.value = false
            }
        }
    }

    /**
     * Runs ML Kit Arabic + Latin text recognition on a bitmap and returns JSON
     * in the same format the rest of the app expects:
     * { "lines": [ { "text": "...", "box_2d": [ymin, xmin, ymax, xmax] } ] }
     */
    private suspend fun runMlKitOcr(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        val imgW = bitmap.width.toFloat().coerceAtLeast(1f)
        val imgH = bitmap.height.toFloat().coerceAtLeast(1f)

        // Run Arabic recognizer first (handles Arabic & Urdu script)
        val arabicRecognizer = TextRecognition.getClient(ArabicTextRecognizerOptions.Builder().build())
        val arabicResult = arabicRecognizer.process(image).await()

        // Also run Latin recognizer for any Roman/English text on the page
        val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val latinResult = latinRecognizer.process(image).await()

        val lines = mutableListOf<String>()

        // Collect all text blocks from both recognizers, normalizing bounding boxes to 0-1000
        for (result in listOf(arabicResult, latinResult)) {
            for (block in result.textBlocks) {
                for (line in block.lines) {
                    val text = line.text.trim()
                    if (text.isEmpty()) continue
                    val box = line.boundingBox
                    if (box != null) {
                        val ymin = ((box.top / imgH) * 1000).toInt().coerceIn(0, 1000)
                        val xmin = ((box.left / imgW) * 1000).toInt().coerceIn(0, 1000)
                        val ymax = ((box.bottom / imgH) * 1000).toInt().coerceIn(0, 1000)
                        val xmax = ((box.right / imgW) * 1000).toInt().coerceIn(0, 1000)
                        val escaped = text.replace("\"", "\\\"").replace("\n", " ")
                        lines.add("""    { "text": "$escaped", "box_2d": [$ymin, $xmin, $ymax, $xmax] }""")
                    } else {
                        val escaped = text.replace("\"", "\\\"").replace("\n", " ")
                        lines.add("""    { "text": "$escaped", "box_2d": [0, 0, 100, 1000] }""")
                    }
                }
            }
        }

        arabicRecognizer.close()
        latinRecognizer.close()

        return if (lines.isEmpty()) {
            """{ "lines": [] }"""
        } else {
            "{\n  \"lines\": [\n${lines.joinToString(",\n")}\n  ]\n}"
        }
    }

    /**
     * Saves notes associated with currently open page index
     */
    fun savePageNote(note: String) {
        val bookId = _currentBook.value?.id ?: return
        val pageNum = _currentPageIndex.value
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveNoteText(bookId, pageNum, note)
        }
    }

    /**
     * Save an extracted text highlight in local database with optional comment/sticky note
     */
    fun saveTextHighlight(text: String, colorHex: String, comment: String? = null, id: Long = 0) {
        val bookId = _currentBook.value?.id ?: return
        val pageNum = _currentPageIndex.value

        viewModelScope.launch(Dispatchers.IO) {
            val highlight = Highlight(
                id = id,
                bookId = bookId,
                pageNumber = pageNum,
                selectedText = text.trim(),
                colorHex = colorHex,
                comment = comment
            )
            repository.addHighlight(highlight)
        }
    }

    /**
     * Delete an existing highlight
     */
    fun deleteTextHighlight(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeHighlight(id)
        }
    }

    /**
     * Request classical grammar or word definition analysis from Gemini API
     */
    fun explainSelectedText(text: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiExplanation.value = null
            try {
                val explanation = GeminiApiRepository.explainPhrase(text)
                _aiExplanation.value = explanation
            } catch (e: Exception) {
                _aiExplanation.value = "Unable to fetch explanation: ${e.localizedMessage}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    /**
     * Translates a selected phrase
     */
    fun translateSelectedText(text: String, targetLang: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiTranslation.value = null
            try {
                val translation = GeminiApiRepository.translateText(text, targetLang)
                _aiTranslation.value = translation
            } catch (e: Exception) {
                _aiTranslation.value = "Translation error: ${e.localizedMessage}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun clearExplanationAndTranslation() {
        _aiExplanation.value = null
        _aiTranslation.value = null
    }

    private fun cleanupRenderer() {
        try {
            pdfRenderer?.close()
        } catch (e: Exception) {
            // Ignored closure exception
        }
        try {
            fileDescriptor?.close()
        } catch (e: Exception) {
            // Ignored closure exception
        }
        pdfRenderer = null
        fileDescriptor = null
        pageStateCollectorJob?.cancel()
        highlightsCollectorJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        cleanupRenderer()
    }

    // Single central viewmodel factory
    class Factory(private val repository: BookRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReaderViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
