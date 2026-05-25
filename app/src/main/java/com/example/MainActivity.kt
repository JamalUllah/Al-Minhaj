package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.AppDatabase
import com.example.data.BookRepository
import com.example.ui.MadrassahReaderApp
import com.example.ui.ReaderViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room persistence layer
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = BookRepository(database.bookDao())
        
        // Instantiate the centralized study viewModel
        val viewModel: ReaderViewModel by viewModels {
            ReaderViewModel.Factory(repository)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MadrassahReaderApp(viewModel = viewModel)
            }
        }
    }
}
