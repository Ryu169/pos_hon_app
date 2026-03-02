package com.example.poshon.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val price: Int,
    val stock: Int = 0,
    // TAMBAHAN BARU: Menyimpan alamat foto di HP
    val imageUri: String? = null,
    val vendor: String //Kolom Penanda UMKM ("HON" atau "BERSUA")
)