package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String, // File URI or "sample"
    val title: String,
    val addedAt: Long = System.currentTimeMillis(),
    val totalPages: Int = 0
)

@Entity(tableName = "page_states")
data class PageState(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val pageNumber: Int,
    val extractedText: String? = null,
    val noteText: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "highlights")
data class Highlight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val pageNumber: Int,
    val selectedText: String,
    val startOffset: Int = 0,
    val endOffset: Int = 0,
    val colorHex: String = "#FFEB3B", // default yellow
    val comment: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
