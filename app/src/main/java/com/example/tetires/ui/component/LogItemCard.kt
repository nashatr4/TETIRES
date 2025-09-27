package com.example.tetires.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tetires.data.model.LogItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun LogItemCard(
    item: LogItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.tanggalReadable, fontWeight = FontWeight.Medium)
                Text(text = "${item.namaBus} • ${item.platNomor}", color = Color.Gray)
            }
            Text(
                text = item.summaryStatus,
                modifier = Modifier.padding(start = 8.dp),
                color = if (item.summaryStatus == "Aus") Color(0xFFE33629) else Color(0xFF0C5900),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
