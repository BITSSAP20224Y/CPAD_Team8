package com.example.currencyidentifier.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect // Import LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.currencyidentifier.data.CurrencyDetectionResult
import com.example.currencyidentifier.ui.viewmodel.CurrencyViewModel

@Composable
fun ResultScreen(
    viewModel: CurrencyViewModel,
    onBackToHome: () -> Unit,
    // THIS IS THE CRUCIAL NEW PARAMETER FOR TEXT-TO-SPEECH
    onSpeakText: (String) -> Unit
) {
    val detectionResult by viewModel.detectionResult.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState()
    val capturedImageBitmap by viewModel.capturedImageBitmap.observeAsState() // Observe the processed bitmap

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = onBackToHome,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back to Home")
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Processing image...", style = MaterialTheme.typography.titleLarge)
            } else if (errorMessage != null) {
                Text(
                    text = "Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBackToHome) {
                    Text("Try Again")
                }
            } else if (detectionResult != null) {
                val result = detectionResult as CurrencyDetectionResult

                // Display the captured/processed image
                capturedImageBitmap?.asImageBitmap()?.let { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Captured Image",
                        modifier = Modifier
                            .size(250.dp)
                            .padding(bottom = 16.dp),
                        contentScale = ContentScale.Fit
                    )
                } ?: run {
                    // Fallback if bitmap is null
                    Text("Image preview not available", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                }


                Text(
                    "Detected Currency:",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    "${result.currencyName}", // Now correctly uses currencyName
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                // The confidence percentage will still reflect what your model outputs.
                // The '868.68%' issue needs to be addressed in CurrencyRepository.kt
                // if it's not a display formatting issue.
                Text(
                    "Confidence: ${"%.2f".format(result.confidence * 100)}%", // Now correctly uses confidence
                    style = MaterialTheme.typography.titleMedium
                )

                // --- TEXT-TO-SPEECH INTEGRATION START ---
                // Convert the detected currency name to a spoken phrase
                val amountInWords = when (result.currencyName) {
                    "ten" -> "Ten Indian Rupees"
                    "twenty" -> "Twenty Indian Rupees"
                    "fifty" -> "Fifty Indian Rupees"
                    "hundred" -> "One Hundred Indian Rupees"
                    "two hundred" -> "Two Hundred Indian Rupees" // Updated to match "labels" with spaces
                    "five hundred" -> "Five Hundred Indian Rupees" // Updated to match "labels" with spaces
                    "two thousand" -> "Two Thousand Indian Rupees" // Updated to match "labels" with spaces
                    "one" -> "One Indian Rupee" // Added, assuming it's in your labels.txt
                    "five" -> "Five Indian Rupees" // Added, assuming it's in your labels.txt
                    // Add more currency denominations you expect from your model's labels.txt
                    // Ensure these match the exact string output of your model.
                    else -> "Unknown currency. Please try again." // More descriptive message
                }

                Text(
                    text = "Amount in words: $amountInWords",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )

                // Use LaunchedEffect to trigger speech when the result is successfully displayed
                // and the 'amountInWords' changes.
                LaunchedEffect(amountInWords) {
                    // Only speak if a meaningful amount is detected
                    if (amountInWords != "Unknown currency. Please try again.") {
                        onSpeakText(amountInWords)
                    } else {
                        // Optionally, speak a message for unknown currency as well
                        onSpeakText("No familiar currency detected.")
                    }
                }
                // --- TEXT-TO-SPEECH INTEGRATION END ---

                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onBackToHome) {
                    Text("Back to Home")
                }
            } else {
                Text(
                    "No detection result available.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBackToHome) {
                    Text("Go Home")
                }
            }
        }
    }
}