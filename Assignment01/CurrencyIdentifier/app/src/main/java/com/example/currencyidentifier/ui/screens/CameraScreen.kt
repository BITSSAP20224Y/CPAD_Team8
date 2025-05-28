package com.example.currencyidentifier.ui.screens

import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup.LayoutParams // Correct import for LayoutParams
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView // Import PreviewView directly
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign // Correct import for TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.currencyidentifier.ui.viewmodel.CurrencyViewModel
import com.example.currencyidentifier.utils.BitmapUtils

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "CameraScreen"

@Composable
fun CameraScreen(
    viewModel: CurrencyViewModel, // <<< Ensure viewModel is here
    onImageCaptured: (Bitmap) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val isLoading by viewModel.isLoading.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState()

    // Create and remember the PreviewView
    val previewView = remember { PreviewView(context) }

    DisposableEffect(previewView) { // Key on previewView so it re-runs if previewView changes (unlikely here)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider) // Directly use the PreviewView's surfaceProvider
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                viewModel.setErrorMessage("Camera binding failed: ${exc.message}")
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = {
                    previewView.apply { // Use the already created and remembered previewView
                        layoutParams = LayoutParams( // Correct LayoutParams import
                            LayoutParams.MATCH_PARENT,
                            LayoutParams.MATCH_PARENT
                        )
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Back button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = MaterialTheme.colorScheme.onPrimary // Or onSurface if it's dark text on light background
                )
            }

            // Capture button
            FloatingActionButton(
                onClick = {
                    Log.d(TAG, "Capture button clicked.")
                    viewModel.setIsLoading(true) // <<< Calling viewModel's method
                    imageCapture?.takePicture(
                        cameraExecutor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                Log.d(TAG, "Image capture success. Format: ${image.format}")
                                val bitmap = BitmapUtils.imageProxyToBitmap(image)
                                Log.d(TAG, "Converted bitmap to ARGB_8888. Size: ${bitmap.width}x${bitmap.height}, Config: ${bitmap.config}")
                                image.close()
                                onImageCaptured(bitmap)
                                viewModel.setIsLoading(false) // <<< Calling viewModel's method
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e(TAG, "Image capture failed: ${exception.message}", exception)
                                viewModel.setErrorMessage("Image capture failed: ${exception.message}") // <<< Calling viewModel's method
                                viewModel.setIsLoading(false) // <<< Calling viewModel's method
                            }
                        }
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Camera, contentDescription = "Capture image")
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (errorMessage != null) {
                Text(
                    text = "Error: ${errorMessage!!}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 64.dp)
                        .padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}