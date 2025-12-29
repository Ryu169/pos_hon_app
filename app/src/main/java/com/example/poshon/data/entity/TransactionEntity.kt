package com.example.poshon.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val productName: String,
    val quantity: Int,
    val price: Int,
    val total: Int
)
