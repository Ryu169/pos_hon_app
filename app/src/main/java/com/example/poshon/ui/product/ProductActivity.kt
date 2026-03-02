package com.example.poshon.ui.product

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.poshon.R
import com.example.poshon.data.database.PosDatabase
import com.example.poshon.data.entity.ProductEntity
import com.example.poshon.ui.report.ReportActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductActivity : AppCompatActivity() {

    private lateinit var db: PosDatabase
    private lateinit var adapter: ProductAdapter

    private var selectedImageUri: Uri? = null
    private lateinit var ivPreview: ImageView

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            ivPreview.load(uri)
            val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                contentResolver.takePersistableUriPermission(uri, flag)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "Tidak ada foto dipilih", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product)

        db = PosDatabase.getInstance(this)

        val etName = findViewById<EditText>(R.id.etProductName)
        val etPrice = findViewById<EditText>(R.id.etProductPrice)
        // Pastikan Anda sudah menambahkan EditText ini di activity_product.xml
        val etVendor = findViewById<EditText>(R.id.etProductVendor)

        val btnSave = findViewById<Button>(R.id.btnAddProduct)
        val recyclerView = findViewById<RecyclerView>(R.id.rvProductList)
        val btnOpenReport = findViewById<Button>(R.id.btnOpenReport)

        ivPreview = findViewById<ImageView>(R.id.ivProductPreview)
        val tvSelectPhoto = findViewById<TextView>(R.id.tvSelectPhoto)

        ivPreview.setOnClickListener { openPhotoPicker() }
        tvSelectPhoto.setOnClickListener { openPhotoPicker() }

        adapter = ProductAdapter(emptyList()) { product ->
            deleteProduct(product)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val priceStr = etPrice.text.toString().trim()
            val vendor = etVendor.text.toString().trim()

            if (name.isEmpty() || priceStr.isEmpty() || vendor.isEmpty()) {
                Toast.makeText(this, "Nama, Harga, dan Vendor wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price = priceStr.toInt()
            val imageUriString = selectedImageUri?.toString()

            // PEMANGGILAN SUDAH BENAR (5 Parameter)
            saveProduct(name, price, imageUriString, 0, vendor)

            etName.text.clear()
            etPrice.text.clear()
            etVendor.text.clear()
            ivPreview.setImageResource(android.R.drawable.ic_menu_gallery)
            selectedImageUri = null
            etName.clearFocus()
        }

        btnOpenReport.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java)
            startActivity(intent)
        }

        loadProducts()
    }

    private fun openPhotoPicker() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun loadProducts() {
        lifecycleScope.launch(Dispatchers.IO) {
            val products = db.productDao().getAllProducts()
            withContext(Dispatchers.Main) {
                adapter.updateData(products)
            }
        }
    }

    // PERBAIKAN UTAMA ADA DI SINI
    // Sekarang fungsi ini menerima 5 parameter: name, price, imageUri, stock, vendor
    private fun saveProduct(name: String, price: Int, imageUri: String?, stock: Int, vendor: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.productDao().insertProduct(
                ProductEntity(
                    name = name,
                    price = price,
                    stock = stock,      // Menyimpan stok
                    imageUri = imageUri,
                    vendor = vendor     // Menyimpan vendor (HON/BERSUA)
                )
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ProductActivity, "Produk Disimpan!", Toast.LENGTH_SHORT).show()
                loadProducts()
            }
        }
    }

    private fun deleteProduct(product: ProductEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.productDao().deleteProduct(product)
            withContext(Dispatchers.Main) {
                loadProducts()
            }
        }
    }
}