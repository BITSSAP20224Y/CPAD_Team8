package com.example.currencyidentifier.data

import android.content.Context
import android.graphics.Bitmap
import com.example.currencyidentifier.ml.CurrencyDetector // This now refers to the auto-generated class
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException

// Add the 'open' keyword here to allow inheritance
open class CurrencyRepository(private val context: Context) {

    // Define the labels that match your model's output order
    // ENSURE THESE MATCH THE EXACT LABELS IN YOUR model's labels.txt file!
    // AND ALSO MATCH THE CASES IN ResultScreen.kt
    private val labels = listOf(
        "ten", "twenty", "fifty", "hundred", "two hundred", "five hundred", "two thousand", // Corrected
        "one", "five" // Assuming you also have one and five rupee notes
    )

    // Define model input dimensions
    private val imageSizeX = 224
    private val imageSizeY = 224

    // Define a confidence threshold for displaying results
    private val confidenceThreshold = 0.7f // Adjust as needed

    // Initialize the auto-generated model using newInstance()
    private val currencyDetector: CurrencyDetector? by lazy {
        try {
            // This is how you instantiate the auto-generated TFLite model
            CurrencyDetector.newInstance(context)
        } catch (e: IOException) {
            // Log the error or handle it appropriately (e.g., show a Toast to the user)
            e.printStackTrace()
            null // Return null if initialization fails
        }
    }

    // Image preprocessing pipeline
    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(imageSizeY, imageSizeX, ResizeOp.ResizeMethod.BILINEAR))
        .add(NormalizeOp(0f, 255f)) // Assuming your model expects pixel values normalized from 0-255 to 0-1
        // Adjust NormalizeOp values if your model was trained differently (e.g., (127.5f, 127.5f) for -1 to 1)
        .build()

    // Function to perform currency detection asynchronously
    // Add the 'open' keyword here to allow overriding
    open suspend fun detectCurrency(bitmap: Bitmap): CurrencyDetectionResult {
        return withContext(Dispatchers.Default) { // Changed to Dispatchers.Default for CPU-bound tasks
            if (currencyDetector == null) {
                // Corrected to pass Float for confidence and the bitmap
                return@withContext CurrencyDetectionResult(
                    currencyName = "Unknown",
                    confidence = 0f, // Placeholder float value for confidence
                    processedBitmap = bitmap
                )
            }

            // Step 1: Convert and preprocess image
            val tensorImage = TensorImage(DataType.FLOAT32) // Use FLOAT32 as per NormalizeOp
            tensorImage.load(bitmap)
            val processedImage = imageProcessor.process(tensorImage)

            // Step 2: Prepare input buffer for the model
            val inputFeature0 = TensorBuffer.createFixedSize(
                intArrayOf(1, imageSizeY, imageSizeX, 3), // Assuming batch_size=1, height, width, channels=3
                DataType.FLOAT32 // Or UINT8 if your model expects quantized input
            )
            inputFeature0.loadBuffer(processedImage.buffer) // Load the byte buffer from the processed TensorImage

            // Step 3: Run inference using the auto-generated process method
            val outputs = currencyDetector!!.process(inputFeature0)

            // Step 4: Extract results from the output
            val probabilities = outputs.outputFeature0AsTensorBuffer.floatArray

            // Step 5: Get predicted label and confidence
            val maxConfidence = probabilities.maxOrNull() ?: 0f
            val maxIdx = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1

            val detectedLabel = if (maxIdx in labels.indices && maxConfidence >= confidenceThreshold) {
                labels[maxIdx]
            } else {
                "Unknown" // Or "Not detected"
            }

            return@withContext CurrencyDetectionResult(
                currencyName = detectedLabel, // Use 'currencyName' to match the data class
                confidence = maxConfidence,   // Provide the 'confidence' value
                processedBitmap = bitmap      // Provide the 'processedBitmap' value
            )
        }
    }

    // Function to clean up resources when the repository is no longer needed
    // Add the 'open' keyword here to allow overriding
    open fun closeDetector() {
        currencyDetector?.close() // Use safe call operator for nullable detector
    }
}