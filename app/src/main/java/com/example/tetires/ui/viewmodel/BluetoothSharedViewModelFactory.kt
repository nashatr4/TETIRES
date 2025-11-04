package com.example.tetires.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory untuk membuat instance BluetoothSharedViewModel
 * agar bisa menerima Application sebagai parameter.
 */
class BluetoothSharedViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BluetoothSharedViewModel(application) as T
    }
}

