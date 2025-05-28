package com.example.currencyidentifier.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.currencyidentifier.data.CurrencyDetectionResult
import com.example.currencyidentifier.data.CurrencyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CurrencyViewModel(private val repository: CurrencyRepository) : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _detectionResult = MutableLiveData<CurrencyDetectionResult?>(null)
    val detectionResult: LiveData<CurrencyDetectionResult?> = _detectionResult

    private val _capturedImageBitmap = MutableLiveData<Bitmap?>(null)
    val capturedImageBitmap: LiveData<Bitmap?> = _capturedImageBitmap

    // Functions to set loading state and error message.
    // IMPORTANT: If these can be called from a background thread (which they currently are
    // from CameraScreen's onCaptureSuccess), they MUST use postValue.
    // If they were ONLY ever called from the UI (main thread), .value would be fine.
    // To be safe, postValue is used here.
    fun setIsLoading(loading: Boolean) {
        _isLoading.postValue(loading) // Use postValue
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.postValue(message) // Use postValue
    }

    // This function is called from CameraScreen's onCaptureSuccess, which is on cameraExecutor (background thread).
    fun detectCurrency(bitmap: Bitmap) {
        // Initial LiveData updates (for loading state, clearing previous results)
        // MUST use postValue because this method is called from a background thread.
        _isLoading.postValue(true)
        _errorMessage.postValue(null)
        _detectionResult.postValue(null)
        _capturedImageBitmap.postValue(bitmap) // Store the processed bitmap

        viewModelScope.launch {
            try {
                // The repository's detectCurrency is a suspend function.
                // It will likely switch to Dispatchers.Default for heavy ML work,
                // and then return to the launching dispatcher (Dispatchers.Main by default for viewModelScope).
                val result = repository.detectCurrency(bitmap)

                // Once we are back on the main thread (due to viewModelScope.launch's default Dispatchers.Main)
                // or if the suspend function internally returned to main, .value is generally safe.
                // However, to maintain consistency and robust thread safety, using .postValue()
                // here as well is harmless and provides ultimate peace of mind.
                _detectionResult.postValue(result) // Still use postValue for consistency

            } catch (e: Exception) {
                // Error message update must also be on the main thread.
                // Since this catch block is within the viewModelScope.launch, it's on Main.
                // But again, using postValue is safer.
                _errorMessage.postValue("Detection failed: ${e.message}")
                e.printStackTrace()
            } finally {
                // Final loading state update must also be on the main thread.
                _isLoading.postValue(false)
            }
        }
    }

    fun clearDetectionResult() {
        // These methods are typically called from the UI (Main thread),
        // so .value is technically fine. However, for ultimate consistency with
        // how other LiveData updates are handled in this ViewModel, postValue is used.
        _detectionResult.postValue(null)
        _errorMessage.postValue(null)
        _capturedImageBitmap.postValue(null)
    }

    override fun onCleared() {
        super.onCleared()
        repository.closeDetector()
    }
}

class CurrencyViewModelFactory(private val repository: CurrencyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CurrencyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CurrencyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}