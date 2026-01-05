package com.example.poshon.ui.product

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.poshon.R
import com.example.poshon.data.database.PosDatabase
import com.example.poshon.data.entity.ProductEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductActivity : AppCompatActivity() {

    private lateinit var db: PosDatabase
    private lateinit var adapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product)

        // 1. Inisialisasi Database
        db = PosDatabase.getInstance(this)

        // 2. Binding View
        val etName = findViewById<EditText>(R.id.etProductName)
        val etPrice = findViewById<EditText>(R.id.etProductPrice)
        // Pastikan ID ini sesuai dengan layout XML Anda (btnAddProduct atau btnSaveProduct)
        val btnSave = findViewById<Button>(R.id.btnAddProduct)
        val recyclerView = findViewById<RecyclerView>(R.id.rvProductList)

        // 3. Setup RecyclerView
        adapter = ProductAdapter(emptyList()) { product ->
            deleteProduct(product)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 4. Tombol Simpan
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val priceStr = etPrice.text.toString().trim()

            // Validasi Input
            if (name.isEmpty()) {
                etName.error = "Nama produk wajib diisi"
                return@setOnClickListener
            }
            if (priceStr.isEmpty()) {
                etPrice.error = "Harga wajib diisi"
                return@setOnClickListener
            }

            val price = priceStr.toInt()

            // Simpan Data
            saveProduct(name, price)

            // Reset Form
            etName.text.clear()
            etPrice.text.clear()
            // Hilangkan fokus agar keyboard turun (opsional)
            etName.clearFocus()
            etPrice.clearFocus()
        }

        // 5. Load Data Awal
        loadProducts()
    }

    private fun loadProducts() {
        // Menggunakan lifecycleScope agar aman terhadap lifecycle activity
        lifecycleScope.launch(Dispatchers.IO) {
            val products = db.productDao().getAllProducts()

            // Pindah ke Main Thread untuk update UI
            withContext(Dispatchers.Main) {
                adapter.updateData(products)
            }
        }
    }

    private fun saveProduct(name: String, price: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.productDao().insertProduct(
                ProductEntity(
                    name = name,
                    price = price,
                    stock = 0 // Default stock 0 (sesuai kode Anda)
                )
            )

            // Refresh list setelah insert & Tampilkan Toast di Main Thread
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ProductActivity, "Produk Disimpan!", Toast.LENGTH_SHORT).show()
                loadProducts()
            }
        }
    }

    private fun deleteProduct(product: ProductEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.productDao().deleteProduct(product)

            // Refresh list & Tampilkan Toast
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ProductActivity, "${product.name} dihapus", Toast.LENGTH_SHORT).show()
                loadProducts()
            }
        }
    }
}