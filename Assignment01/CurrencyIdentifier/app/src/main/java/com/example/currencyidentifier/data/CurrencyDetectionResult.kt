package com.example.currencyidentifier.data

import android.graphics.Bitmap

data class CurrencyDetectionResult(
    val currencyName: String,
    val confidence: Float,
    val processedBitmap: Bitmap? = null
)