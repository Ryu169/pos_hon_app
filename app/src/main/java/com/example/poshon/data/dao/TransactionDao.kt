package com.example.poshon.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.poshon.data.entity.TransactionEntity

@Dao
interface TransactionDao {

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("SELECT SUM(total) FROM transactions")
    suspend fun getTotalIncome(): Int?
}
