package com.example.tetires

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

enum class PosisiBan{
    D_KI, D_KA, B_KI, B_KA
}

enum class StatusPengecekan{
    BelumDicek,
    Berhasil,
    Gagal
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CekBan(navController: NavController){
    var statusBan by remember {
        mutableStateOf(mapOf(
            PosisiBan.D_KI to StatusPengecekan.BelumDicek,
            PosisiBan.D_KA to StatusPengecekan.BelumDicek,
            PosisiBan.B_KI to StatusPengecekan.BelumDicek,
            PosisiBan.B_KA to StatusPengecekan.BelumDicek
        ))
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Cek Ban",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BusLayout(
                modifier = Modifier.weight(1f),
                statusBan = statusBan,
                onTireClick = { posisi ->
                    val currentStatus = statusBan[posisi]
                    val nextStatus = when (currentStatus) {
                        StatusPengecekan.BelumDicek -> StatusPengecekan.Berhasil
                        StatusPengecekan.Berhasil -> StatusPengecekan.Gagal
                        StatusPengecekan.Gagal -> StatusPengecekan.BelumDicek
                        else -> StatusPengecekan.BelumDicek
                    }
                    statusBan = statusBan + (posisi to nextStatus)
                }
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { /* ke halaman DetailPengecekan */ },
                modifier = Modifier
                    .padding(bottom = 100.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3949A3)
                )
            ) {
                Text("Detail Pengecekan", color = Color.White)
            }
        }
    }
}

@Composable
fun BusLayout(
    modifier: Modifier = Modifier,
    statusBan: Map<PosisiBan, StatusPengecekan>,
    onTireClick: (PosisiBan) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.buscekban),
            contentDescription = "Bus",
            modifier = Modifier.size(400.dp)
        )
        StatusIcon(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 40.dp, y = 120.dp),
            status = statusBan.getValue(PosisiBan.D_KI),
            onClick = { onTireClick(PosisiBan.D_KI) }
        )
        StatusIcon(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-40).dp, y = 120.dp),
            status = statusBan.getValue(PosisiBan.D_KA),
            onClick = { onTireClick(PosisiBan.D_KA) }
        )
        StatusIcon(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 40.dp, y = (-120).dp),
            status = statusBan.getValue(PosisiBan.B_KI),
            onClick = { onTireClick(PosisiBan.B_KI) }
        )
        StatusIcon(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-40).dp, y = (-120).dp),
            status = statusBan.getValue(PosisiBan.B_KA),
            onClick = { onTireClick(PosisiBan.B_KA) }
        )
    }
}

@Composable
fun StatusIcon(
    modifier: Modifier = Modifier,
    status: StatusPengecekan,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = modifier) {
        val (icon, color) = when (status) {
            StatusPengecekan.BelumDicek -> Icons.Default.AddCircle to Color.Black
            StatusPengecekan.Berhasil -> Icons.Default.CheckCircle to Color(0xFF10B981)
            StatusPengecekan.Gagal -> Icons.Default.Cancel to Color(0xFFEF4444)
        }
        Icon(
            imageVector = icon,
            contentDescription = "Status Icon",
            tint = color,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CekBan() {
    CekBan(navController = rememberNavController())
}