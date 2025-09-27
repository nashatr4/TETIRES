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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
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
    ConstraintLayout(
        modifier = modifier
            .fillMaxSize()
    ) {
        val (busImage, iconDKI, iconDKA, iconBKI, iconBKA) = createRefs()

        // Garis bantu horizontal
        val topGuideline = createGuidelineFromTop(0.3f)     // Garis di 30% dari atas
        val bottomGuideline = createGuidelineFromBottom(0.3f) // Garis di 30% dari bawah

        // Garis bantu vertikal
        val startGuideline = createGuidelineFromStart(0.1f) // Garis di 10% dari kiri
        val endGuideline = createGuidelineFromEnd(0.1f)   // Garis di 10% dari kanan

        Image(
            painter = painterResource(id = R.drawable.buscekban),
            contentDescription = "Bus",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .aspectRatio(0.35f)
                .constrainAs(busImage) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )
        StatusIcon(
            modifier = Modifier.constrainAs(iconDKI) {
                top.linkTo(topGuideline)
                start.linkTo(startGuideline)
            },
            status = statusBan.getValue(PosisiBan.D_KI),
            onClick = { onTireClick(PosisiBan.D_KI) }
        )
        StatusIcon(
            modifier = Modifier.constrainAs(iconDKA) {
                top.linkTo(topGuideline)
                end.linkTo(endGuideline)
            },
            status = statusBan.getValue(PosisiBan.D_KA),
            onClick = { onTireClick(PosisiBan.D_KA) }
        )
        StatusIcon(
            modifier = Modifier.constrainAs(iconBKI) {
                bottom.linkTo(bottomGuideline)
                start.linkTo(startGuideline)
            },
            status = statusBan.getValue(PosisiBan.B_KI),
            onClick = { onTireClick(PosisiBan.B_KI) }
        )
        StatusIcon(
            modifier = Modifier.constrainAs(iconBKA) {
                bottom.linkTo(bottomGuideline)
                end.linkTo(endGuideline)
            },
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