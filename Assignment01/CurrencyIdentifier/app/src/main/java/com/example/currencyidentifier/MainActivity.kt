package com.example.currencyidentifier

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.currencyidentifier.data.CurrencyRepository
import com.example.currencyidentifier.ui.screens.CameraScreen
import com.example.currencyidentifier.ui.screens.HomeScreen
import com.example.currencyidentifier.ui.screens.ResultScreen
import com.example.currencyidentifier.ui.theme.CurrencyIdentifierTheme
import com.example.currencyidentifier.ui.viewmodel.CurrencyViewModel
import com.example.currencyidentifier.ui.viewmodel.CurrencyViewModelFactory
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale // Import Locale for TTS language setting

class MainActivity : ComponentActivity(), OnInitListener { // Implement OnInitListener

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions result if needed, though Accompanist handles much of it
    }

    private lateinit var tts: TextToSpeech // Declare TextToSpeech object

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize TextToSpeech engine
        tts = TextToSpeech(this, this)

        // Request permissions on startup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
            )
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
            )
        }

        setContent {
            CurrencyIdentifierTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val currencyViewModel: CurrencyViewModel = viewModel(
                        factory = CurrencyViewModelFactory(CurrencyRepository(applicationContext))
                    )

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onCapturePhotoClick = { navController.navigate("camera") },
                                onUploadImageClick = { bitmap ->
                                    currencyViewModel.detectCurrency(bitmap)
                                    navController.navigate("result")
                                },
                                viewModel = currencyViewModel
                            )
                        }
                        composable("camera") {
                            CameraScreen(
                                viewModel = currencyViewModel,
                                onImageCaptured = { bitmap ->
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        currencyViewModel.detectCurrency(bitmap)
                                        navController.navigate("result")
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("result") {
                            ResultScreen(
                                viewModel = currencyViewModel,
                                onBackToHome = {
                                    currencyViewModel.clearDetectionResult()
                                    navController.popBackStack("home", inclusive = false)
                                },
                                onSpeakText = { text -> // Pass the speakOut function
                                    speakOut(text)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Callback for TextToSpeech initialization
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set language to US English. For Indian English or other languages,
            // you might need to try different locales or handle missing data.
            // Example for Indian English: tts.setLanguage(Locale("en", "IN"))
            val result = tts.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Inform the user if the language data is missing or not supported
                Toast.makeText(this, "Text-to-Speech language not supported or data missing.", Toast.LENGTH_LONG).show()
            }
        } else {
            // Inform the user if TTS initialization failed
            Toast.makeText(this, "Text-to-Speech initialization failed.", Toast.LENGTH_LONG).show()
        }
    }

    // Function to speak out text
    private fun speakOut(text: String) {
        if (::tts.isInitialized) {
            // Stop any ongoing speech before starting a new one
            if (tts.isSpeaking) {
                tts.stop()
            }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    override fun onDestroy() {
        // Shutdown TextToSpeech engine to release resources
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}