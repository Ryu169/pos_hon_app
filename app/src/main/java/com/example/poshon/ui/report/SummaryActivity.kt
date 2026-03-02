package com.example.poshon.ui.report

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.poshon.R
import com.example.poshon.data.database.PosDatabase
import com.example.poshon.data.entity.ReportItemEntity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SummaryActivity : AppCompatActivity() {

    private lateinit var db: PosDatabase
    private lateinit var tvTitle: TextView
    private lateinit var tvTotal: TextView
    private lateinit var containerTopProducts: LinearLayout

    // Variabel penyimpan filter saat ini
    private var currentVendorFilter: String = "Semua Vendor"
    private var currentTimeFilter: String = "Hari Ini" // Default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        db = PosDatabase.getInstance(this)

        val spinnerVendor = findViewById<Spinner>(R.id.spinnerVendorSummary)
        val spinnerTime = findViewById<Spinner>(R.id.spinnerTimeFilter)

        tvTitle = findViewById(R.id.tvSummaryTitle)
        tvTotal = findViewById(R.id.tvSummaryTotal)
        containerTopProducts = findViewById(R.id.containerTopProducts)
        val btnBack = findViewById<Button>(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        setupTimeSpinner(spinnerTime)
        setupVendorSpinner(spinnerVendor)
    }

    private fun setupTimeSpinner(spinner: Spinner) {
        val timeOptions = arrayOf("Hari Ini", "7 Hari Terakhir", "30 Hari Terakhir", "1 Tahun Terakhir", "Semua Waktu")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, timeOptions)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentTimeFilter = timeOptions[position]
                refreshDashboard() // Hitung ulang saat waktu berubah
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupVendorSpinner(spinner: Spinner) {
        lifecycleScope.launch {
            val dao = db.reportItemDao()
            val vendorListFromDb = dao.getAllVendors()

            val spinnerItems = mutableListOf("Semua Vendor")
            spinnerItems.addAll(vendorListFromDb)

            val adapter = ArrayAdapter(this@SummaryActivity, android.R.layout.simple_spinner_dropdown_item, spinnerItems)
            spinner.adapter = adapter

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    currentVendorFilter = spinnerItems[position]
                    refreshDashboard() // Hitung ulang saat vendor berubah
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    // --- FUNGSI UTAMA: MENGHITUNG DATA ---
    private fun refreshDashboard() {
        lifecycleScope.launch {
            val dao = db.reportItemDao()

            // 1. Ambil Data Mentah (Sesuai Vendor)
            val rawData = if (currentVendorFilter == "Semua Vendor") {
                dao.getAllReports()
            } else {
                dao.getReportsByVendor(currentVendorFilter)
            }

            // 2. Filter Berdasarkan Waktu
            val filteredData = filterDataByTime(rawData, currentTimeFilter)

            // 3. Hitung Total Omset
            val totalRevenue = filteredData.sumOf { it.totalPrice }
            val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            formatRp.maximumFractionDigits = 0

            tvTitle.text = "TOTAL ($currentVendorFilter - $currentTimeFilter)"
            tvTotal.text = formatRp.format(totalRevenue)

            // 4. Hitung Top 5 Produk
            calculateTopProducts(filteredData)
        }
    }

    private fun filterDataByTime(items: List<ReportItemEntity>, timeFilter: String): List<ReportItemEntity> {
        if (timeFilter == "Semua Waktu") return items

        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val cal = Calendar.getInstance()

        // Reset jam ke 00:00:00 agar perbandingan tanggal akurat
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val todayStart = cal.time

        // Tentukan batas tanggal awal (StartDate)
        when (timeFilter) {
            "Hari Ini" -> { /* Tidak diubah, tetap hari ini 00:00 */ }
            "7 Hari Terakhir" -> cal.add(Calendar.DAY_OF_YEAR, -7)
            "30 Hari Terakhir" -> cal.add(Calendar.DAY_OF_YEAR, -30)
            "1 Tahun Terakhir" -> cal.add(Calendar.YEAR, -1)
        }
        val startDate = cal.time

        // Lakukan penyaringan
        return items.filter {
            try {
                val itemDate = dateFormat.parse(it.date)
                // Logika: Item harus >= startDate.
                // Jika "Hari Ini", maka itemDate harus >= todayStart
                itemDate != null && (itemDate.after(startDate) || itemDate == startDate || (timeFilter == "Hari Ini" && itemDate.time >= todayStart.time))
            } catch (e: Exception) {
                false // Abaikan jika format tanggal salah
            }
        }
    }

    private fun calculateTopProducts(items: List<ReportItemEntity>) {
        containerTopProducts.removeAllViews() // Bersihkan list lama

        if (items.isEmpty()) {
            val tvEmpty = TextView(this)
            tvEmpty.text = "Tidak ada penjualan pada periode ini."
            tvEmpty.gravity = Gravity.CENTER
            tvEmpty.setPadding(0, 32, 0, 32)
            containerTopProducts.addView(tvEmpty)
            return
        }

        // Logic Top 5: Group by Nama -> Sum Qty -> Sort Desc -> Take 5
        val topProducts = items
            .groupBy { it.productName }
            .mapValues { entry -> entry.value.sumOf { it.quantity } } // Hitung total qty per produk
            .toList()
            .sortedByDescending { (_, totalQty) -> totalQty } // Urutkan dari terbesar
            .take(5) // Ambil 5 teratas

        // Tampilkan ke Layout
        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        formatRp.maximumFractionDigits = 0

        topProducts.forEachIndexed { index, (name, qty) ->
            // Buat Layout Baris Programmatically agar ringan
            val rowLayout = LinearLayout(this)
            rowLayout.orientation = LinearLayout.HORIZONTAL
            rowLayout.setPadding(0, 16, 0, 16)

            // Nomor (1, 2, 3...)
            val tvNo = TextView(this)
            tvNo.text = "${index + 1}."
            tvNo.textSize = 14f
            tvNo.setTypeface(null, Typeface.BOLD)
            tvNo.width = 60 // Fixed width

            // Nama Produk
            val tvName = TextView(this)
            tvName.text = name
            tvName.textSize = 14f
            tvName.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            // Jumlah Terjual
            val tvQty = TextView(this)
            tvQty.text = "$qty Terjual"
            tvQty.textSize = 14f
            tvQty.setTypeface(null, Typeface.BOLD)
            tvQty.setTextColor(Color.parseColor("#4CAF50")) // Warna Hijau

            rowLayout.addView(tvNo)
            rowLayout.addView(tvName)
            rowLayout.addView(tvQty)

            containerTopProducts.addView(rowLayout)

            // Garis pembatas tipis
            if (index < topProducts.size - 1) {
                val divider = View(this)
                divider.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                divider.setBackgroundColor(Color.parseColor("#EEEEEE"))
                containerTopProducts.addView(divider)
            }
        }
    }
}