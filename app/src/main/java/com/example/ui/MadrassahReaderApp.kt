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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    val isPageBookmarked by viewModel.isCurrentPageBookmarked.collectAsState()
    var showGoToPageDialog by remember { mutableStateOf(false) }

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
                    // Toggle Bookmark Page Button
                    IconButton(
                        onClick = { 
                            viewModel.toggleBookmarkForCurrentPage()
                        }
                    ) {
                        Icon(
                            imageVector = if (isPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark Page",
                            tint = if (isPageBookmarked) Color(0xFFFBC02D) else AntiqueIvory
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))

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
                    if (isWideScreen) {
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
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { 
                            if (pageState?.extractedText != null) {
                                isPaneVisible = true
                            } else {
                                viewModel.runOcrOnCurrentPage() 
                            }
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
                    if (isWideScreen) {
                        TextButton(
                            onClick = { 
                                if (pageState?.extractedText != null) {
                                    isPaneVisible = true
                                } else {
                                    viewModel.runOcrOnCurrentPage()
                                }
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

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showGoToPageDialog = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "صفحة  ${currentPageIndex + 1} / ${book.totalPages}",
                            color = AntiqueIvory,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowCircleUp,
                            contentDescription = "انتقال إلى صفحة (Jump to Page)",
                            tint = AntiqueIvory.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
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

            // Beautiful interactive Page Jumping Dialog
            if (showGoToPageDialog) {
                var pageInputText by remember { mutableStateOf("") }
                var errorMessage by remember { mutableStateOf<String?>(null) }
                
                AlertDialog(
                    onDismissRequest = { showGoToPageDialog = false },
                    title = { 
                        Text(
                            text = "انتقال إلى صفحة (Go to Page)", 
                            fontWeight = FontWeight.Bold, 
                            color = ScholarPrimary,
                            fontFamily = FontFamily.Serif
                        ) 
                    },
                    text = {
                        Column {
                            Text(
                                text = "أدخل رقم الصفحة بين 1 و ${book.totalPages}:", 
                                fontSize = 13.sp, 
                                color = ScholarSecondary,
                                fontFamily = FontFamily.Serif
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = pageInputText,
                                onValueChange = { 
                                    pageInputText = it
                                    errorMessage = null
                                },
                                placeholder = { Text("مثال: 5") },
                                modifier = Modifier.fillMaxWidth().testTag("goto_page_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ScholarPrimary,
                                    unfocusedBorderColor = NaturalSand,
                                    focusedTextColor = DarkText,
                                    unfocusedTextColor = DarkText
                                ),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                singleLine = true
                            )
                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = errorMessage!!, 
                                    color = Color(0xFFA13333), 
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val pNum = pageInputText.toIntOrNull()
                                if (pNum != null && pNum in 1..book.totalPages) {
                                    viewModel.goToPage(pNum - 1, context)
                                    showGoToPageDialog = false
                                } else {
                                    errorMessage = "رقم غير صحيح! يرجى إدخال قيمة بين 1 و ${book.totalPages}."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ScholarPrimary)
                        ) {
                            Text("انتقال (Go)")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showGoToPageDialog = false }) {
                            Text("إلغاء (Cancel)", color = ScholarSecondary)
                        }
                    }
                )
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
                            onSaveHighlight = { text, color, comment, hId, startOff, endOff ->
                                viewModel.saveTextHighlight(text, color, comment, hId, startOff, endOff)
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
                            onSaveHighlight = { text, color, comment, hId, startOff, endOff ->
                                viewModel.saveTextHighlight(text, color, comment, hId, startOff, endOff)
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

    // Paragraph interaction action sheet dialogue
    selectedParagraphForAction?.let { paragraph ->
        Dialog(onDismissRequest = { selectedParagraphForAction = null }) {
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
                            text = paragraph,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Serif,
                            color = DarkText,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
                                        viewModel.saveTextHighlight(paragraph, "#FFF176") // Gold/Yellow
                                        selectedParagraphForAction = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF176)),
                                    modifier = Modifier.height(36.dp).weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("أصفر", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.saveTextHighlight(paragraph, "#81C784") // Mint Green
                                        selectedParagraphForAction = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                                    modifier = Modifier.height(36.dp).weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("أخضر", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.saveTextHighlight(paragraph, "#80DEEA") // Ocean Blue
                                        selectedParagraphForAction = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF80DEEA)),
                                    modifier = Modifier.height(36.dp).weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("أزرق", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.saveTextHighlight(paragraph, "#E1BEE7") // Soft Purple
                                        selectedParagraphForAction = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1BEE7)),
                                    modifier = Modifier.height(36.dp).weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("بنفسجي", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.saveTextHighlight(paragraph, "#FF8A80") // Light Red
                                        selectedParagraphForAction = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8A80)),
                                    modifier = Modifier.height(36.dp).weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("أحمر", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = NaturalSand)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Research AI Translation
                    Button(
                        onClick = {
                            isPaneVisible = true
                            viewModel.translateSelectedText(paragraph, "Urdu")
                            selectedParagraphForAction = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CustomGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = "Translate", tint = ScholarSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ترجمہ (Translate to Urdu)", color = ScholarSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                                val clip = android.content.ClipData.newPlainText("Madrassah Text", paragraph)
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

data class DetectedLine(
    val text: String,
    val box2d: List<Int>? = null
)

data class DetectedWord(
    val text: String,
    val lineIndex: Int,
    val wordIndexInLine: Int,
    val charStartInLine: Int,
    val charEndInLine: Int,
    val xMin: Float,
    val xMax: Float,
    val yMin: Float,
    val yMax: Float
)

fun getWordsFromLine(lineText: String, lineIndex: Int, yMin: Float, xMin: Float, yMax: Float, xMax: Float): List<DetectedWord> {
    val wordsList = mutableListOf<DetectedWord>()
    val matcher = java.util.regex.Pattern.compile("\\S+").matcher(lineText)
    val foundWords = mutableListOf<Pair<String, IntRange>>()
    while (matcher.find()) {
        foundWords.add(Pair(matcher.group(), matcher.start()..matcher.end()))
    }
    
    val totalWords = foundWords.size
    if (totalWords == 0) return emptyList()
    
    val rtl = lineText.any { it.code in 0x0590..0x08FF }
    val totalChars = lineText.length.toFloat().coerceAtLeast(1f)
    
    for (idx in 0 until totalWords) {
        val (wordText, range) = foundWords[idx]
        val startFrac = range.first / totalChars
        val endFrac = range.last / totalChars
        
        val wordXMin: Float
        val wordXMax: Float
        if (rtl) {
            wordXMin = xMax - (endFrac * (xMax - xMin))
            wordXMax = xMax - (startFrac * (xMax - xMin))
        } else {
            wordXMin = xMin + (startFrac * (xMax - xMin))
            wordXMax = xMin + (endFrac * (xMax - xMin))
        }
        
        wordsList.add(
            DetectedWord(
                text = wordText,
                lineIndex = lineIndex,
                wordIndexInLine = idx,
                charStartInLine = range.first,
                charEndInLine = range.last,
                xMin = minOf(wordXMin, wordXMax),
                xMax = maxOf(wordXMin, wordXMax),
                yMin = yMin,
                yMax = yMax
            )
        )
    }
    return wordsList
}

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
    onSaveHighlight: (text: String, colorHex: String, comment: String?, id: Long, startOffset: Int, endOffset: Int) -> Unit,
    onDeleteHighlight: (Long) -> Unit,
    onExplainText: (String) -> Unit,
    onTranslateText: (String) -> Unit
) {
    val context = LocalContext.current
    var activeSelectionText by remember(pageState) { mutableStateOf<String?>(null) }
    var activeSelectionMinY by remember { mutableStateOf(0.5f) }

    var editingHighlightId by remember { mutableStateOf<Long?>(null) }
    var editingHighlightColorHex by remember { mutableStateOf("#FFF176") }
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

                val allWords = remember(detectedLines) {
                    val list = mutableListOf<DetectedWord>()
                    detectedLines.forEachIndexed { lineIdx, line ->
                        val box = line.box2d
                        val (yMin, xMin, yMax, xMax) = if (box != null && box.size == 4) {
                            val y0 = (box[0] / 1000f).coerceIn(0f, 1f)
                            val x0 = (box[1] / 1000f).coerceIn(0f, 1f)
                            val y1 = (box[2] / 1000f).coerceIn(0f, 1f)
                            val x1 = (box[3] / 1000f).coerceIn(0f, 1f)
                            listOf(y0, x0, y1, x1)
                        } else {
                            val y0 = lineIdx.toFloat() / (detectedLines.size.coerceAtLeast(1)).toFloat()
                            val y1 = (lineIdx + 0.8f) / (detectedLines.size.coerceAtLeast(1)).toFloat()
                            listOf(y0, 0.05f, y1, 0.95f)
                        }
                        val lineWords = getWordsFromLine(line.text, lineIdx, yMin, xMin, yMax, xMax)
                        list.addAll(lineWords)
                    }
                    list
                }

                var dragStartIndex by remember { mutableStateOf<Int?>(null) }
                var dragCurrentIndex by remember { mutableStateOf<Int?>(null) }
                var isDragging by remember { mutableStateOf(false) }

                var activeSelectionStartWordIdx by remember(pageState) { mutableStateOf<Int?>(null) }
                var activeSelectionEndWordIdx by remember(pageState) { mutableStateOf<Int?>(null) }

                fun findClosestWordIndexFrac(fracOffset: Offset): Int {
                    if (allWords.isEmpty()) return -1
                    var closestIdx = 0
                    var minDist = Double.MAX_VALUE
                    
                    allWords.forEachIndexed { index, word ->
                        val cx = (word.xMin + word.xMax) / 2f
                        val cy = (word.yMin + word.yMax) / 2f
                        
                        val dx = (fracOffset.x - cx)
                        val dy = (fracOffset.y - cy) * 1.5f
                        val dist = (dx * dx + dy * dy).toDouble()
                        if (dist < minDist) {
                            minDist = dist
                            closestIdx = index
                        }
                    }
                    return closestIdx
                }

                fun findClosestWordIndex(pixelsOffset: Offset, canvasWidth: Float, canvasHeight: Float): Int {
                    val sizeX = canvasWidth.coerceAtLeast(1f)
                    val sizeY = canvasHeight.coerceAtLeast(1f)
                    val fracOffset = Offset(pixelsOffset.x / sizeX, pixelsOffset.y / sizeY)
                    return findClosestWordIndexFrac(fracOffset)
                }

                fun isWordHighlighted(word: DetectedWord, wordIndex: Int, highlightsList: List<Highlight>): Highlight? {
                    return highlightsList.find { h ->
                        if (h.startOffset != 0 || h.endOffset != 0) {
                            wordIndex in h.startOffset..h.endOffset
                        } else {
                            h.selectedText.contains(word.text) && (detectedLines.getOrNull(word.lineIndex)?.text?.contains(h.selectedText) == true || h.selectedText.contains(detectedLines.getOrNull(word.lineIndex)?.text ?: ""))
                        }
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(allWords, highlights) {
                            detectTapGestures { offset ->
                                val xFraction = offset.x / size.width.toFloat()
                                val yFraction = offset.y / size.height.toFloat()
                                val fracOffset = Offset(xFraction, yFraction)
                                
                                val wordIdx = findClosestWordIndexFrac(fracOffset)
                                if (wordIdx != -1) {
                                    val clickedWord = allWords[wordIdx]
                                    val clickedLine = detectedLines.getOrNull(clickedWord.lineIndex)
                                    
                                    if (clickedLine != null) {
                                        var assocHighlight: Highlight? = null
                                        for (h in highlights) {
                                            if (h.startOffset != 0 || h.endOffset != 0) {
                                                if (wordIdx in h.startOffset..h.endOffset) {
                                                    assocHighlight = h
                                                    break
                                                }
                                            } else {
                                                if (h.selectedText.contains(clickedWord.text) && (clickedLine.text.contains(h.selectedText) || h.selectedText.contains(clickedLine.text))) {
                                                    assocHighlight = h
                                                    break
                                                }
                                            }
                                        }

                                        if (assocHighlight != null) {
                                            editingHighlightId = assocHighlight.id
                                            editingHighlightColorHex = assocHighlight.colorHex
                                            stickyNoteTextInput = assocHighlight.comment ?: ""
                                            showStickyNoteEditor = !assocHighlight.comment.isNullOrBlank()
                                            activeSelectionText = assocHighlight.selectedText
                                            
                                            activeSelectionStartWordIdx = assocHighlight.startOffset
                                            activeSelectionEndWordIdx = assocHighlight.endOffset
                                        } else {
                                            editingHighlightId = null
                                            editingHighlightColorHex = "#FFF176"
                                            stickyNoteTextInput = ""
                                            showStickyNoteEditor = false
                                            activeSelectionText = clickedWord.text
                                            
                                            activeSelectionStartWordIdx = wordIdx
                                            activeSelectionEndWordIdx = wordIdx
                                        }
                                        activeSelectionMinY = clickedWord.yMin
                                    }
                                }
                            }
                        }
                        .pointerInput(allWords, highlights) {
                            var currentDragPixels = Offset.Zero
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentDragPixels = offset
                                    val wordIdx = findClosestWordIndex(offset, size.width.toFloat(), size.height.toFloat())
                                    if (wordIdx != -1) {
                                        dragStartIndex = wordIdx
                                        dragCurrentIndex = wordIdx
                                        isDragging = true
                                    }
                                },
                                onDragEnd = {
                                    val start = dragStartIndex
                                    val current = dragCurrentIndex
                                    if (start != null && current != null) {
                                        val minIdx = minOf(start, current)
                                        val maxIdx = maxOf(start, current)
                                        
                                        val selectedWords = allWords.subList(minIdx, maxIdx + 1)
                                        if (selectedWords.isNotEmpty()) {
                                            val combinedText = selectedWords.groupBy { it.lineIndex }
                                                .values
                                                .joinToString("\n") { lineWords ->
                                                    lineWords.joinToString(" ") { it.text }
                                                }
                                            
                                            activeSelectionStartWordIdx = minIdx
                                            activeSelectionEndWordIdx = maxIdx
                                            
                                            val existingHighlight = highlights.find { h ->
                                                (h.startOffset == minIdx && h.endOffset == maxIdx) ||
                                                (h.selectedText == combinedText)
                                            }
                                            
                                            if (existingHighlight != null) {
                                                editingHighlightId = existingHighlight.id
                                                editingHighlightColorHex = existingHighlight.colorHex
                                                stickyNoteTextInput = existingHighlight.comment ?: ""
                                                showStickyNoteEditor = !existingHighlight.comment.isNullOrBlank()
                                                activeSelectionText = existingHighlight.selectedText
                                            } else {
                                                editingHighlightId = null
                                                editingHighlightColorHex = "#FFF176"
                                                stickyNoteTextInput = ""
                                                showStickyNoteEditor = false
                                                activeSelectionText = combinedText
                                            }
                                            activeSelectionMinY = selectedWords.first().yMin
                                        }
                                    }
                                    dragStartIndex = null
                                    dragCurrentIndex = null
                                    isDragging = false
                                },
                                onDragCancel = {
                                    dragStartIndex = null
                                    dragCurrentIndex = null
                                    isDragging = false
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    currentDragPixels = Offset(
                                        (currentDragPixels.x + dragAmount.x).coerceIn(0f, size.width.toFloat()),
                                        (currentDragPixels.y + dragAmount.y).coerceIn(0f, size.height.toFloat())
                                    )
                                    val wordIdx = findClosestWordIndex(currentDragPixels, size.width.toFloat(), size.height.toFloat())
                                    if (wordIdx != -1) {
                                        dragCurrentIndex = wordIdx
                                    }
                                }
                            )
                        }
                ) {
                    fun DrawScope.drawIconButtonLikeKnob(center: Offset, isStart: Boolean) {
                        val pinColor = ScholarPrimary
                        val poleLength = 14.dp.toPx()
                        val knobRadius = 5.dp.toPx()
                        
                        val lineTop = if (isStart) center.y - poleLength else center.y
                        val lineBottom = if (isStart) center.y else center.y + poleLength
                        val knobCenterY = if (isStart) center.y - poleLength else center.y + poleLength
                        
                        drawLine(
                            color = pinColor,
                            start = Offset(center.x, lineTop),
                            end = Offset(center.x, lineBottom),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawCircle(
                            color = pinColor,
                            radius = knobRadius,
                            center = Offset(center.x, knobCenterY)
                        )
                    }

                    // 1. Draw Saved Highlights (with BlendMode.Multiply, continuous grouping)
                    highlights.forEach { highlight ->
                        val highlightColor = try {
                            Color(android.graphics.Color.parseColor(highlight.colorHex))
                        } catch (e: Exception) {
                            Color(0xFFFFF176)
                        }

                        // Filter and find index pairs for all words matching the highlight target boundary
                        val wordsInHighlight = allWords.mapIndexed { index, word -> index to word }
                            .filter { (wordIndex, word) ->
                                if (highlight.startOffset != 0 || highlight.endOffset != 0) {
                                    wordIndex in highlight.startOffset..highlight.endOffset
                                } else {
                                    val lineText = detectedLines.getOrNull(word.lineIndex)?.text ?: ""
                                    highlight.selectedText.contains(word.text) &&
                                    (lineText.contains(highlight.selectedText) || highlight.selectedText.contains(lineText))
                                }
                            }

                        if (wordsInHighlight.isNotEmpty()) {
                            // Group words in this highlight by line index to draw unified bars/lines
                            val wordsByLine = wordsInHighlight.groupBy { it.second.lineIndex }

                            wordsByLine.forEach { (lineIndex, lineWords) ->
                                val sortedLineWords = lineWords.sortedBy { it.first }
                                val contiguousSegments = mutableListOf<List<Pair<Int, DetectedWord>>>()
                                var currentSegment = mutableListOf<Pair<Int, DetectedWord>>()

                                for (item in sortedLineWords) {
                                    if (currentSegment.isEmpty()) {
                                        currentSegment.add(item)
                                    } else {
                                        val lastIdx = currentSegment.last().first
                                        if (item.first == lastIdx + 1) {
                                            currentSegment.add(item)
                                        } else {
                                            contiguousSegments.add(currentSegment)
                                            currentSegment = mutableListOf(item)
                                        }
                                    }
                                }
                                if (currentSegment.isNotEmpty()) {
                                    contiguousSegments.add(currentSegment)
                                }

                                // Draw each contiguous horizontal subset as one single continuous block
                                contiguousSegments.forEach { segment ->
                                    val left = size.width * segment.minOf { it.second.xMin }
                                    val right = size.width * segment.maxOf { it.second.xMax }
                                    val top = size.height * segment.minOf { it.second.yMin }
                                    val bottom = size.height * segment.maxOf { it.second.yMax }

                                    drawRect(
                                        color = highlightColor.copy(alpha = 0.55f),
                                        topLeft = Offset(left, top),
                                        size = Size(right - left, bottom - top),
                                        blendMode = BlendMode.Multiply
                                    )
                                    drawRoundRect(
                                        color = highlightColor.copy(alpha = 0.8f),
                                        topLeft = Offset(left, top),
                                        size = Size(right - left, bottom - top),
                                        cornerRadius = CornerRadius(4f, 4f),
                                        style = Stroke(width = 1.dp.toPx()),
                                        blendMode = BlendMode.Multiply
                                    )
                                }
                            }
                        }
                    }

                    // 2. Draw Active Drag Highlight (continuous grouping)
                    if (isDragging && dragStartIndex != null && dragCurrentIndex != null) {
                        val minIdx = minOf(dragStartIndex!!, dragCurrentIndex!!)
                        val maxIdx = maxOf(dragStartIndex!!, dragCurrentIndex!!)
                        
                        val dragHighlightColor = Color(0xFF90CAF9)
                        val dragOutlineColor = Color(0xFF1976D2)

                        val activeWords = allWords.mapIndexed { index, word -> index to word }
                            .filter { it.first in minIdx..maxIdx }

                        if (activeWords.isNotEmpty()) {
                            // Group drag words by line for responsive, unified, continuous selection box drawing
                            val wordsByLine = activeWords.groupBy { it.second.lineIndex }

                            wordsByLine.forEach { (lineIndex, lineWords) ->
                                val sortedLineWords = lineWords.sortedBy { it.first }
                                val contiguousSegments = mutableListOf<List<Pair<Int, DetectedWord>>>()
                                var currentSegment = mutableListOf<Pair<Int, DetectedWord>>()

                                for (item in sortedLineWords) {
                                    if (currentSegment.isEmpty()) {
                                        currentSegment.add(item)
                                    } else {
                                        val lastIdx = currentSegment.last().first
                                        if (item.first == lastIdx + 1) {
                                            currentSegment.add(item)
                                        } else {
                                            contiguousSegments.add(currentSegment)
                                            currentSegment = mutableListOf(item)
                                        }
                                    }
                                }
                                if (currentSegment.isNotEmpty()) {
                                    contiguousSegments.add(currentSegment)
                                }

                                contiguousSegments.forEach { segment ->
                                    val left = size.width * segment.minOf { it.second.xMin }
                                    val right = size.width * segment.maxOf { it.second.xMax }
                                    val top = size.height * segment.minOf { it.second.yMin }
                                    val bottom = size.height * segment.maxOf { it.second.yMax }

                                    drawRect(
                                        color = dragHighlightColor.copy(alpha = 0.55f),
                                        topLeft = Offset(left, top),
                                        size = Size(right - left, bottom - top),
                                        blendMode = BlendMode.Multiply
                                    )
                                    drawRoundRect(
                                        color = dragOutlineColor.copy(alpha = 0.8f),
                                        topLeft = Offset(left, top),
                                        size = Size(right - left, bottom - top),
                                        cornerRadius = CornerRadius(4f, 4f),
                                        style = Stroke(width = 1.dp.toPx()),
                                        blendMode = BlendMode.Multiply
                                    )
                                }
                            }
                        }
                    }

                    // 3. Draw Sticky Note Pins
                    highlights.forEach { h ->
                        if (!h.comment.isNullOrBlank()) {
                            val firstWordIndex = if (h.startOffset != 0 || h.endOffset != 0) {
                                h.startOffset
                            } else {
                                allWords.indexOfFirst { it.text.contains(h.selectedText) || h.selectedText.contains(it.text) }
                            }
                            
                            val firstWord = allWords.getOrNull(firstWordIndex)
                            if (firstWord != null) {
                                val left = size.width * firstWord.xMin
                                val top = size.height * firstWord.yMin
                                
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

                    // 4. Draw Start and End handle knobs (Visual Anchor Point markers)
                    val selectMinIdx = if (isDragging) {
                        if (dragStartIndex != null && dragCurrentIndex != null) minOf(dragStartIndex!!, dragCurrentIndex!!) else null
                    } else {
                        activeSelectionStartWordIdx
                    }

                    val selectMaxIdx = if (isDragging) {
                        if (dragStartIndex != null && dragCurrentIndex != null) maxOf(dragStartIndex!!, dragCurrentIndex!!) else null
                    } else {
                        activeSelectionEndWordIdx
                    }

                    if (selectMinIdx != null && selectMaxIdx != null && selectMinIdx != -1 && selectMaxIdx != -1) {
                        val firstWord = allWords.getOrNull(selectMinIdx)
                        val lastWord = allWords.getOrNull(selectMaxIdx)
                        
                        if (firstWord != null) {
                            val isFirstRtl = firstWord.text.any { it.code in 0x0590..0x08FF }
                            val startFracX = if (isFirstRtl) firstWord.xMax else firstWord.xMin
                            val startYFraction = (firstWord.yMin + firstWord.yMax) / 2f
                            
                            drawIconButtonLikeKnob(
                                center = Offset(startFracX * size.width, startYFraction * size.height), 
                                isStart = true
                            )
                        }
                        
                        if (lastWord != null) {
                            val isLastRtl = lastWord.text.any { it.code in 0x0590..0x08FF }
                            val endFracX = if (isLastRtl) lastWord.xMin else lastWord.xMax
                            val endYFraction = (lastWord.yMin + lastWord.yMax) / 2f
                            
                            drawIconButtonLikeKnob(
                                center = Offset(endFracX * size.width, endYFraction * size.height), 
                                isStart = false
                            )
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
                                    text = activeSelectionText!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Serif,
                                    color = DarkText,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
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
                                    // Gold/Yellow
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color(0xFFFFF176), RoundedCornerShape(100.dp))
                                            .border(
                                                BorderStroke(
                                                    if (editingHighlightColorHex == "#FFF176") 2.dp else 0.dp,
                                                    if (editingHighlightColorHex == "#FFF176") ScholarPrimary else Color.Transparent
                                                ),
                                                RoundedCornerShape(100.dp)
                                            )
                                            .clickable {
                                                editingHighlightColorHex = "#FFF176"
                                                onSaveHighlight(
                                                    activeSelectionText!!,
                                                    "#FFF176",
                                                    stickyNoteTextInput.ifBlank { null },
                                                    editingHighlightId ?: 0L,
                                                    activeSelectionStartWordIdx ?: 0,
                                                    activeSelectionEndWordIdx ?: 0
                                                )
                                                if (!showStickyNoteEditor) {
                                                    activeSelectionText = null
                                                }
                                            }
                                    )
                                    
                                    // Mint Green
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color(0xFF81C784), RoundedCornerShape(100.dp))
                                            .border(
                                                BorderStroke(
                                                    if (editingHighlightColorHex == "#81C784") 2.dp else 0.dp,
                                                    if (editingHighlightColorHex == "#81C784") ScholarPrimary else Color.Transparent
                                                ),
                                                RoundedCornerShape(100.dp)
                                            )
                                            .clickable {
                                                editingHighlightColorHex = "#81C784"
                                                onSaveHighlight(
                                                    activeSelectionText!!,
                                                    "#81C784",
                                                    stickyNoteTextInput.ifBlank { null },
                                                    editingHighlightId ?: 0L,
                                                    activeSelectionStartWordIdx ?: 0,
                                                    activeSelectionEndWordIdx ?: 0
                                                )
                                                if (!showStickyNoteEditor) {
                                                    activeSelectionText = null
                                                }
                                            }
                                    )
                                    
                                    // Ocean Blue
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color(0xFF80DEEA), RoundedCornerShape(100.dp))
                                            .border(
                                                BorderStroke(
                                                    if (editingHighlightColorHex == "#80DEEA") 2.dp else 0.dp,
                                                    if (editingHighlightColorHex == "#80DEEA") ScholarPrimary else Color.Transparent
                                                ),
                                                RoundedCornerShape(100.dp)
                                            )
                                            .clickable {
                                                editingHighlightColorHex = "#80DEEA"
                                                onSaveHighlight(
                                                    activeSelectionText!!,
                                                    "#80DEEA",
                                                    stickyNoteTextInput.ifBlank { null },
                                                    editingHighlightId ?: 0L,
                                                    activeSelectionStartWordIdx ?: 0,
                                                    activeSelectionEndWordIdx ?: 0
                                                )
                                                if (!showStickyNoteEditor) {
                                                    activeSelectionText = null
                                                }
                                            }
                                    )

                                    // Soft Purple
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color(0xFFE1BEE7), RoundedCornerShape(100.dp))
                                            .border(
                                                BorderStroke(
                                                    if (editingHighlightColorHex == "#E1BEE7") 2.dp else 0.dp,
                                                    if (editingHighlightColorHex == "#E1BEE7") ScholarPrimary else Color.Transparent
                                                ),
                                                RoundedCornerShape(100.dp)
                                            )
                                            .clickable {
                                                editingHighlightColorHex = "#E1BEE7"
                                                onSaveHighlight(
                                                    activeSelectionText!!,
                                                    "#E1BEE7",
                                                    stickyNoteTextInput.ifBlank { null },
                                                    editingHighlightId ?: 0L,
                                                    activeSelectionStartWordIdx ?: 0,
                                                    activeSelectionEndWordIdx ?: 0
                                                )
                                                if (!showStickyNoteEditor) {
                                                    activeSelectionText = null
                                                }
                                            }
                                    )

                                    // Light Red
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color(0xFFFF8A80), RoundedCornerShape(100.dp))
                                            .border(
                                                BorderStroke(
                                                    if (editingHighlightColorHex == "#FF8A80") 2.dp else 0.dp,
                                                    if (editingHighlightColorHex == "#FF8A80") ScholarPrimary else Color.Transparent
                                                ),
                                                RoundedCornerShape(100.dp)
                                            )
                                            .clickable {
                                                editingHighlightColorHex = "#FF8A80"
                                                onSaveHighlight(
                                                    activeSelectionText!!,
                                                    "#FF8A80",
                                                    stickyNoteTextInput.ifBlank { null },
                                                    editingHighlightId ?: 0L,
                                                    activeSelectionStartWordIdx ?: 0,
                                                    activeSelectionEndWordIdx ?: 0
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
                                            val clip = android.content.ClipData.newPlainText("Madrassah Manuscript", activeSelectionText)
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
                                    
                                    // Translation
                                    IconButton(
                                        onClick = {
                                            onTranslateText(activeSelectionText!!)
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
                                                editingHighlightId ?: 0L,
                                                activeSelectionStartWordIdx ?: 0,
                                                activeSelectionEndWordIdx ?: 0
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
    val bookmarks by viewModel.currentBookBookmarks.collectAsState()

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
                text = { Text("الترجمة (OCR)", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onTabChange(1) },
                icon = { Icon(Icons.Default.Mic, contentDescription = "Voicenotes") },
                text = { Text("المُذَكِّرَة", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { onTabChange(2) },
                icon = { Icon(Icons.Default.Bookmarks, contentDescription = "Bookmarks") },
                text = { Text("الإشارات", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1) }
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

                2 -> {
                    // Bookmarks List panel
                    BookmarksPanel(
                        bookmarks = bookmarks,
                        onGoToPage = { pageIndex -> viewModel.goToPage(pageIndex, context) },
                        onDeleteBookmark = { id -> viewModel.deleteBookmark(id) }
                    )
                }
            }
        }
    }
}

/**
 * 3b. SAVED BOOKMARKS LIST PANEL
 */
@Composable
fun BookmarksPanel(
    bookmarks: List<com.example.data.Bookmark>,
    onGoToPage: (Int) -> Unit,
    onDeleteBookmark: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "الإشارات المرجعية المحفوظة",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = ScholarPrimary,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = "سجل الإشارات المحفوظة في المخطوطة للرجوع السريع.",
                fontSize = 12.sp,
                color = ScholarSecondary.copy(alpha = 0.8f),
                fontFamily = FontFamily.Serif
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (bookmarks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "No Bookmarks",
                            tint = ScholarSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "لا توجد إشارات مرجعية مضافة حالياً",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = ScholarSecondary.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Serif
                        )
                    }
                }
            }
        } else {
            items(bookmarks) { bookmark ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ClassicPaper),
                    border = BorderStroke(1.dp, NaturalSand),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Bookmark Marker",
                                tint = Color(0xFFFBC02D),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "صفحة ${bookmark.pageNumber + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = DarkText,
                                    fontFamily = FontFamily.Serif
                                )
                                Text(
                                    text = "خطوة حفظ سريعة",
                                    fontSize = 11.sp,
                                    color = ScholarSecondary.copy(alpha = 0.7f),
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { onGoToPage(bookmark.pageNumber) },
                                colors = ButtonDefaults.textButtonColors(contentColor = ScholarPrimary),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowCircleRight,
                                    contentDescription = "Jump",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("انتقال (Go)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = { onDeleteBookmark(bookmark.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remove Bookmark",
                                    tint = Color(0xFFA13333),
                                    modifier = Modifier.size(18.dp)
                                )
							}
                        }
                    }
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
                            fontSize = 13.sp,
                            color = DarkText,
                            lineHeight = 18.sp
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
                        Text(
                            text = err,
                            color = Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
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
