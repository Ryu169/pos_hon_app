package com.example.poshon.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.poshon.data.entity.TransactionEntity

@Dao
interface TransactionDao {

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("SELECT SUM(total) FROM transactions")
    suspend fun getTotalIncome(): Int?

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

}
