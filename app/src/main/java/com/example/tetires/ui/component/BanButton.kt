package com.example.tetires.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BanButton(
    statusAus: Boolean?, // null = belum dicek, true = aus (merah), false = aman (hijau)
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = when (statusAus) {
        null -> Color.LightGray
        true -> Color(0xFFE33629)
        false -> Color(0xFF10B981)
    }
    Box(
        modifier = modifier
            .size(48.dp)
            .background(color = color, shape = CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (statusAus) {
                null -> "?"
                true -> "AUS"
                false -> "OK"
            },
            color = Color.White
        )
    }
}
