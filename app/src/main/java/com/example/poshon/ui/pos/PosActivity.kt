package com.example.poshon.ui.pos

import android.Manifest
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

        // UI
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

        // Database
        val database = PosDatabase.getInstance(this)
        val transactionDao = database.transactionDao()
        val productDao = database.productDao()
        val printerHelper = PrinterHelper()

        var productList: List<ProductEntity> = emptyList()

        // 🔄 Load Produk ke Spinner
        lifecycleScope.launch {
            productList = productDao.getAllProducts()

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
                Toast.makeText(this, "Produk belum ada", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val qtyText = etQuantity.text.toString()
            if (qtyText.isBlank()) {
                Toast.makeText(this, "Qty wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
        }

        // 🗑 Reset
        btnReset.setOnClickListener {
            lifecycleScope.launch {
                transactionDao.deleteAll()
                refreshData()
            }
        }

        // 🧾 Print
        btnPrint.setOnClickListener {
            if (!checkBluetoothPermission()) {
                Toast.makeText(this, "Izin Bluetooth belum diberikan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val transactions = transactionDao.getAllTransactions()
                val totalIncome = transactionDao.getTotalIncome() ?: 0
                if (transactions.isNotEmpty()) {
                    printerHelper.print(buildReceipt(transactions, totalIncome))
                }
            }
        }

        // 📦 Produk
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

    private fun buildReceipt(
        transactions: List<TransactionEntity>,
        total: Int
    ): String {
        val sb = StringBuilder()
        sb.append("POS UMKM\n")
        sb.append("------------------------------\n")
        transactions.forEach {
            sb.append("${it.productName} x${it.quantity}\n")
            sb.append("Rp ${it.total}\n")
        }
        sb.append("------------------------------\n")
        sb.append("TOTAL : Rp $total\n\nTerima kasih\n\n\n")
        return sb.toString()
    }
}
