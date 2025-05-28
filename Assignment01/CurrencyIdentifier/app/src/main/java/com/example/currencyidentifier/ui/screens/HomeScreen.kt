package com.example.currencyidentifier.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.currencyidentifier.R
import com.example.currencyidentifier.ui.theme.CurrencyIdentifierTheme
import com.example.currencyidentifier.ui.viewmodel.CurrencyViewModel
import com.example.currencyidentifier.utils.BitmapUtils
import com.example.currencyidentifier.data.CurrencyDetectionResult
import com.example.currencyidentifier.data.CurrencyRepository

@Composable
fun HomeScreen(
    onCapturePhotoClick: () -> Unit,
    onUploadImageClick: (Bitmap) -> Unit,
    viewModel: CurrencyViewModel
) {
    val context = LocalContext.current

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle results if needed, though for now we rely on user granting them
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
            )
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
            )
        }
    }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bitmap = BitmapUtils.getBitmapFromUri(context, uri)
            if (bitmap != null) {
                onUploadImageClick(bitmap)
            } else {
                viewModel.setErrorMessage("Failed to load image from URI.")
            }
        } else {
            viewModel.setErrorMessage("Image selection cancelled.")
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "App Logo",
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Currency Identifier",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(64.dp))
        Button(onClick = onCapturePhotoClick) {
            Text("Capture Photo")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        ) {
            Text("Upload Image")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    CurrencyIdentifierTheme {
        val context = LocalContext.current

        val mockRepository = object : CurrencyRepository(context) {
            override suspend fun detectCurrency(bitmap: Bitmap): CurrencyDetectionResult {
                // Corrected: Pass 'null' for processedBitmap or an actual mock Bitmap if you have one.
                return CurrencyDetectionResult("Mock Currency", 0.99f, null) // CHANGED HERE
            }

            override fun closeDetector() {
                // Do nothing for preview
            }
        }

        val mockViewModel = CurrencyViewModel(mockRepository)

        HomeScreen(
            onCapturePhotoClick = {},
            onUploadImageClick = {},
            viewModel = mockViewModel
        )
    }
}