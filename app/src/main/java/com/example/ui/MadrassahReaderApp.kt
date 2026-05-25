package com.example.ui

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.Book
import com.example.data.Highlight
import com.example.data.PageState
import com.example.speech.SpeechRecognizerManager
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.Canvas

// Natural Tones styling color palette
val IvoryBackground = Color(0xFFFDF8F3) // Cozy warm linen/sand
val ClassicPaper = Color(0xFFF5EFE9) // Sand paper shade
val ScholarPrimary = Color(0xFF7A5933) // Rich coffee/wood brown
val ScholarSecondary = Color(0xFF857365) // Muted warm earthy brown-gray
val AntiqueIvory = Color(0xFFFDF8F3) // Natural Ivory
val VerseGreen = Color(0xFF516F52) // Soft natural sage green
val CustomGold = Color(0xFFC8B59E) // Warm sand/beige muted accent
val DarkText = Color(0xFF201A17) // Deep dark warm charcoal
val NaturalSand = Color(0xFFEADDD2) // Soft sand/beige border accent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MadrassahReaderApp(viewModel: ReaderViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val currentBook by viewModel.currentBook.collectAsState()
    val books by viewModel.books.collectAsState()

    // Initialize sample book on start
    LaunchedEffect(Unit) {
        viewModel.setupSampleBook(context)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AntiqueIvory
    ) {
        AnimatedContent(
            targetState = currentBook,
            transitionSpec = {
                fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "ScreenNavigation"
        ) { activeBook ->
            if (activeBook == null) {
                LibraryShelfScreen(
                    books = books,
                    onBookSelect = { book -> viewModel.openBook(book, context) },
                    onBookDelete = { book -> viewModel.deleteBook(book) },
                    onImportPdf = { uri, name -> viewModel.importUserPdf(uri, name, context) }
                )
            } else {
                BookReaderWorkspace(
                    book = activeBook,
                    viewModel = viewModel,
                    onBackToLibrary = { viewModel.closeCurrentBook() }
                )
            }
        }
    }
}

