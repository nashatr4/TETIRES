package com.example.tetires.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bus",
    indices = [Index(value = ["platNomor"], unique = true)]
)
data class Bus(
    @PrimaryKey(autoGenerate = true)
    val idBus: Long = 0,
    val namaBus: String,
    val platNomor: String
)
