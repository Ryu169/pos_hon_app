package com.example.poshon.ui.report

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.poshon.R
import com.example.poshon.data.database.PosDatabase
import com.example.poshon.data.entity.OrderEntity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ReportActivity : AppCompatActivity() {

    private lateinit var adapter: ReportAdapter
    private lateinit var database: PosDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        database = PosDatabase.getInstance(this)

        val recyclerView = findViewById<RecyclerView>(R.id.rvReportList)
        val btnSummary = findViewById<Button>(R.id.btnGoToSummary)

        // Setup Adapter dengan Callback Klik
        adapter = ReportAdapter(emptyList()) { selectedOrder ->
            showDetailDialog(selectedOrder)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnSummary.setOnClickListener {
            startActivity(Intent(this, SummaryActivity::class.java))
        }

        loadTransactionList()
    }

    private fun loadTransactionList() {
        lifecycleScope.launch {
            val rawOrders = database.orderDao().getAllOrders()
            // Grouping data berdasarkan tanggal
            val groupedData = rawOrders.groupBy { it.date }
            val displayList = mutableListOf<ReportListItem>()

            groupedData.forEach { (date, ordersForDay) ->
                val totalDaily = ordersForDay.sumOf { it.totalAmount }
                displayList.add(ReportListItem.DateHeader(date, totalDaily))
                ordersForDay.forEach { order ->
                    displayList.add(ReportListItem.TransactionItem(order))
                }
            }
            adapter.updateData(displayList)
        }
    }

    // --- FUNGSI MENAMPILKAN DETAIL TRANSAKSI (YANG DIUPDATE) ---
    private fun showDetailDialog(order: OrderEntity) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_transaction_detail, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        // Binding View
        val tvId = dialogView.findViewById<TextView>(R.id.tvDetailId)
        val tvDateTime = dialogView.findViewById<TextView>(R.id.tvDetailDateTime)
        val tvItems = dialogView.findViewById<TextView>(R.id.tvDetailItems)
        val tvMethod = dialogView.findViewById<TextView>(R.id.tvDetailMethod)
        val tvTotal = dialogView.findViewById<TextView>(R.id.tvDetailTotal)
        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseDetail)

        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        formatRp.maximumFractionDigits = 0

        // 🌟 UPDATE DI SINI: Ganti Kode TRX dengan Nama Pelanggan
        tvId.text = order.customerName

        // Tampilkan Tanggal & Jam
        tvDateTime.text = "${order.date}  ${order.time}"

        tvMethod.text = order.paymentMethod
        tvTotal.text = formatRp.format(order.totalAmount)

        // Ambil Data Barang dari Database
        lifecycleScope.launch {
            val items = database.reportItemDao().getItemsByDateTime(order.date, order.time)

            if (items.isNotEmpty()) {
                val sb = StringBuilder()
                items.forEach { item ->
                    val priceStr = formatRp.format(item.totalPrice)
                    // Format: 2x Nasi Goreng (HON) ... Rp 30.000
                    sb.append("${item.quantity}x ${item.productName} (${item.vendor})\n")
                    sb.append("   $priceStr\n\n")
                }
                tvItems.text = sb.toString()
            } else {
                tvItems.text = "Detail barang tidak ditemukan."
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}