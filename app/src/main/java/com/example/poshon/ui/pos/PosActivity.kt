package com.example.poshon.ui.pos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.poshon.R
import com.example.poshon.data.database.PosDatabase
import com.example.poshon.data.entity.TransactionEntity
import com.example.poshon.util.PrinterHelper
import kotlinx.coroutines.launch

class PosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pos)

        // 🔐 CEK & MINTA PERMISSION BLUETOOTH (ANDROID 12+)
        if (!checkBluetoothPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    ),
                    1001
                )
            }
        }

        val etProductName = findViewById<EditText>(R.id.etProductName)
        val etQuantity = findViewById<EditText>(R.id.etQuantity)
        val etPrice = findViewById<EditText>(R.id.etPrice)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnPrint = findViewById<Button>(R.id.btnPrint)
        val tvTotalIncome = findViewById<TextView>(R.id.tvTotalIncome)
        val tvTransactionList = findViewById<TextView>(R.id.tvTransactionList)

        val database = PosDatabase.getInstance(this)
        val transactionDao = database.transactionDao()
        val printerHelper = PrinterHelper()

        fun refreshData() {
            lifecycleScope.launch {
                val transactions = transactionDao.getAllTransactions()
                val totalIncome = transactionDao.getTotalIncome() ?: 0

                tvTotalIncome.text = "Total Omzet: Rp $totalIncome"

                if (transactions.isEmpty()) {
                    tvTransactionList.text = "Belum ada transaksi"
                } else {
                    val builder = StringBuilder()
                    transactions.forEach {
                        builder.append("${it.productName} x${it.quantity} = Rp ${it.total}\n")
                    }
                    tvTransactionList.text = builder.toString()
                }
            }
        }

        btnSave.setOnClickListener {
            val productName = etProductName.text.toString()
            val qtyText = etQuantity.text.toString()
            val priceText = etPrice.text.toString()

            if (productName.isBlank() || qtyText.isBlank() || priceText.isBlank()) {
                Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val quantity = qtyText.toInt()
            val price = priceText.toInt()
            val total = quantity * price

            lifecycleScope.launch {
                transactionDao.insertTransaction(
                    TransactionEntity(
                        productName = productName,
                        quantity = quantity,
                        price = price,
                        total = total
                    )
                )
                refreshData()
            }

            etProductName.text.clear()
            etQuantity.text.clear()
            etPrice.text.clear()
        }

        // 🧾 CETAK STRUK
        btnPrint.setOnClickListener {
            if (!checkBluetoothPermission()) {
                Toast.makeText(
                    this,
                    "Izin Bluetooth belum diberikan",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val transactions = transactionDao.getAllTransactions()
                val totalIncome = transactionDao.getTotalIncome() ?: 0

                if (transactions.isEmpty()) {
                    Toast.makeText(
                        this@PosActivity,
                        "Tidak ada transaksi",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val receiptText = buildReceipt(transactions, totalIncome)
                printerHelper.print(receiptText)
            }
        }

        refreshData()
    }

    // 🔐 FUNGSI CEK PERMISSION (CONNECT + SCAN)
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
        } else {
            true
        }
    }

    // 🧾 FORMAT STRUK
    private fun buildReceipt(
        transactions: List<TransactionEntity>,
        total: Int
    ): String {
        val builder = StringBuilder()

        builder.append("POS UMKM\n")
        builder.append("------------------------------\n")

        transactions.forEach {
            builder.append("${it.productName} x${it.quantity}\n")
            builder.append("Rp ${it.total}\n")
        }

        builder.append("------------------------------\n")
        builder.append("TOTAL : Rp $total\n\n")
        builder.append("Terima kasih\n\n\n")

        return builder.toString()
    }
}
