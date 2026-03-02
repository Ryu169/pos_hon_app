package com.example.poshon.ui.pos

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.poshon.R
import com.example.poshon.data.database.PosDatabase
import com.example.poshon.data.entity.TransactionEntity
import com.example.poshon.data.entity.OrderEntity
import com.example.poshon.data.entity.ReportItemEntity
import com.example.poshon.ui.product.ProductActivity
import com.example.poshon.util.PrinterHelper
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

class PosActivity : AppCompatActivity() {

    private lateinit var gridAdapter: ProductGridAdapter
    private lateinit var transactionAdapter: TransactionAdapter

    // DAO
    private lateinit var orderDao: com.example.poshon.data.dao.OrderDao
    private lateinit var transactionDao: com.example.poshon.data.dao.TransactionDao
    private lateinit var reportItemDao: com.example.poshon.data.dao.ReportItemDao

    // Helper
    private lateinit var printerHelper: PrinterHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pos)

        // 1. Cek Permission Bluetooth
        if (!checkBluetoothPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN), 1001
            )
        }

        // 2. Inisialisasi Database
        val database = PosDatabase.getInstance(this)
        transactionDao = database.transactionDao()
        val productDao = database.productDao()
        orderDao = database.orderDao()
        reportItemDao = database.reportItemDao()

        printerHelper = PrinterHelper(this)

        // 3. Binding Views
        val rvGrid = findViewById<RecyclerView>(R.id.rvProductGrid)
        val rvTransaction = findViewById<RecyclerView>(R.id.rvTransaction)
        val tvTotal = findViewById<TextView>(R.id.tvTotalIncome)
        val btnPrint = findViewById<Button>(R.id.btnPrint)
        val btnProduct = findViewById<View>(R.id.btnProduct)

        // --- FUNGSI UPDATE UI ---
        fun refreshTransaction() {
            lifecycleScope.launch {
                val transactions = transactionDao.getAllTransactions()
                val totalIncome = transactionDao.getTotalIncome() ?: 0

                val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                formatRp.maximumFractionDigits = 0

                transactionAdapter.updateData(transactions)
                tvTotal.text = "Total: ${formatRp.format(totalIncome)}"
            }
        }

        // --- SETUP ADAPTER TRANSAKSI ---
        transactionAdapter = TransactionAdapter(
            transactions = emptyList(),
            onPlusClick = { item ->
                lifecycleScope.launch {
                    val newQty = item.quantity + 1
                    val newTotal = newQty * item.price
                    transactionDao.updateTransaction(item.copy(quantity = newQty, total = newTotal))
                    refreshTransaction()
                }
            },
            onMinusClick = { item ->
                lifecycleScope.launch {
                    if (item.quantity > 1) {
                        val newQty = item.quantity - 1
                        val newTotal = newQty * item.price
                        transactionDao.updateTransaction(item.copy(quantity = newQty, total = newTotal))
                        refreshTransaction()
                    } else {
                        Toast.makeText(this@PosActivity, "Tekan silang merah untuk hapus", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDeleteClick = { itemToDelete ->
                lifecycleScope.launch {
                    transactionDao.deleteTransaction(itemToDelete)
                    refreshTransaction()
                    Toast.makeText(this@PosActivity, "Dihapus: ${itemToDelete.productName}", Toast.LENGTH_SHORT).show()
                }
            }
        )

        rvTransaction.layoutManager = LinearLayoutManager(this)
        rvTransaction.adapter = transactionAdapter

        // --- SETUP GRID MENU ---
        gridAdapter = ProductGridAdapter(emptyList()) { product ->
            lifecycleScope.launch {
                val existingItem = transactionDao.getTransactionByProductId(product.id)
                if (existingItem != null) {
                    val newQty = existingItem.quantity + 1
                    val newTotal = newQty * existingItem.price
                    transactionDao.updateTransaction(existingItem.copy(quantity = newQty, total = newTotal))
                } else {
                    transactionDao.insertTransaction(
                        TransactionEntity(
                            productId = product.id,
                            productName = product.name,
                            quantity = 1,
                            price = product.price,
                            total = product.price,
                            vendor = product.vendor
                        )
                    )
                    Toast.makeText(this@PosActivity, "+1 ${product.name}", Toast.LENGTH_SHORT).show()
                }
                refreshTransaction()
            }
        }
        rvGrid.layoutManager = GridLayoutManager(this, 2)
        rvGrid.adapter = gridAdapter

        lifecycleScope.launch {
            gridAdapter.updateData(productDao.getAllProducts())
        }

        // --- TOMBOL AKSI ---
        btnProduct.setOnClickListener {
            startActivity(Intent(this, ProductActivity::class.java))
        }

        // --- LOGIKA UTAMA PEMBAYARAN ---
        btnPrint.setOnClickListener {
            lifecycleScope.launch {
                val totalIncome = transactionDao.getTotalIncome() ?: 0
                if (totalIncome > 0) {
                    showPaymentDialog(totalIncome)
                } else {
                    Toast.makeText(this@PosActivity, "Keranjang kosong!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnPrint.setOnLongClickListener {
            if (checkBluetoothPermission()) {
                printerHelper.browseBluetoothDevice { deviceName ->
                    btnPrint.text = "Print ($deviceName)"
                }
            }
            true
        }

        refreshTransaction()
    }

    // --- FUNGSI MENAMPILKAN DIALOG BAYAR ---
    private fun showPaymentDialog(totalAmount: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payment, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val tvTotal = dialogView.findViewById<TextView>(R.id.tvDialogTotal)
        val etCash = dialogView.findViewById<EditText>(R.id.etCashReceived)
        val etName = dialogView.findViewById<EditText>(R.id.etCustomerName) // ⬅️ Input Nama Pelanggan
        val tvChange = dialogView.findViewById<TextView>(R.id.tvChange)
        val btnPas = dialogView.findViewById<Button>(R.id.btnExactMoney)
        val btnQris = dialogView.findViewById<Button>(R.id.btnQris)
        val btnProcess = dialogView.findViewById<Button>(R.id.btnProcessPayment)

        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        formatRp.maximumFractionDigits = 0
        tvTotal.text = formatRp.format(totalAmount)

        // 1. Logic Hitung Real-time
        etCash.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val cash = s.toString().toIntOrNull() ?: 0
                val change = cash - totalAmount

                if (change >= 0) {
                    tvChange.text = "Kembalian: ${formatRp.format(change)}"
                    tvChange.setTextColor(Color.parseColor("#4CAF50"))
                } else {
                    tvChange.text = "Kurang: ${formatRp.format(Math.abs(change))}"
                    tvChange.setTextColor(Color.parseColor("#F44336"))
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 2. Tombol Uang Pas
        btnPas.setOnClickListener {
            etCash.setText(totalAmount.toString())
            etCash.clearFocus()
        }

        // 3. Tombol QRIS
        btnQris.setOnClickListener {
            dialog.dismiss()
            // Ambil nama, default "Umum" jika kosong
            val custName = if (etName.text.toString().isEmpty()) "Umum" else etName.text.toString()
            processPaymentSuccess(totalAmount, "QRIS", 0, custName)
        }

        // 4. Tombol Proses Bayar (Cash)
        btnProcess.setOnClickListener {
            val cashString = etCash.text.toString()
            if (cashString.isEmpty()) {
                etCash.error = "Masukkan jumlah uang"
                return@setOnClickListener
            }
            val cashReceived = cashString.toIntOrNull() ?: 0

            if (cashReceived < totalAmount) {
                etCash.error = "Uang kurang!"
                return@setOnClickListener
            }

            // Hitung Kembalian
            val change = cashReceived - totalAmount

            dialog.dismiss()
            // Ambil nama, default "Umum" jika kosong
            val custName = if (etName.text.toString().isEmpty()) "Umum" else etName.text.toString()
            processPaymentSuccess(totalAmount, "CASH", change, custName)
        }

        dialog.show()
    }

    // --- FUNGSI SETELAH BAYAR ---
    private fun processPaymentSuccess(totalAmount: Int, method: String, change: Int, customerName: String) { // ⬅️ Parameter Baru
        lifecycleScope.launch {
            val transactions = transactionDao.getAllTransactions()
            val cashReceived = if (method == "CASH") totalAmount + change else totalAmount

            // Generate Preview dengan Nama Pelanggan
            val receiptText = printerHelper.getReceiptPreview(
                transactions,
                totalAmount,
                method,
                cashReceived,
                change,
                customerName // ⬅️ Pass ke Printer Helper
            )

            // Preview Struk
            AlertDialog.Builder(this@PosActivity)
                .setTitle("Struk Belanja ($method)")
                .setMessage(receiptText)
                .setCancelable(false)
                .setNeutralButton("PRINT") { _, _ ->
                    if (checkBluetoothPermission()) {
                        // Print Struk dengan Nama Pelanggan
                        printerHelper.printReceipt(
                            transactions,
                            totalAmount,
                            method,
                            cashReceived,
                            change,
                            customerName // ⬅️ Pass ke Printer Helper
                        )
                    }
                }
                .setPositiveButton("TUTUP (SELESAI)") { _, _ ->
                    saveOrderAndClear(totalAmount, method, customerName) // ⬅️ Simpan ke DB
                }
                .show()
        }
    }

    // --- FUNGSI SIMPAN KE DATABASE ---
    private fun saveOrderAndClear(totalAmount: Int, paymentMethod: String, customerName: String) { // ⬅️ Parameter Baru
        lifecycleScope.launch {
            val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            val now = Date()
            val dateString = dateFormat.format(now)
            val timeString = timeFormat.format(now)

            val transactions = transactionDao.getAllTransactions()

            // Simpan Item Laporan
            transactions.forEach { item ->
                reportItemDao.insertReportItem(
                    ReportItemEntity(
                        date = dateString, time = timeString,
                        productName = item.productName, vendor = item.vendor,
                        quantity = item.quantity, totalPrice = item.total
                    )
                )
            }

            // Simpan Header Order (Dengan Nama Pelanggan)
            orderDao.insertOrder(
                OrderEntity(
                    date = dateString,
                    time = timeString,
                    totalAmount = totalAmount,
                    paymentMethod = paymentMethod,
                    customerName = customerName // ⬅️ Simpan Nama
                )
            )

            transactionDao.deleteAll()

            val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            formatRp.maximumFractionDigits = 0
            val tvTotal = findViewById<TextView>(R.id.tvTotalIncome)
            tvTotal.text = "Total: ${formatRp.format(0)}"
            transactionAdapter.updateData(emptyList())

            Toast.makeText(this@PosActivity, "Transaksi Selesai!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true
    }
}