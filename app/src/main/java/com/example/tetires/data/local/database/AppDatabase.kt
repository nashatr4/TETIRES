package com.example.tetires.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tetires.data.local.dao.BusDao
import com.example.tetires.data.local.dao.DetailBanDao
import com.example.tetires.data.local.dao.PengecekanDao
import com.example.tetires.data.local.entity.Bus
import com.example.tetires.data.local.entity.DetailBan
import com.example.tetires.data.local.entity.Pengecekan

/**
 * Room Database utama aplikasi.
 */
@Database(
    entities = [
        Bus::class,
        Pengecekan::class,
        DetailBan::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    // DAO
    abstract fun busDao(): BusDao
    abstract fun pengecekanDao(): PengecekanDao
    abstract fun detailBanDao(): DetailBanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tetires.db" // nama file database
                )
                    .fallbackToDestructiveMigration() // reset DB kalau ada perubahan versi
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
