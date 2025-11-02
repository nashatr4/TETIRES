package com.example.tetires.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class TerminalViewModel(private val context: Context) : ViewModel() {

    var terminalText = mutableStateOf("")
    var lastCheck = mutableStateOf("")

    private var lastClearedLog = ""

    fun addLog(text: String) {
        terminalText.value += "\n$text"
        // jangan update lastCheck di sini
    }

    fun clearLogs() {
        if (terminalText.value.isNotEmpty()) {
            lastClearedLog = terminalText.value // simpan log sebelum hapus
            lastCheck.value = lastClearedLog // langsung simpan full log
            terminalText.value = "" // kosongkan terminal
        }
    }

    fun restoreLastLogs() {
        if (lastClearedLog.isNotEmpty()) {
            terminalText.value = lastClearedLog
            lastClearedLog = "" // reset biar tombol undo gak terus muncul
        }
    }
}
