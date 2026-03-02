package com.example.poshon.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.poshon.data.dao.OrderDao
import com.example.poshon.data.dao.ProductDao
import com.example.poshon.data.dao.TransactionDao
import com.example.poshon.data.dao.ReportItemDao // 1. Import DAO Baru
import com.example.poshon.data.entity.OrderEntity
import com.example.poshon.data.entity.ProductEntity
import com.example.poshon.data.entity.TransactionEntity
import com.example.poshon.data.entity.ReportItemEntity // 2. Import Entity Baru

@Database(
    entities = [
        ProductEntity::class,
        TransactionEntity::class,
        OrderEntity::class,
        ReportItemEntity::class // 3. WAJIB DITAMBAHKAN DI SINI
    ],
    version = 6, // 4. Versi dinaikkan ke 3 agar tabel baru terbentuk
    exportSchema = false
)
abstract class PosDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun orderDao(): OrderDao
    abstract fun reportItemDao(): ReportItemDao // 5. Daftarkan fungsi DAO di sini

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
                    .fallbackToDestructiveMigration() // Aman: Hapus data lama dan buat struktur baru
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}