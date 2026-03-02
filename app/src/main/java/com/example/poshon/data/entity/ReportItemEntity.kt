package com.example.poshon.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "report_items")
data class ReportItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val time: String,      // ⬅️ BARU: Kolom Jam
    val productName: String,
    val vendor: String,
    val quantity: Int,
    val totalPrice: Int
)