package com.example.poshon.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.poshon.data.entity.OrderEntity

@Dao
interface OrderDao {
    @Insert
    suspend fun insertOrder(order: OrderEntity)

    @Query("SELECT * FROM orders ORDER BY id DESC")
    suspend fun getAllOrders(): List<OrderEntity>

    @Query("SELECT SUM(totalAmount) FROM orders")
    suspend fun getTotalRevenue(): Int?
}