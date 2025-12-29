package com.example.poshon.data.dao

import androidx.room.*
import com.example.poshon.data.entity.ProductEntity

@Dao
interface ProductDao {

    // CREATE
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    // READ
    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllProducts(): List<ProductEntity>

    // UPDATE
    @Update
    suspend fun updateProduct(product: ProductEntity)

    // DELETE (1 item)
    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    // DELETE ALL
    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()
}