/**
 * 1. Library Shelf Screen (Dashboard)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryShelfScreen(
    books: List<Book>,
    onBookSelect: (Book) -> Unit,
    onBookDelete: (Book) -> Unit,
    onImportPdf: (Uri, String) -> Unit
) {
    val context = LocalContext.current
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                val contentResolver = context.contentResolver
                val filename = getContentFileName(contentResolver, it) ?: "مجهول_كتاب.pdf"
                onImportPdf(it, filename)
                Toast.makeText(context, "إضافة الكتاب بنجاح!", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "المَكْتَبَةُ المِنْهَاجِيَّة",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 25.sp,
                            color = ScholarPrimary
                        )
                        Text(
                            text = "Al-Minhaj PDF Reader & Researcher",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = ScholarSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AntiqueIvory
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePicker.launch(arrayOf("application/pdf")) },
                containerColor = ScholarPrimary,
                contentColor = AntiqueIvory,
                modifier = Modifier.testTag("import_pdf_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Import Book")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "إضافة كتاب PDF", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        containerColor = AntiqueIvory
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AntiqueIvory, IvoryBackground)
                    )
                )
        ) {
            // Introductory Card
            Card(
                colors = CardDefaults.cardColors(containerColor = ClassicPaper),
                border = BorderStroke(1.dp, NaturalSand),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(ScholarPrimary, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Academic Books icon",
                            tint = AntiqueIvory,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "مرحباً بكم يا طالب العلم",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = ScholarPrimary
                        )
                        Text(
                            text = "Welcome Scholar. Study classical Arabic/Urdu texts with AI OCR transcriptions, paragraph instant-highlights, and voice-to-text Urdu/Arabic page notes.",
                            fontSize = 12.sp,
                            color = DarkText.copy(alpha = 0.8f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Text(
                text = "كتبك الدراسية (Your Bookshelf)",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = ScholarPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            if (books.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.LibraryBooks,
                            contentDescription = "Empty Shelf",
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No books imported yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(books) { book ->
                        BookShelfItem(
                            book = book,
                            onSelect = { onBookSelect(book) },
                            onDelete = { onBookDelete(book) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookShelfItem(
    book: Book,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("book_item_${book.id}"),
        colors = CardDefaults.cardColors(containerColor = ClassicPaper),
        border = BorderStroke(1.dp, NaturalSand)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (book.uri == "sample") Icons.Default.AutoStories else Icons.Default.PictureAsPdf,
                contentDescription = "Book icon",
                tint = if (book.uri == "sample") VerseGreen else ScholarPrimary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = DarkText,
                    maxLines = 1
                )
                Text(
                    text = if (book.totalPages > 0) "${book.totalPages} صفحات (Pages)" else "جاري التحميل...",
                    fontSize = 12.sp,
                    color = ScholarSecondary.copy(alpha = 0.8f)
                )
            }
            IconButton(onClick = { showConfirmDelete = true }) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Book",
                    tint = Color(0xFFA13333)
                )
            }
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("حذف الكتاب؟ (Delete Book?)", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${book.title}'? This will erase all took notes and highlights as well.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showConfirmDelete = false
                    }
                ) {
                    Text("حذف (Delete)", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("إلغاء (Cancel)")
                }
            }
        )
    }
}

/**
 * 2. Book Reader Workspace Screen (Workspace Editor split/tabbed)
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookReaderWorkspace(
    book: Book,
    viewModel: ReaderViewModel,
    onBackToLibrary: () -> Unit
) {
    val context = LocalContext.current
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    val pageBitmap by viewModel.currentPageBitmap.collectAsState()
    val pageState by viewModel.currentPageState.collectAsState()
    val highlights by viewModel.currentHighlights.collectAsState()

    // Gemini states
    val isOcrLoading by viewModel.isOcrLoading.collectAsState()
    val ocrError by viewModel.ocrError.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiExplanation by viewModel.aiExplanation.collectAsState()
    val aiTranslation by viewModel.aiTranslation.collectAsState()

    // Tab index: 0 = Research and Highlights, 1 = Handwritten & Voice Notes
    var selectedTab by remember { mutableStateOf(0) }

    // Floating action dialog states on tapped paragraph
    var selectedParagraphForAction by remember { mutableStateOf<String?>(null) }
    
    // Page navigation Go-To popup dialog state
    var showGoToPageDialog by remember { mutableStateOf(false) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 760
    var isPaneVisible by remember(isWideScreen) { mutableStateOf(isWideScreen) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = book.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AntiqueIvory,
                            maxLines = 1
                        )
                        Text(
                            text = "صفحة ${currentPageIndex + 1} من ${book.totalPages}",
                            fontSize = 12.sp,
                            color = AntiqueIvory.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToLibrary) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AntiqueIvory
                        )
                    }
                },
                actions = {
                    // Toggle Workspace Sidebar / Notes View Button
                    IconButton(
                        onClick = { isPaneVisible = !isPaneVisible }
                    ) {
                        Icon(
                            imageVector = if (isPaneVisible) Icons.Default.MenuBook else Icons.Default.VerticalSplit,
                            contentDescription = if (isPaneVisible) "عرض صفحة كاملة (Fullscreen)" else "عرض الأدوات (Show Tools)",
                            tint = AntiqueIvory
                        )
                    }
                    TextButton(
                        onClick = { isPaneVisible = !isPaneVisible }
                    ) {
                        Text(
                            text = if (isPaneVisible) "قراءة (Read)" else "الأدوات (Tools)",
                            color = AntiqueIvory,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { 
                            isPaneVisible = true
                            viewModel.runOcrOnCurrentPage() 
                        },
                        enabled = !isOcrLoading && pageBitmap != null
                    ) {
                        if (isOcrLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AntiqueIvory, strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "AI OCR Scan Text",
                                tint = AntiqueIvory
                            )
                        }
                    }
                    TextButton(
                        onClick = { 
                            isPaneVisible = true
                            viewModel.runOcrOnCurrentPage() 
                        },
                        enabled = !isOcrLoading
                    ) {
                        Text(
                            text = "استخراج النص",
                            color = AntiqueIvory,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ScholarPrimary
                )
            )
        },
        bottomBar = {
            // Adaptive Page Navigation Slider
            Surface(
                color = ScholarPrimary,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.prevPage(context) },
                        enabled = currentPageIndex > 0
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Page",
                            tint = if (currentPageIndex > 0) AntiqueIvory else AntiqueIvory.copy(alpha = 0.3f)
                        )
                    }

                    // Tactile clickable Go-To page button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(BorderStroke(1.dp, AntiqueIvory.copy(alpha = 0.4f)), RoundedCornerShape(20.dp))
                            .clickable { showGoToPageDialog = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Go To Page",
                            tint = AntiqueIvory,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "انتقال إلى صفحة  ${currentPageIndex + 1} / ${book.totalPages}",
                            color = AntiqueIvory,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            fontSize = 13.sp
                        )
                    }

                    IconButton(
                        onClick = { viewModel.nextPage(context) },
                        enabled = currentPageIndex + 1 < book.totalPages
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Page",
                            tint = if (currentPageIndex + 1 < book.totalPages) AntiqueIvory else AntiqueIvory.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        },
        containerColor = AntiqueIvory
    ) { innerPadding ->
        // Direct Split Layout (Display scanned image on the left, interactive tools on the right)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 760.dp

            if (isWideScreen) {
                // Side-by-Side Canvas View on Tablet/Wide Displays
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .border(BorderStroke(1.dp, NaturalSand))
                            .background(ClassicPaper)
                            .padding(8.dp)
                    ) {
                        PdfPageImageDisplay(
                            bitmap = pageBitmap,
                            pageState = pageState,
                            highlights = highlights,
                            onSaveHighlight = { text, color, comment, hId ->
                                viewModel.saveTextHighlight(text, color, comment, hId)
                            },
                            onDeleteHighlight = { hId ->
                                viewModel.deleteTextHighlight(hId)
                            },
                            onExplainText = { text ->
                                isPaneVisible = true
                                viewModel.explainSelectedText(text)
                            },
                            onTranslateText = { text ->
                                isPaneVisible = true
                                viewModel.translateSelectedText(text, "Urdu")
                            }
                        )
                    }
                    if (isPaneVisible) {
                        VerticalDivider(color = NaturalSand, thickness = 1.dp)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            WorkspaceInteractivePane(
                                viewModel = viewModel,
                                selectedTab = selectedTab,
                                onTabChange = { selectedTab = it },
                                pageState = pageState,
                                highlights = highlights,
                                isOcrLoading = isOcrLoading,
                                ocrError = ocrError,
                                isAiLoading = isAiLoading,
                                aiExplanation = aiExplanation,
                                aiTranslation = aiTranslation,
                                onParagraphClick = { selectedParagraphForAction = it }
                            )
                        }
                    }
                }
            } else {
                // Adaptive Vertical/Tabbed view on Mobile Screen widths
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = if (isPaneVisible) {
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(ClassicPaper)
                                .padding(4.dp)
                        } else {
                            Modifier
                                .fillMaxSize()
                                .background(ClassicPaper)
                                .padding(4.dp)
                        }
                    ) {
                        PdfPageImageDisplay(
                            bitmap = pageBitmap,
                            pageState = pageState,
                            highlights = highlights,
                            onSaveHighlight = { text, color, comment, hId ->
                                viewModel.saveTextHighlight(text, color, comment, hId)
                            },
                            onDeleteHighlight = { hId ->
                                viewModel.deleteTextHighlight(hId)
                            },
                            onExplainText = { text ->
                                isPaneVisible = true
                                viewModel.explainSelectedText(text)
                            },
                            onTranslateText = { text ->
                                isPaneVisible = true
                                viewModel.translateSelectedText(text, "Urdu")
                            }
                        )
                    }
                    if (isPaneVisible) {
                        HorizontalDivider(color = NaturalSand, thickness = 1.dp)
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxWidth()
                        ) {
                            WorkspaceInteractivePane(
                                viewModel = viewModel,
                                selectedTab = selectedTab,
                                onTabChange = { selectedTab = it },
                                pageState = pageState,
                                highlights = highlights,
                                isOcrLoading = isOcrLoading,
                                ocrError = ocrError,
                                isAiLoading = isAiLoading,
                                aiExplanation = aiExplanation,
                                aiTranslation = aiTranslation,
                                onParagraphClick = { selectedParagraphForAction = it }
                            )
                        }
                    }
                }
            }
        }
    }

    // Go-To Page popup dialog
    if (showGoToPageDialog) {
        var pageInputText by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        
        AlertDialog(
            onDismissRequest = { showGoToPageDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val inputNum = pageInputText.toIntOrNull()
                        if (inputNum != null && inputNum in 1..book.totalPages) {
                            viewModel.goToPage(inputNum - 1, context)
                            showGoToPageDialog = false
                        } else {
                            errorMessage = "يرجى تحديد رقم صفحة بين 1 و ${book.totalPages}"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ScholarPrimary)
                ) {
                    Text("انتقال", color = AntiqueIvory, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoToPageDialog = false }) {
                    Text("إلغاء", color = ScholarSecondary, fontFamily = FontFamily.Serif)
                }
            },
            title = {
                Text(
                    text = "الانتقال إلى صفحة",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = ScholarPrimary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "أدخل رقم الصفحة المطلوب (من 1 إلى ${book.totalPages}):",
                        fontSize = 14.sp,
                        color = ScholarSecondary,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pageInputText,
                        onValueChange = { 
                            pageInputText = it.filter { char -> char.isDigit() }
                            errorMessage = null
                        },
                        placeholder = { Text("مثال: 5", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScholarPrimary,
                            unfocusedBorderColor = NaturalSand,
                            cursorColor = ScholarPrimary
                        ),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right)
                    )
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                    }
                }
            },
            containerColor = AntiqueIvory,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Paragraph interaction action sheet dialogue
    selectedParagraphForAction?.let { paragraph ->
        Dialog(onDismissRequest = { selectedParagraphForAction = null }) {
            val wordsInPara = remember(paragraph) {
                paragraph.split("\\s+".toRegex()).filter { it.isNotBlank() }
            }
            var pStartIndex by remember(paragraph) { mutableStateOf(0) }
            var pEndIndex by remember(paragraph) { mutableStateOf(maxOf(0, paragraph.split("\\s+".toRegex()).filter { it.isNotBlank() }.size - 1)) }
            
            val activeParaText = remember(paragraph, pStartIndex, pEndIndex, wordsInPara) {
                if (wordsInPara.isEmpty()) {
                    ""
                } else {
                    val s = pStartIndex.coerceIn(0, wordsInPara.size - 1)
                    val e = pEndIndex.coerceIn(s, wordsInPara.size - 1)
                    wordsInPara.subList(s, e + 1).joinToString(" ")
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = AntiqueIvory),
                border = BorderStroke(1.dp, Color(0xFFD4C8B6)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "خيارات البحث والتعليم (Research Tools)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ScholarPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Clipped preview of target text chunk
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ClassicPaper, RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, NaturalSand), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = activeParaText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Serif,
                            color = DarkText,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Word range selector chips inside Dialog
                    if (wordsInPara.size > 1) {
                        Text(
                            text = "تحديد جزئي بالكلمات (Select words):",
                            fontSize = 11.sp,
                            color = ScholarSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Left
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
                        ) {
                            wordsInPara.forEachIndexed { i, word ->
                                val isSelected = i in pStartIndex..pEndIndex
                                val chipBg = if (isSelected) ScholarPrimary else ClassicPaper
                                val chipBorderCol = if (isSelected) ScholarPrimary else NaturalSand
                                val chipTextCol = if (isSelected) AntiqueIvory else ScholarPrimary
                                
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = chipBg,
                                    border = BorderStroke(1.dp, chipBorderCol),
                                    modifier = Modifier.clickable {
                                        if (pStartIndex == pEndIndex) {
                                            if (i < pStartIndex) {
                                                pStartIndex = i
                                            } else {
                                                pEndIndex = i
                                            }
                                        } else {
                                            pStartIndex = i
                                            pEndIndex = i
                                        }
                                    }
                                ) {
                                    Text(
                                        text = word,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Serif,
                                        color = chipTextCol,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Column actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Highlight options
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تعليم بالنص (Highlight):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = ScholarSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.saveTextHighlight(activeParaText, "#FFF9C4") // Yellow
                                        selectedParagraphForAction = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF59D)),
                                    modifier = Modifier.height(36.dp).weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("أصفر", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.saveTextHighlight(activeParaText, "#C8E6C9") // Green
                                        selectedParagraphForAction = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA5D6A7)),
                                    modifier = Modifier.height(36.dp).weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("أخضر", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.saveTextHighlight(activeParaText, "#BBDEFB") // Blue
                                        selectedParagraphForAction = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90CAF9)),
                                    modifier = Modifier.height(36.dp).weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("أزرق", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = NaturalSand)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Research AI utilities
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isPaneVisible = true
                                viewModel.explainSelectedText(activeParaText)
                                selectedParagraphForAction = null
                            },
                            border = BorderStroke(1.dp, VerseGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Explain", tint = VerseGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تفسير (Explain)", color = VerseGreen, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                isPaneVisible = true
                                viewModel.translateSelectedText(activeParaText, "Urdu")
                                selectedParagraphForAction = null
                            },
                            border = BorderStroke(1.dp, CustomGold),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = "Translate", tint = CustomGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ترجمہ (Urdu)", color = CustomGold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick copy button
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Madrassah Text", activeParaText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ النص!", Toast.LENGTH_SHORT).show()
                                selectedParagraphForAction = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ (Copy)", fontSize = 12.sp)
                        }

                        // Close button
                        TextButton(
                            onClick = { selectedParagraphForAction = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء (Dismiss)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

data class WordToken(
    val text: String,
    val yMin: Float,
    val xMin: Float,
    val yMax: Float,
    val xMax: Float,
    val startIndex: Int,
    val endIndex: Int,
    val parentLine: DetectedLine,
    val isRtl: Boolean
)

data class DetectedLine(
    val text: String,
    val box2d: List<Int>? = null
)

fun parseDetectedLines(rawText: String?): List<DetectedLine> {
    if (rawText == null || rawText.isBlank()) return emptyList()
    val trimmed = rawText.trim()
    var jsonText = trimmed
    
    // Attempt markdown json extraction block if wrapped
    if (jsonText.contains("```json")) {
        val start = jsonText.indexOf("```json") + 7
        val end = jsonText.indexOf("```", start)
        if (end > start) {
            jsonText = jsonText.substring(start, end).trim()
        }
    } else if (jsonText.contains("```")) {
        val start = jsonText.indexOf("```") + 3
        val end = jsonText.indexOf("```", start)
        if (end > start) {
            jsonText = jsonText.substring(start, end).trim()
        }
    }

    if (jsonText.startsWith("{") && jsonText.contains("lines")) {
        try {
            val jsonObject = org.json.JSONObject(jsonText)
            val linesArray = jsonObject.getJSONArray("lines")
            val result = mutableListOf<DetectedLine>()
            for (i in 0 until linesArray.length()) {
                val lineObj = linesArray.getJSONObject(i)
                val text = lineObj.optString("text", "")
                val boxArray = lineObj.optJSONArray("box_2d")
                val boxList = if (boxArray != null) {
                    List(boxArray.length()) { idx -> boxArray.getInt(idx) }
                } else null
                
                if (text.isNotBlank()) {
                    result.add(DetectedLine(text, boxList))
                }
            }
            return result
        } catch (e: Exception) {
            android.util.Log.e("MadrassahReaderApp", "Error parsing line coordinates JSON: ${e.message}")
        }
    }

    // Fallback: split by newlines for plain-text entries (backward compatibility)
    return trimmed.split("\n")
        .filter { it.isNotBlank() }
        .map { DetectedLine(it) }
}

/**
 * PDF Visual Image view container supporting coordinate-based text highlights overlays and in-context floating tool tooltip
 */
