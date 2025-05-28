package com.example.currencyidentifier.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.media.Image
import android.net.Uri
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

object BitmapUtils {

    fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        // Log image format for debugging
        println("Image format: ${image.format}")

        val bitmap: Bitmap? = when (image.format) {
            ImageFormat.JPEG -> {
                val buffer = image.planes[0].buffer // JPEG has only one plane
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            ImageFormat.YUV_420_888 -> {
                // This is a common format for camera previews.
                // Converting YUV to Bitmap is more complex and requires specific logic.
                // For simplicity, let's just log a warning for now if you expect JPEG.
                // If you *do* need to handle YUV, you'll need a more robust conversion.
                // A full YUV to RGB conversion would look something like this:
                /*
                val yBuffer = image.planes[0].buffer
                val uBuffer = image.planes[1].buffer
                val vBuffer = image.planes[2].buffer

                val ySize = yBuffer.remaining()
                val uSize = uBuffer.remaining()
                val vSize = vBuffer.remaining()

                val nv21 = ByteArray(ySize + uSize + vSize)

                yBuffer.get(nv21, 0, ySize)
                vBuffer.get(nv21, ySize, vSize)
                uBuffer.get(nv21, ySize + vSize, uSize)

                val yuvImage = android.graphics.YuvImage(
                    nv21, ImageFormat.NV21, image.width, image.height, null
                )
                val out = ByteArrayOutputStream()
                yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 75, out)
                BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
                */
                println("Warning: Image format is YUV_420_888. Direct conversion to Bitmap from this format needs more complex logic.")
                null // Return null or implement YUV conversion
            }
            else -> {
                println("Unsupported image format: ${image.format}")
                null
            }
        }

        // Apply rotation if needed (from the original logic)
        return bitmap?.let {
            val matrix = Matrix()
            // Adjust rotation based on image.imageInfo.rotationDegrees if necessary
            // For back camera, 90 or 270 degrees might be common
            // For front camera, consider mirroring as well
            matrix.postRotate(image.imageInfo.rotationDegrees.toFloat()) // Apply rotation
            Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
        } ?: throw IllegalArgumentException("Could not convert ImageProxy to Bitmap for format ${image.format}")
    }

    /**
     * Converts a content URI to a Bitmap.
     * @param context The application context.
     * @param uri The content URI of the image.
     * @return The Bitmap, or null if conversion fails.
     */
    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun bitmapToByteBuffer(bitmap: Bitmap, width: Int, height: Int, byteBuffer: ByteBuffer) {
        byteBuffer.rewind() // Rewind buffer to start
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (pixel in pixels) {
            // Extract R, G, B channels and normalize to [0, 1]
            // Assuming AARRGGBB format for pixel
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            // Depending on your model's input, you might put them as Float or Byte
            // For example, if your model expects float inputs (e.g., 0.0 to 1.0)
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
            // If your model expects byte inputs (e.g., 0 to 255)
            // byteBuffer.put(((pixel shr 16) and 0xFF).toByte())
            // byteBuffer.put(((pixel shr 8) and 0xFF).toByte())
            // byteBuffer.put((pixel and 0xFF).toByte())
        }
    }
}