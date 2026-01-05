package com.example.poshon.ui.pos

import android.Manifest
import android.app.AlertDialog // Tambahan import untuk Popup
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.poshon.R
import com.example.poshon.data.database.PosDatabase
import com.example.poshon.data.entity.ProductEntity
import com.example.poshon.data.entity.TransactionEntity
import com.example.poshon.ui.product.ProductActivity
import com.example.poshon.util.PrinterHelper
import kotlinx.coroutines.launch

class PosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pos)

        // 🔐 Permission Bluetooth
        if (!checkBluetoothPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                1001
            )
        }

        // UI Initialization
        val spinnerProduct = findViewById<Spinner>(R.id.spinnerProduct)
        val etQuantity = findViewById<EditText>(R.id.etQuantity)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnPrint = findViewById<Button>(R.id.btnPrint)
        val btnReset = findViewById<Button>(R.id.btnReset)
        val btnProduct = findViewById<Button>(R.id.btnProduct)
        val tvTotalIncome = findViewById<TextView>(R.id.tvTotalIncome)
        val tvSummary = findViewById<TextView>(R.id.tvSummary)

        val rvTransaction = findViewById<RecyclerView>(R.id.rvTransaction)
        val adapter = TransactionAdapter(emptyList())
        rvTransaction.layoutManager = LinearLayoutManager(this)
        rvTransaction.adapter = adapter

        // Database & Helper
        val database = PosDatabase.getInstance(this)
        val transactionDao = database.transactionDao()
        val productDao = database.productDao()

        // Pass 'this' (Context) ke PrinterHelper
        val printerHelper = PrinterHelper(this)

        var productList: List<ProductEntity> = emptyList()

        // 🔄 Load Produk ke Spinner
        lifecycleScope.launch {
            productList = productDao.getAllProducts()

            if (productList.isNotEmpty()) {
                val productNames = productList.map {
                    "${it.name} - Rp ${it.price}"
                }

                val spinnerAdapter = ArrayAdapter(
                    this@PosActivity,
                    android.R.layout.simple_spinner_item,
                    productNames
                )
                spinnerAdapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )
                spinnerProduct.adapter = spinnerAdapter
            } else {
                // Handle jika produk kosong
                val emptyAdapter = ArrayAdapter(
                    this@PosActivity,
                    android.R.layout.simple_spinner_item,
                    listOf("Belum ada produk")
                )
                spinnerProduct.adapter = emptyAdapter
            }
        }

        // 🔄 Refresh transaksi
        fun refreshData() {
            lifecycleScope.launch {
                val transactions = transactionDao.getAllTransactions()
                val totalIncome = transactionDao.getTotalIncome() ?: 0
                val count = transactionDao.getTransactionCount()

                adapter.updateData(transactions)
                tvSummary.text = "Jumlah Transaksi: $count"
                tvTotalIncome.text = "Total Omzet: Rp $totalIncome"
            }
        }

        // ➕ Simpan Transaksi
        btnSave.setOnClickListener {
            if (productList.isEmpty()) {
                Toast.makeText(this, "Produk belum ada. Tambah dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val qtyText = etQuantity.text.toString()
            if (qtyText.isBlank()) {
                Toast.makeText(this, "Qty wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val selectedProduct = productList[spinnerProduct.selectedItemPosition]
                val quantity = qtyText.toInt()
                val total = quantity * selectedProduct.price

                lifecycleScope.launch {
                    transactionDao.insertTransaction(
                        TransactionEntity(
                            productId = selectedProduct.id,
                            productName = selectedProduct.name,
                            quantity = quantity,
                            price = selectedProduct.price,
                            total = total
                        )
                    )
                    refreshData()
                }
                etQuantity.text.clear()
            } catch (e: Exception) {
                Toast.makeText(this, "Error memilih produk", Toast.LENGTH_SHORT).show()
            }
        }

        // 🗑 Reset
        btnReset.setOnClickListener {
            lifecycleScope.launch {
                transactionDao.deleteAll()
                refreshData()
            }
        }

        // ==========================================================
        // FITUR PRINTER (Preview & Connect)
        // ==========================================================

        // A. Long Click: Untuk Scan & Pilih Printer (Connect Bluetooth)
        btnPrint.setOnLongClickListener {
            if (checkBluetoothPermission()) {
                printerHelper.browseBluetoothDevice { deviceName ->
                    btnPrint.text = "Print ($deviceName)"
                }
            } else {
                Toast.makeText(this, "Izin Bluetooth Diperlukan", Toast.LENGTH_SHORT).show()
            }
            true
        }

        // B. Click Biasa: TAMPILKAN PREVIEW / SIMULASI STRUK
        btnPrint.setOnClickListener {
            lifecycleScope.launch {
                val transactions = transactionDao.getAllTransactions()
                val totalIncome = transactionDao.getTotalIncome() ?: 0

                if (transactions.isNotEmpty()) {
                    // 1. Ambil Text Preview dari Helper
                    val receiptText = printerHelper.getReceiptPreview(transactions, totalIncome)

                    // 2. Tampilkan Popup Dialog
                    val builder = AlertDialog.Builder(this@PosActivity)
                    builder.setTitle("Preview Struk (Test Mode)")
                    builder.setMessage(receiptText) // Menampilkan teks struk

                    // Tombol Tutup
                    builder.setPositiveButton("Tutup") { dialog, _ ->
                        dialog.dismiss()
                    }

                    // (Opsional) Tombol Print Asli jika nanti sudah ada printer
                    builder.setNeutralButton("🖨️ Print Bluetooth") { _, _ ->
                        if (checkBluetoothPermission()) {
                            printerHelper.printReceipt(transactions, totalIncome)
                        }
                    }

                    builder.show()

                } else {
                    Toast.makeText(this@PosActivity, "Belum ada transaksi", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // ==========================================================

        // 📦 Pindah ke Halaman Produk
        btnProduct.setOnClickListener {
            startActivity(Intent(this, ProductActivity::class.java))
        }

        refreshData()
    }

    private fun checkBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED
        } else true
    }
}