@Composable
fun PdfPageImageDisplay(
    bitmap: Bitmap?,
    pageState: PageState?,
    highlights: List<Highlight>,
    onSaveHighlight: (text: String, colorHex: String, comment: String?, id: Long) -> Unit,
    onDeleteHighlight: (Long) -> Unit,
    onExplainText: (String) -> Unit,
    onTranslateText: (String) -> Unit
) {
    val context = LocalContext.current
    var activeSelectionText by remember(pageState) { mutableStateOf<String?>(null) }
    var activeSelectionMinY by remember { mutableStateOf(0.5f) }

    val wordsList = remember(activeSelectionText) {
        activeSelectionText?.split("\\s+".toRegex())?.filter { it.isNotBlank() } ?: emptyList()
    }
    var wordStartIndex by remember(activeSelectionText) { mutableStateOf(0) }
    var wordEndIndex by remember(activeSelectionText) {
        mutableStateOf(maxOf(0, (activeSelectionText?.split("\\s+".toRegex())?.filter { it.isNotBlank() }?.size ?: 1) - 1))
    }

    val finalSelectedText = remember(activeSelectionText, wordStartIndex, wordEndIndex, wordsList) {
        if (wordsList.isEmpty()) {
            ""
        } else {
            val s = wordStartIndex.coerceIn(0, wordsList.size - 1)
            val e = wordEndIndex.coerceIn(s, wordsList.size - 1)
            wordsList.subList(s, e + 1).joinToString(" ")
        }
    }

    var editingHighlightId by remember { mutableStateOf<Long?>(null) }
    var editingHighlightColorHex by remember { mutableStateOf("#FFF9C4") }
    var stickyNoteTextInput by remember { mutableStateOf("") }
    var showStickyNoteEditor by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(aspectRatio)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "PDF Scanned Page Plate",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                val detectedLines = remember(pageState?.extractedText) {
                    parseDetectedLines(pageState?.extractedText)
                }

                val wordTokens = remember(detectedLines) {
                    val tokens = mutableListOf<WordToken>()
                    detectedLines.forEach { line ->
                        val box = line.box2d
                        if (box != null && box.size == 4) {
                            val yMinL = (box[0] / 1000f).coerceIn(0f, 1f)
                            val xMinL = (box[1] / 1000f).coerceIn(0f, 1f)
                            val yMaxL = (box[2] / 1000f).coerceIn(0f, 1f)
                            val xMaxL = (box[3] / 1000f).coerceIn(0f, 1f)
                            val widthL = xMaxL - xMinL
                            
                            val lineText = line.text
                            val isRtl = lineText.any { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' || it in '\u08A0'..'\u08FF' || it in '\uFB50'..'\uFDFF' || it in '\uFE70'..'\uFEFF' }
                            val totalChars = lineText.length
                            
                            val wordPattern = "\\S+".toRegex()
                            wordPattern.findAll(lineText).forEach { matchResult ->
                                val word = matchResult.value
                                val startIdx = matchResult.range.first
                                val endIdx = matchResult.range.last + 1
                                
                                val fStart = startIdx.toFloat() / totalChars
                                val fEnd = endIdx.toFloat() / totalChars
                                
                                val (wXMin, wXMax) = if (isRtl) {
                                    Pair(
                                        (xMaxL - widthL * fEnd).coerceIn(0f, 1f),
                                        (xMaxL - widthL * fStart).coerceIn(0f, 1f)
                                    )
                                } else {
                                    Pair(
                                        (xMinL + widthL * fStart).coerceIn(0f, 1f),
                                        (xMinL + widthL * fEnd).coerceIn(0f, 1f)
                                    )
                                }
                                
                                tokens.add(
                                    WordToken(
                                        text = word,
                                        yMin = yMinL,
                                        xMin = wXMin,
                                        yMax = yMaxL,
                                        xMax = wXMax,
                                        startIndex = startIdx,
                                        endIndex = endIdx,
                                        parentLine = line,
                                        isRtl = isRtl
                                    )
                                )
                            }
                        }
                    }
                    tokens
                }

                var dragStartFraction by remember { mutableStateOf<Offset?>(null) }
                var dragCurrentFraction by remember { mutableStateOf<Offset?>(null) }
                var isDragging by remember { mutableStateOf(false) }
                var dragStartToken by remember { mutableStateOf<WordToken?>(null) }
                var dragEndToken by remember { mutableStateOf<WordToken?>(null) }
                
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(wordTokens, highlights) {
                            detectTapGestures { offset ->
                                val xFraction = offset.x / size.width.toFloat()
                                val yFraction = offset.y / size.height.toFloat()
                                
                                var clickedToken = wordTokens.find { t ->
                                    xFraction in t.xMin..t.xMax && yFraction in t.yMin..t.yMax
                                }
                                
                                if (clickedToken == null) {
                                    val clickedLine = detectedLines.find { line ->
                                        val box = line.box2d
                                        if (box != null && box.size == 4) {
                                            val yMin = (box[0] / 1000f).coerceIn(0f, 1f)
                                            val xMin = (box[1] / 1000f).coerceIn(0f, 1f)
                                            val yMax = (box[2] / 1000f).coerceIn(0f, 1f)
                                            val xMax = (box[3] / 1000f).coerceIn(0f, 1f)
                                            xFraction in xMin..xMax && yFraction in yMin..yMax
                                        } else {
                                            false
                                        }
                                    }
                                    if (clickedLine != null) {
                                        clickedToken = wordTokens.find { it.parentLine == clickedLine }
                                    }
                                }
                                
                                if (clickedToken != null) {
                                    val clickedHighlight = highlights.find { h ->
                                        val cleanHighlight = h.selectedText.trim()
                                        val cleanLine = clickedToken.parentLine.text.trim()
                                        if (cleanHighlight.contains(clickedToken.text)) {
                                            cleanLine.contains(cleanHighlight) || cleanHighlight.contains(cleanLine) ||
                                            cleanHighlight.split("\\s+".toRegex()).any { it == clickedToken.text }
                                        } else {
                                            false
                                        }
                                    }
                                    
                                    if (clickedHighlight != null) {
                                        editingHighlightId = clickedHighlight.id
                                        editingHighlightColorHex = clickedHighlight.colorHex
                                        stickyNoteTextInput = clickedHighlight.comment ?: ""
                                        showStickyNoteEditor = !clickedHighlight.comment.isNullOrBlank()
                                        activeSelectionText = clickedHighlight.selectedText
                                    } else {
                                        editingHighlightId = null
                                        editingHighlightColorHex = "#FFF9C4"
                                        stickyNoteTextInput = ""
                                        showStickyNoteEditor = false
                                        activeSelectionText = clickedToken.parentLine.text
                                    }
                                    activeSelectionMinY = clickedToken.yMin
                                }
                            }
                        }
                        .pointerInput(wordTokens, highlights) {
                            val findNearest = { offset: Offset ->
                                var nearest: WordToken? = null
                                var minDist = Float.MAX_VALUE
                                wordTokens.forEach { token ->
                                    val tokenX = (token.xMin + token.xMax) / 2f
                                    val tokenY = (token.yMin + token.yMax) / 2f
                                    val dx = offset.x - tokenX
                                    val dy = offset.y - tokenY
                                    val d = dx * dx + dy * dy * 4f // Weight vertical difference more
                                    if (d < minDist) {
                                        minDist = d
                                        nearest = token
                                    }
                                }
                                nearest
                            }
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val startFraction = Offset(offset.x / size.width.toFloat(), offset.y / size.height.toFloat())
                                    dragStartFraction = startFraction
                                    dragCurrentFraction = startFraction
                                    val t = findNearest(startFraction)
                                    dragStartToken = t
                                    dragEndToken = t
                                    isDragging = true
                                },
                                onDragEnd = {
                                    val st = dragStartToken
                                    val et = dragEndToken
                                    if (st != null && et != null) {
                                        val idxStart = wordTokens.indexOf(st)
                                        val idxEnd = wordTokens.indexOf(et)
                                        if (idxStart != -1 && idxEnd != -1) {
                                            val minIdx = minOf(idxStart, idxEnd)
                                            val maxIdx = maxOf(idxStart, idxEnd)
                                            val selectedTokens = wordTokens.subList(minIdx, maxIdx + 1)
                                            
                                            if (selectedTokens.isNotEmpty()) {
                                                val groupedByLine = selectedTokens.groupBy { it.parentLine }
                                                val orderedLines = groupedByLine.keys.sortedBy { it.box2d?.get(0) ?: 0 }
                                                
                                                val combinedText = orderedLines.joinToString("\n") { line ->
                                                    val lineTokens = groupedByLine[line]!!
                                                    val sortedLineTokens = if (lineTokens.firstOrNull()?.isRtl == true) {
                                                        lineTokens.sortedByDescending { it.xMax }
                                                    } else {
                                                        lineTokens.sortedBy { it.xMin }
                                                    }
                                                    sortedLineTokens.joinToString(" ") { it.text }
                                                }
                                                
                                                val existingMatch = highlights.find { h ->
                                                    h.selectedText.contains(combinedText) || combinedText.contains(h.selectedText)
                                                }
                                                
                                                if (existingMatch != null) {
                                                    editingHighlightId = existingMatch.id
                                                    editingHighlightColorHex = existingMatch.colorHex
                                                    stickyNoteTextInput = existingMatch.comment ?: ""
                                                    showStickyNoteEditor = !existingMatch.comment.isNullOrBlank()
                                                    activeSelectionText = existingMatch.selectedText
                                                } else {
                                                    editingHighlightId = null
                                                    editingHighlightColorHex = "#FFF9C4"
                                                    stickyNoteTextInput = ""
                                                    showStickyNoteEditor = false
                                                    activeSelectionText = combinedText
                                                }
                                                activeSelectionMinY = selectedTokens.minOf { it.yMin }
                                            }
                                        }
                                    }
                                    dragStartFraction = null
                                    dragCurrentFraction = null
                                    dragStartToken = null
                                    dragEndToken = null
                                    isDragging = false
                                },
                                onDragCancel = {
                                    dragStartFraction = null
                                    dragCurrentFraction = null
                                    dragStartToken = null
                                    dragEndToken = null
                                    isDragging = false
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val currentX = (dragCurrentFraction?.x ?: 0f) + (dragAmount.x / size.width.toFloat())
                                    val currentY = (dragCurrentFraction?.y ?: 0f) + (dragAmount.y / size.height.toFloat())
                                    val currentFraction = Offset(currentX.coerceIn(0f, 1f), currentY.coerceIn(0f, 1f))
                                    dragCurrentFraction = currentFraction
                                    dragEndToken = findNearest(currentFraction)
                                }
                            )
                        }
                ) {
                    // Group highlights and draw continuous rectangles per line
                    highlights.forEach { h ->
                        val hText = h.selectedText.trim()
                        val matchingTokens = wordTokens.filter { token ->
                            val lText = token.parentLine.text.trim()
                            if (lText.contains(hText)) {
                                val startCharIndex = token.parentLine.text.indexOf(hText)
                                val endCharIndex = startCharIndex + hText.length
                                token.startIndex >= startCharIndex && token.endIndex <= endCharIndex
                            } else if (hText.contains(lText)) {
                                true
                            } else {
                                val hWords = hText.split("\\s+".toRegex()).filter { it.isNotBlank() }
                                hWords.contains(token.text) && (
                                    hText.contains(token.text) && (
                                        (token.startIndex < 15 && hText.startsWith(token.text)) ||
                                        (token.endIndex > lText.length - 15 && hText.endsWith(token.text)) ||
                                        hText.contains(lText.substring(0, minOf(lText.length, 10))) ||
                                        hText.contains(lText.substring(maxOf(0, lText.length - 10)))
                                    )
                                )
                            }
                        }

                        val highlightColor = try {
                            Color(android.graphics.Color.parseColor(h.colorHex))
                        } catch (e: Exception) {
                            Color(0xFFFFF9C4)
                        }

                        val tokensByLine = matchingTokens.groupBy { it.parentLine }
                        tokensByLine.forEach { (_, lineTokens) ->
                            if (lineTokens.isNotEmpty()) {
                                val minX = lineTokens.minOf { it.xMin }
                                val maxX = lineTokens.maxOf { it.xMax }
                                val minY = lineTokens.minOf { it.yMin }
                                val maxY = lineTokens.maxOf { it.yMax }

                                val left = size.width * minX
                                val top = size.height * minY
                                val right = size.width * maxX
                                val bottom = size.height * maxY

                                // Beautiful solid marker-like fill without distracting border strokes
                                drawRoundRect(
                                    color = highlightColor.copy(alpha = 0.38f),
                                    topLeft = Offset(left, top),
                                    size = Size(right - left, bottom - top),
                                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                            }
                        }
                    }

                    // Draw dragging selection continuously per line in reading order
                    if (isDragging && dragStartToken != null && dragEndToken != null) {
                        val idxStart = wordTokens.indexOf(dragStartToken!!)
                        val idxEnd = wordTokens.indexOf(dragEndToken!!)
                        if (idxStart != -1 && idxEnd != -1) {
                            val minIdx = minOf(idxStart, idxEnd)
                            val maxIdx = maxOf(idxStart, idxEnd)
                            val draggedTokens = wordTokens.subList(minIdx, maxIdx + 1)

                            val tokensByLine = draggedTokens.groupBy { it.parentLine }
                            tokensByLine.forEach { (_, lineTokens) ->
                                if (lineTokens.isNotEmpty()) {
                                    val minX = lineTokens.minOf { it.xMin }
                                    val maxX = lineTokens.maxOf { it.xMax }
                                    val minY = lineTokens.minOf { it.yMin }
                                    val maxY = lineTokens.maxOf { it.yMax }

                                    val left = size.width * minX
                                    val top = size.height * minY
                                    val right = size.width * maxX
                                    val bottom = size.height * maxY

                                    // Gorgeous light-blue marker highlight for ongoing drag selection
                                    drawRoundRect(
                                        color = Color(0xFF90CAF9).copy(alpha = 0.5f),
                                        topLeft = Offset(left, top),
                                        size = Size(right - left, bottom - top),
                                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                    
                    highlights.forEach { h ->
                        if (!h.comment.isNullOrBlank()) {
                            val hText = h.selectedText.trim()
                            val firstToken = wordTokens.find { t ->
                                val lText = t.parentLine.text.trim()
                                if (lText.contains(hText)) {
                                    val startCharIndex = t.parentLine.text.indexOf(hText)
                                    t.startIndex >= startCharIndex && t.endIndex <= startCharIndex + hText.length
                                } else {
                                    hText.contains(t.text)
                                }
                            }
                            
                            if (firstToken != null) {
                                val left = size.width * firstToken.xMin
                                val top = size.height * firstToken.yMin
                                val pinRadius = 6.dp.toPx()
                                val pinX = left + 8.dp.toPx()
                                val pinY = top + 8.dp.toPx()
                                
                                drawCircle(
                                    color = Color(0xFFFFB300),
                                    radius = pinRadius,
                                    center = Offset(pinX, pinY)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = pinRadius / 2f,
                                    center = Offset(pinX, pinY)
                                )
                            }
                        }
                    }
                }

                // Inline, adaptive, floating overlay bar directly under or over selection region mirroring Adobe PDF Premium
                if (activeSelectionText != null) {
                    val alignMenu = if (activeSelectionMinY > 0.5f) Alignment.TopCenter else Alignment.BottomCenter
                    val floatPadding = if (activeSelectionMinY > 0.5f) PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp) else PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp)
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AntiqueIvory.copy(alpha = 0.98f)),
                        border = BorderStroke(1.dp, Color(0xFFD4C8B6)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .align(alignMenu)
                            .padding(floatPadding)
                            .fillMaxWidth(0.92f)
                            .animateContentSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (editingHighlightId != null) "تعديل الملاحظة والتعليم" else "خيارات النص المختار (Text Options)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = ScholarPrimary
                                )
                                IconButton(
                                    onClick = { activeSelectionText = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = ScholarSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ClassicPaper, RoundedCornerShape(6.dp))
                                    .border(BorderStroke(1.dp, NaturalSand), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = finalSelectedText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Serif,
                                    color = DarkText,
                                    lineHeight = 20.sp,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (wordsList.size > 1) {
                                Text(
                                    text = "تحديد جزئي بالكلمات (Partial line range chooser):",
                                    fontSize = 11.sp,
                                    color = ScholarSecondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Left
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
                                ) {
                                    wordsList.forEachIndexed { i, word ->
                                        val isSelected = i in wordStartIndex..wordEndIndex
                                        val chipBg = if (isSelected) ScholarPrimary else ClassicPaper
                                        val chipBorderCol = if (isSelected) ScholarPrimary else NaturalSand
                                        val chipTextCol = if (isSelected) AntiqueIvory else ScholarPrimary
                                        
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = chipBg,
                                            border = BorderStroke(1.dp, chipBorderCol),
                                            modifier = Modifier.clickable {
                                                if (wordStartIndex == wordEndIndex) {
                                                    if (i < wordStartIndex) {
                                                        wordStartIndex = i
                                                    } else {
                                                        wordEndIndex = i
                                                    }
                                                } else {
                                                    wordStartIndex = i
                                                    wordEndIndex = i
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = word,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Serif,
                                                color = chipTextCol,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dynamic Highlight color selection triggers instant draw without leaving page
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Yellow
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color(0xFFFFF59D), RoundedCornerShape(100.dp))
                                            .border(
                                                BorderStroke(
                                                    if (editingHighlightColorHex == "#FFF9C4") 2.dp else 0.dp,
                                                    if (editingHighlightColorHex == "#FFF9C4") ScholarPrimary else Color.Transparent
                                                ),
                                                RoundedCornerShape(100.dp)
                                            )
                                            .clickable {
                                                editingHighlightColorHex = "#FFF9C4"
                                                onSaveHighlight(
                                                    finalSelectedText,
                                                    "#FFF9C4",
                                                    stickyNoteTextInput.ifBlank { null },
                                                    editingHighlightId ?: 0L
                                                )
                                                if (!showStickyNoteEditor) {
                                                    activeSelectionText = null
                                                }
                                            }
                                    )
                                    
                                    // Green
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color(0xFFA5D6A7), RoundedCornerShape(100.dp))
                                            .border(
                                                BorderStroke(
                                                    if (editingHighlightColorHex == "#C8E6C9") 2.dp else 0.dp,
                                                    if (editingHighlightColorHex == "#C8E6C9") ScholarPrimary else Color.Transparent
                                                ),
                                                RoundedCornerShape(100.dp)
                                            )
                                            .clickable {
                                                editingHighlightColorHex = "#C8E6C9"
                                                onSaveHighlight(
                                                    finalSelectedText,
                                                    "#C8E6C9",
                                                    stickyNoteTextInput.ifBlank { null },
                                                    editingHighlightId ?: 0L
                                                )
                                                if (!showStickyNoteEditor) {
                                                    activeSelectionText = null
                                                }
                                            }
                                    )
                                    
                                    // Blue
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color(0xFF90CAF9), RoundedCornerShape(100.dp))
                                            .border(
                                                BorderStroke(
                                                    if (editingHighlightColorHex == "#BBDEFB") 2.dp else 0.dp,
                                                    if (editingHighlightColorHex == "#BBDEFB") ScholarPrimary else Color.Transparent
                                                ),
                                                RoundedCornerShape(100.dp)
                                            )
                                            .clickable {
                                                editingHighlightColorHex = "#BBDEFB"
                                                onSaveHighlight(
                                                    finalSelectedText,
                                                    "#BBDEFB",
                                                    stickyNoteTextInput.ifBlank { null },
                                                    editingHighlightId ?: 0L
                                                )
                                                if (!showStickyNoteEditor) {
                                                    activeSelectionText = null
                                                }
                                            }
                                    )
                                }
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Sticky Note Toggle/Edit option
                                    IconButton(
                                        onClick = { showStickyNoteEditor = !showStickyNoteEditor },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(if (showStickyNoteEditor) ScholarPrimary.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(6.dp))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Comment,
                                            contentDescription = "Sticky Note",
                                            tint = if (showStickyNoteEditor) ScholarPrimary else ScholarSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    
                                    // Copy text option
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Madrassah Manuscript", finalSelectedText)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "تم النسخ!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy text",
                                            tint = ScholarSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    
                                    // AI Explanation
                                    IconButton(
                                        onClick = {
                                            onExplainText(finalSelectedText)
                                            activeSelectionText = null
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Explain Text",
                                            tint = VerseGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Translation
                                    IconButton(
                                        onClick = {
                                            onTranslateText(finalSelectedText)
                                            activeSelectionText = null
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Translate,
                                            contentDescription = "Translate Urdu",
                                            tint = CustomGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    
                                    // Delete Highlight
                                    if (editingHighlightId != null) {
                                        IconButton(
                                            onClick = {
                                                onDeleteHighlight(editingHighlightId!!)
                                                activeSelectionText = null
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete highlight",
                                                tint = Color.Red.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Yellow inline postcard memo sticky note field
                            if (showStickyNoteEditor) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = NaturalSand)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                OutlinedTextField(
                                    value = stickyNoteTextInput,
                                    onValueChange = { stickyNoteTextInput = it },
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Serif),
                                    placeholder = { Text("اكتب خاطرة أو شرح للخطوط هنا (أضف مفكرة لاصقة)...", fontSize = 11.sp, color = Color.Gray) },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 120.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ScholarPrimary,
                                        unfocusedBorderColor = NaturalSand,
                                        focusedContainerColor = ClassicPaper,
                                        unfocusedContainerColor = ClassicPaper
                                    )
                                )
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { showStickyNoteEditor = false },
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Text("إغلاق", fontSize = 11.sp, color = ScholarSecondary)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                            onSaveHighlight(
                                                activeSelectionText!!,
                                                editingHighlightColorHex,
                                                stickyNoteTextInput.ifBlank { null },
                                                editingHighlightId ?: 0L
                                            )
                                            activeSelectionText = null
                                            showStickyNoteEditor = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ScholarPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("حفظ الملاحظة", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = ScholarPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "جاري تحميل الصفحة (Rendering...)",
                    fontSize = 12.sp,
                    color = ScholarSecondary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * 3. Interactive Split-Pane Workspace (Containing OCR transcripts, highlights, AI cards, audio, notes)
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WorkspaceInteractivePane(
    viewModel: ReaderViewModel,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    pageState: PageState?,
    highlights: List<Highlight>,
    isOcrLoading: Boolean,
    ocrError: String?,
    isAiLoading: Boolean,
    aiExplanation: String?,
    aiTranslation: String?,
    onParagraphClick: (String) -> Unit
) {
    val context = LocalContext.current
    var notesInputState by remember { mutableStateOf("") }

    // Sync input box when db updates or page shifts
    LaunchedEffect(pageState?.id, pageState?.noteText) {
        notesInputState = pageState?.noteText ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AntiqueIvory)
    ) {
        // Scholar Study Tabs Header
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = AntiqueIvory,
            contentColor = ScholarPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = ScholarPrimary
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabChange(0) },
                icon = { Icon(Icons.Default.Translate, contentDescription = "Texts and Research") },
                text = { Text("النص والبحث (OCR)", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onTabChange(1) },
                icon = { Icon(Icons.Default.Mic, contentDescription = "Voicenotes") },
                text = { Text("المُذَكِّرَة (Voice Notes)", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        // Expanded interactive results space
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> {
                    // Highlights & OCR pane
                    OcrResearchPanel(
                        extractedText = pageState?.extractedText,
                        isOcrLoading = isOcrLoading,
                        ocrError = ocrError,
                        highlights = highlights,
                        isAiLoading = isAiLoading,
                        aiExplanation = aiExplanation,
                        aiTranslation = aiTranslation,
                        onParagraphClick = onParagraphClick,
                        onClearAiState = { viewModel.clearExplanationAndTranslation() },
                        onRunOcr = { viewModel.runOcrOnCurrentPage() },
                        onDeleteHighlight = { id -> viewModel.deleteTextHighlight(id) }
                    )
                }

                1 -> {
                    // Handwritten Notes and Urdu/Arabic voice dictation panel
                    VoiceNotesPanel(
                        currentText = notesInputState,
                        onTextChange = {
                            notesInputState = it
                            viewModel.savePageNote(it)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Panel 1: Scanned Text OCR research dashboard
 */
@Composable
fun OcrResearchPanel(
    extractedText: String?,
    isOcrLoading: Boolean,
    ocrError: String?,
    highlights: List<Highlight>,
    isAiLoading: Boolean,
    aiExplanation: String?,
    aiTranslation: String?,
    onParagraphClick: (String) -> Unit,
    onClearAiState: () -> Unit,
    onRunOcr: () -> Unit,
    onDeleteHighlight: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Loading States
        if (isAiLoading) {
            item {
                Card(
                    colors = CardColors(containerColor = ClassicPaper, contentColor = ScholarPrimary, disabledContainerColor = Color.Transparent, disabledContentColor = Color.Transparent),
                    border = BorderStroke(1.dp, CustomGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = CustomGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("جاري استدعاء معجم الذكاء الاصطناعي (Querying Gemini Research)...", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Display AI Explanation cards
        if (aiExplanation != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AntiqueIvory),
                    border = BorderStroke(1.dp, VerseGreen)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Explain", tint = VerseGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("توضيح وبحث لغوي (AI Explanation)", fontWeight = FontWeight.Bold, color = VerseGreen, fontSize = 14.sp)
                            }
                            IconButton(onClick = onClearAiState, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = aiExplanation,
                            fontSize = 13.sp,
                            color = DarkText,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Display AI Translation results
        if (aiTranslation != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AntiqueIvory),
                    border = BorderStroke(1.dp, CustomGold)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Translate, contentDescription = "Translation", tint = CustomGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ترجمہ بذیعہ ذہین معاون (Urdu Translation)", fontWeight = FontWeight.Bold, color = CustomGold, fontSize = 14.sp)
                            }
                            IconButton(onClick = onClearAiState, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = aiTranslation,
                            fontSize = 14.sp,
                            color = DarkText,
                            fontFamily = FontFamily.Serif,
                            lineHeight = 22.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Scanned Extracted Text Box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ClassicPaper),
                border = BorderStroke(1.dp, NaturalSand)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "النص المستخرج (Arabic/Urdu Script Plate)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ScholarPrimary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "💡 Scholar's Tip: Tap any sentence/paragraph below to highlight, copy, explain, or translate it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ScholarSecondary.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (extractedText == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Scanner,
                                contentDescription = "Ocr Pending",
                                tint = Color.LightGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "لم يتم تحديد نص لهذه الصفحة بعد",
                                fontWeight = FontWeight.Bold,
                                color = ScholarPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "The scanned text for this page hasn't been extracted. Run the Gemini AI transcription scanner.",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onRunOcr,
                                enabled = !isOcrLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = ScholarPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = "OCR")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("البدء بالاستخراج (Run AI OCR Scan)")
                            }
                        }
                    } else {
                        // Group words and split cleanly by paragraphs/newlines to enable simple line tap selection!
                        val detectedLines = remember(extractedText) { parseDetectedLines(extractedText) }

                        detectedLines.forEach { line ->
                            val textUnit = line.text
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onParagraphClick(textUnit) },
                                colors = CardDefaults.cardColors(containerColor = AntiqueIvory),
                                border = BorderStroke(0.5.dp, NaturalSand)
                            ) {
                                Text(
                                    text = textUnit,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    fontFamily = FontFamily.Serif,
                                    color = DarkText,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                )
                            }
                        }
                    }

                    ocrError?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = err,
                                color = Color.Red,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onRunOcr,
                                colors = ButtonDefaults.buttonColors(containerColor = ScholarPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "إعادة المحاولة",
                                    color = AntiqueIvory,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active page highlights database block
        item {
            Text(
                text = "الكلمات المعلمة (Page Highlights Log)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ScholarPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (highlights.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد أي نصوص معلمة في هذه الصفحة.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        } else {
            items(highlights) { highlight ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(android.graphics.Color.parseColor(highlight.colorHex)).copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = highlight.selectedText,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Serif,
                                color = Color.Black,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        IconButton(onClick = { onDeleteHighlight(highlight.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Erase Highlight", tint = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Panel 2: Notes & Urdu/Arabic speech dictation panel
 */
@Composable
fun VoiceNotesPanel(
    currentText: String,
    onTextChange: (String) -> Unit
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var partialText by remember { mutableStateOf("") }
    var speechError by remember { mutableStateOf<String?>(null) }
    var micLanguageCode by remember { mutableStateOf("ur-PK") } // default urdu

    // Android audio recorder permission checker
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                Toast.makeText(context, "Microphone access is locked. Cannot use speech-to-text input.", Toast.LENGTH_LONG).show()
            }
        }
    )

    // Speech Recognizer listener setup
    val speechRecognizerManager = remember {
        SpeechRecognizerManager(
            context = context,
            onStart = {
                isListening = true
                speechError = null
                partialText = ""
            },
            onResults = { result ->
                isListening = false
                partialText = ""
                // Append text safely
                val joined = if (currentText.isEmpty()) result else "$currentText $result"
                onTextChange(joined)
            },
            onPartialResults = { partial ->
                partialText = partial
            },
            onError = { err ->
                speechError = err
                isListening = false
            },
            onStopped = {
                isListening = false
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizerManager.stopListening()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Upper Speech Convert Dashboard
        Card(
            colors = CardDefaults.cardColors(containerColor = ClassicPaper),
            border = BorderStroke(1.dp, NaturalSand)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "محول الصوت إلى كتابة (Speech-To-Text Console)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ScholarPrimary
                )
                Text(
                    text = "Translate read scripts into Urdu or standard Arabic vocal inputs instantly without writing manually.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ScholarSecondary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Select voice language
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "لغة الإدخال (Recording Voice):",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = ScholarSecondary
                    )

                    Row {
                        FilterChip(
                            selected = micLanguageCode == "ur-PK",
                            onClick = { micLanguageCode = "ur-PK" },
                            label = { Text("اردو (Urdu)") }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilterChip(
                            selected = micLanguageCode == "ar-SA",
                            onClick = { micLanguageCode = "ar-SA" },
                            label = { Text("العربية (Arabic)") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Recorder State UI Feedback
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AntiqueIvory, RoundedCornerShape(8.dp))
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isListening) VerseGreen else NaturalSand
                            ),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isListening) {
                            Text(
                                text = "حالة التسجيل: جار الاستماع الآن... (Microphone Active)",
                                fontSize = 11.sp,
                                color = VerseGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = partialText.ifEmpty { "تحدث الآن (Speak now...)" },
                                textAlign = TextAlign.Center,
                                color = DarkText,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "انقر فوق الميكروفون لبدء الإملاء",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        speechError?.let { err ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = err,
                                color = Color.Red,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Microphone action FAB
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            val permissionCheck = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            )
                            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                                recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                if (isListening) {
                                    speechRecognizerManager.stopListening()
                                } else {
                                    speechRecognizerManager.startListening(micLanguageCode)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListening) Color(0xFFA13333) else ScholarPrimary
                        ),
                        modifier = Modifier.testTag("microphone_toggle")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Voice dictation toggle"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isListening) "إيقاف (Stop Recording)" else "إملاء صوتي (Record Voice)"
                        )
                    }
                }
            }
        }

        // Notes Workspace editor layout
        Card(
            colors = CardDefaults.cardColors(containerColor = AntiqueIvory),
            border = BorderStroke(1.dp, Color(0xFFDCCFBD)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Text(
                    text = "كتابة الملاحظات اليدوية والمنطوقة (Hands & Vocal Annotations Log):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ScholarPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = currentText,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("notes_text_field"),
                    placeholder = {
                        Text(
                            text = "اكتب ملاحظاتك العلمية هنا للرجوع إليها، أو استخدم الإملاء الصوتي أعلاه...",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScholarPrimary,
                        unfocusedBorderColor = Color(0xFFD4C8B8),
                        focusedContainerColor = AntiqueIvory,
                        unfocusedContainerColor = AntiqueIvory
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

/**
 * Resolve original PDF file title string
 */
private fun getContentFileName(resolver: android.content.ContentResolver, uri: Uri): String? {
    var name: String? = null
    val cursor = resolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index != -1) {
                name = it.getString(index)
            }
        }
    }
    return name ?: uri.path?.substringAfterLast('/')
}
