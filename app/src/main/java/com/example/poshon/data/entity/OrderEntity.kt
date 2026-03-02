package com.example.poshon.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val time: String,
    val totalAmount: Int,
    val paymentMethod: String,
    val customerName: String = "Umum" // ⬅️ KOLOM BARU (Default "Umum")
)