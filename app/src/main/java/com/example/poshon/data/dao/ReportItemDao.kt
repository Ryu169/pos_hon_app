package com.example.poshon.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.poshon.data.entity.ReportItemEntity

@Dao
interface ReportItemDao {
    @Insert
    suspend fun insertReportItem(item: ReportItemEntity)

    // Ambil semua laporan
    @Query("SELECT * FROM report_items ORDER BY id DESC")
    suspend fun getAllReports(): List<ReportItemEntity>

    // 🌟 LOGIC UTAMA: Ambil laporan KHUSUS vendor tertentu
    @Query("SELECT * FROM report_items WHERE vendor = :vendorName ORDER BY id DESC")
    suspend fun getReportsByVendor(vendorName: String): List<ReportItemEntity>

    // Hitung Total Omset per Vendor (Legacy)
    @Query("SELECT SUM(totalPrice) FROM report_items WHERE vendor = :vendorName")
    suspend fun getTotalRevenueByVendor(vendorName: String): Int?

    // --- TAMBAHAN BARU UNTUK DETAIL TRANSAKSI ---
    @Query("SELECT * FROM report_items WHERE date = :date AND time = :time")
    suspend fun getItemsByDateTime(date: String, time: String): List<ReportItemEntity>

    // --- TAMBAHAN BARU UNTUK SUMMARY DINAMIS ---

    // 1. Ambil semua nama vendor yang unik (Distinct) untuk Dropdown
    @Query("SELECT DISTINCT vendor FROM report_items")
    suspend fun getAllVendors(): List<String>

    // 2. Hitung Total Omset Keseluruhan (Semua Vendor digabung)
    @Query("SELECT SUM(totalPrice) FROM report_items")
    suspend fun getGrandTotal(): Int?

    // 3. Hitung Total Omset Spesifik per Vendor (Dipakai di SummaryActivity)
    @Query("SELECT SUM(totalPrice) FROM report_items WHERE vendor = :vendorName")
    suspend fun getVendorTotal(vendorName: String): Int?
}