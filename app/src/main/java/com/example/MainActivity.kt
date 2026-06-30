package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.CalculationRepository
import com.example.ui.CalculatorAppContent
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CalculatorViewModel
import com.example.viewmodel.CalculatorViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room DB, Repository and ViewModel
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.calculationDao()
        val repository = CalculationRepository(dao)
        val factory = CalculatorViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[CalculatorViewModel::class.java]

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                CalculatorAppContent(viewModel = viewModel)
            }
        }
    }
}
