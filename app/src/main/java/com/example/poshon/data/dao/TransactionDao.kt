package com.example.poshon.data.dao

import androidx.room.*
import com.example.poshon.data.entity.TransactionEntity

@Dao
interface TransactionDao {

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity)

    // Mengambil semua data (urutan terbaru di atas)
    @Query("SELECT * FROM transactions ORDER BY id DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("SELECT SUM(total) FROM transactions")
    suspend fun getTotalIncome(): Int?

    // Menghapus SEMUA data (Tombol Reset)
    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    // Menghapus SATU item saja (Tombol Sampah)
    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    // --- TAMBAHAN BARU (PENTING) ---

    // 1. Update: Digunakan saat kita menambah Qty (misal dari 1 jadi 2)
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    // 2. Cek Barang: Mencari apakah produk ID ini sudah ada di keranjang?
    @Query("SELECT * FROM transactions WHERE productId = :productId LIMIT 1")
    suspend fun getTransactionByProductId(productId: Int): TransactionEntity?
}