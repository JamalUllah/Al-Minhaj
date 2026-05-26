package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    // Books
    @Query("SELECT * FROM books ORDER BY addedAt DESC")
    fun getAllBooks(): Flow<List<Book>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBook(bookId: Long)

    // Page States (Notes/Extracted Text)
    @Query("SELECT * FROM page_states WHERE bookId = :bookId AND pageNumber = :pageNumber LIMIT 1")
    fun getPageStateFlow(bookId: Long, pageNumber: Int): Flow<PageState?>

    @Query("SELECT * FROM page_states WHERE bookId = :bookId AND pageNumber = :pageNumber LIMIT 1")
    suspend fun getPageState(bookId: Long, pageNumber: Int): PageState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageState(pageState: PageState)

    // Highlights
    @Query("SELECT * FROM highlights WHERE bookId = :bookId AND pageNumber = :pageNumber ORDER BY createdAt ASC")
    fun getHighlightsForPage(bookId: Long, pageNumber: Int): Flow<List<Highlight>>

    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY pageNumber ASC, createdAt ASC")
    fun getAllHighlightsForBook(bookId: Long): Flow<List<Highlight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: Highlight): Long

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlight(id: Long)

    // Bookmarks
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY pageNumber ASC")
    fun getBookmarksForBook(bookId: Long): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId AND pageNumber = :pageNumber LIMIT 1")
    suspend fun getBookmarkForPage(bookId: Long, pageNumber: Int): Bookmark?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark): Long

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId AND pageNumber = :pageNumber")
    suspend fun deleteBookmarkForPage(bookId: Long, pageNumber: Int)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Long)

    // Last Read Page Tracker
    @Query("UPDATE books SET lastReadPage = :pageNumber WHERE id = :bookId")
    suspend fun updateLastReadPage(bookId: Long, pageNumber: Int)
}
