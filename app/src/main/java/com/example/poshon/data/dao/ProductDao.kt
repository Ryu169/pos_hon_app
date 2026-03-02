package com.example.poshon.data.dao

import androidx.room.*
import com.example.poshon.data.entity.ProductEntity

@Dao
interface ProductDao {

    // CREATE
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    // READ
    // PERBAIKAN: Menggunakan 'products' (pakai 's')
    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllProducts(): List<ProductEntity>

    // UPDATE
    @Update
    suspend fun updateProduct(product: ProductEntity)

    // DELETE (1 item)
    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    // DELETE ALL
    // PERBAIKAN: Menggunakan 'products'
    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    // STOCK
    // PERBAIKAN: Menggunakan 'products'
    @Query("UPDATE products SET stock = stock - :qty WHERE id = :productId")
    suspend fun reduceStock(productId: Int, qty: Int)

    // PERBAIKAN: Menggunakan 'products'
    @Query("SELECT stock FROM products WHERE id = :productId")
    suspend fun getStock(productId: Int): Int?
}