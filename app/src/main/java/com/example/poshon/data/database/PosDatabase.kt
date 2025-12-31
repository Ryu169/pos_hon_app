package com.example.poshon.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.poshon.data.dao.ProductDao
import com.example.poshon.data.dao.TransactionDao
import com.example.poshon.data.entity.ProductEntity
import com.example.poshon.data.entity.TransactionEntity

@Database(
    entities = [
        ProductEntity::class,
        TransactionEntity::class
    ],
    version = 2, // ⬅️ NAIKKAN VERSION
    exportSchema = false
)
abstract class PosDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: PosDatabase? = null

        fun getInstance(context: Context): PosDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    PosDatabase::class.java,
                    "pos_db"
                )
                    .fallbackToDestructiveMigration() // ⬅️ PENTING
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

