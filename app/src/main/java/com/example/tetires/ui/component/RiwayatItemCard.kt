package com.example.tetires.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tetires.data.model.PengecekanRingkas
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

@Composable
fun RiwayatItemCard(
    item: PengecekanRingkas,
    onDetail: (Long) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.tanggalReadable)
            }
            Spacer(modifier = Modifier.width(8.dp))
            // 4 small status dots / labels
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusDot(item.statusDki)
                StatusDot(item.statusDka)
                StatusDot(item.statusBki)
                StatusDot(item.statusBka)
            }
            IconButton(onClick = { onDetail(item.idCek) }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Detail")
            }
        }
    }
}

@Composable
private fun StatusDot(status: Boolean?) {
    val color = when (status) {
        null -> Color.LightGray
        true -> Color(0xFFE33629)
        false -> Color(0xFF10B981)
    }
    Box(modifier = Modifier.size(14.dp).background(color = color, shape = RoundedCornerShape(7.dp)))
}
