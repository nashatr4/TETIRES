package com.example.tetires.ui.screen

import androidx.compose.foundation.Image
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
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavController
import com.example.tetires.R
import com.example.tetires.ui.viewmodel.MainViewModel
import com.example.tetires.data.model.PosisiBan


enum class StatusPengecekan {
    BelumDicek,
    TidakAus,
    Aus
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CekBanScreen(
    navController: NavController,
    viewModel: MainViewModel,
    idCek: Long
) {
    var statusBan by remember {
        mutableStateOf(
            mapOf(
                PosisiBan.DKI to StatusPengecekan.BelumDicek,
                PosisiBan.DKA to StatusPengecekan.BelumDicek,
                PosisiBan.BKI to StatusPengecekan.BelumDicek,
                PosisiBan.BKA to StatusPengecekan.BelumDicek
            )
        )
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
                    val currentStatus = statusBan[posisi]!!
                    val nextStatus = when (currentStatus) {
                        StatusPengecekan.BelumDicek -> StatusPengecekan.TidakAus
                        StatusPengecekan.TidakAus -> StatusPengecekan.Aus
                        StatusPengecekan.Aus -> StatusPengecekan.BelumDicek
                    }
                    statusBan = statusBan + (posisi to nextStatus)

                    // ✅ kirim posisi yang diklik ke ViewModel
                    viewModel.updateCheckPartial(
                        idCek = idCek,
                        posisi = posisi, // diperbaiki dari posisiBan → posisi
                        ukuran = 1.5f
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { navController.navigate("detailPengecekan/$idCek") },
                modifier = Modifier.padding(bottom = 100.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3949A3))
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
    onTireClick: (PosisiBan) -> Unit
) {
    ConstraintLayout(modifier = modifier.fillMaxSize()) {
        val (busImage, iconDKI, iconDKA, iconBKI, iconBKA) = createRefs()

        val topGuideline = createGuidelineFromTop(0.3f)
        val bottomGuideline = createGuidelineFromBottom(0.3f)
        val startGuideline = createGuidelineFromStart(0.1f)
        val endGuideline = createGuidelineFromEnd(0.1f)

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
            status = statusBan.getValue(PosisiBan.DKI),
            onClick = { onTireClick(PosisiBan.DKI) }
        )

        StatusIcon(
            modifier = Modifier.constrainAs(iconDKA) {
                top.linkTo(topGuideline)
                end.linkTo(endGuideline)
            },
            status = statusBan.getValue(PosisiBan.DKA),
            onClick = { onTireClick(PosisiBan.DKA) }
        )

        StatusIcon(
            modifier = Modifier.constrainAs(iconBKI) {
                bottom.linkTo(bottomGuideline)
                start.linkTo(startGuideline)
            },
            status = statusBan.getValue(PosisiBan.BKI),
            onClick = { onTireClick(PosisiBan.BKI) }
        )

        StatusIcon(
            modifier = Modifier.constrainAs(iconBKA) {
                bottom.linkTo(bottomGuideline)
                end.linkTo(endGuideline)
            },
            status = statusBan.getValue(PosisiBan.BKA),
            onClick = { onTireClick(PosisiBan.BKA) }
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
            StatusPengecekan.TidakAus -> Icons.Default.CheckCircle to Color(0xFF10B981)
            StatusPengecekan.Aus -> Icons.Default.Cancel to Color(0xFFEF4444)
        }
        Icon(
            imageVector = icon,
            contentDescription = "Status Ban",
            tint = color,
            modifier = Modifier.size(40.dp)
        )
    }
}
