package com.example.data

import kotlinx.coroutines.flow.Flow

class BookRepository(private val bookDao: BookDao) {
    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()

    suspend fun insertBook(book: Book): Long = bookDao.insertBook(book)

    suspend fun deleteBook(bookId: Long) = bookDao.deleteBook(bookId)

    fun getPageStateFlow(bookId: Long, pageNumber: Int): Flow<PageState?> =
        bookDao.getPageStateFlow(bookId, pageNumber)

    suspend fun getPageState(bookId: Long, pageNumber: Int): PageState? =
        bookDao.getPageState(bookId, pageNumber)

    suspend fun savePageState(pageState: PageState) = bookDao.insertPageState(pageState)

    suspend fun saveNoteText(bookId: Long, pageNumber: Int, noteText: String?) {
        val existing = bookDao.getPageState(bookId, pageNumber)
        if (existing != null) {
            bookDao.insertPageState(existing.copy(noteText = noteText, updatedAt = System.currentTimeMillis()))
        } else {
            bookDao.insertPageState(PageState(bookId = bookId, pageNumber = pageNumber, noteText = noteText))
        }
    }

    suspend fun saveExtractedText(bookId: Long, pageNumber: Int, extractedText: String?) {
        val existing = bookDao.getPageState(bookId, pageNumber)
        if (existing != null) {
            bookDao.insertPageState(existing.copy(extractedText = extractedText, updatedAt = System.currentTimeMillis()))
        } else {
            bookDao.insertPageState(PageState(bookId = bookId, pageNumber = pageNumber, extractedText = extractedText))
        }
    }

    fun getHighlightsForPage(bookId: Long, pageNumber: Int): Flow<List<Highlight>> =
        bookDao.getHighlightsForPage(bookId, pageNumber)

    fun getAllHighlightsForBook(bookId: Long): Flow<List<Highlight>> =
        bookDao.getAllHighlightsForBook(bookId)

    suspend fun addHighlight(highlight: Highlight): Long = bookDao.insertHighlight(highlight)

    suspend fun removeHighlight(id: Long) = bookDao.deleteHighlight(id)

    // Bookmarks and Last Page Tracker Integration
    fun getBookmarksForBook(bookId: Long): Flow<List<Bookmark>> = bookDao.getBookmarksForBook(bookId)

    suspend fun getBookmarkForPage(bookId: Long, pageNumber: Int): Bookmark? = bookDao.getBookmarkForPage(bookId, pageNumber)

    suspend fun addBookmark(bookmark: Bookmark): Long = bookDao.insertBookmark(bookmark)

    suspend fun removeBookmarkForPage(bookId: Long, pageNumber: Int) = bookDao.deleteBookmarkForPage(bookId, pageNumber)

    suspend fun removeBookmark(id: Long) = bookDao.deleteBookmark(id)

    suspend fun updateLastReadPage(bookId: Long, pageNumber: Int) = bookDao.updateLastReadPage(bookId, pageNumber)
}
