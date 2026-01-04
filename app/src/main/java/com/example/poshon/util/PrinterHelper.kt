package com.example.poshon.util

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.widget.Toast
import com.dantsu.escpposprinter.EscPosPrinter
import com.dantsu.escpposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escpposprinter.textparser.PrinterTextParserImg
import com.example.poshon.R
import com.example.poshon.data.entity.TransactionEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrinterHelper(private val context: Context) {

    // Menyimpan koneksi printer
    private var selectedConnection: BluetoothPrintersConnections? = null

    // 1. Fungsi Scan & Pilih Printer
    @SuppressLint("MissingPermission")
    fun browseBluetoothDevice(onDeviceSelected: (String) -> Unit) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(context, "Mohon nyalakan Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        // Ambil device yang sudah dipairing di Setting HP
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        val deviceList = ArrayList<BluetoothDevice>()
        val deviceNames = ArrayList<String>()

        if (pairedDevices.isNotEmpty()) {
            for (device in pairedDevices) {
                deviceList.add(device)
                // Filter nama agar mudah dicari, biasanya ada kata "Printer" atau "BlueTooth"
                // Tapi untuk Smartcom kadang namanya unik, jadi tampilkan semua saja
                deviceNames.add(device.name ?: "Unknown Device")
            }
        } else {
            Toast.makeText(context, "Pairing printer di Setting Bluetooth HP dulu ya!", Toast.LENGTH_LONG).show()
            return
        }

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Pilih Smartcom Printer")
        builder.setItems(deviceNames.toTypedArray()) { _, which ->
            val device = deviceList[which]

            // Simpan koneksi
            selectedConnection = BluetoothPrintersConnections.selectFirstPairedBluetoothPrinter(
                bluetoothAdapter,
                device.address
            )

            onDeviceSelected(device.name ?: "Printer")
            Toast.makeText(context, "Siap print ke: ${device.name}", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    // 2. Fungsi Cetak Khusus 58mm
    fun printReceipt(transactions: List<TransactionEntity>, total: Int) {
        if (selectedConnection == null) {
            Toast.makeText(context, "Tekan tahan tombol Print untuk pilih printer!", Toast.LENGTH_LONG).show()
            return
        }

        try {
            // SETTING PENTING UNTUK 58MM:
            // 203 = DPI Standar Thermal
            // 48f = Lebar cetak efektif (kertas 58mm biasanya margin kiri kanan, sisa 48mm area cetak)
            // 32  = Jumlah karakter per baris (Agar font tidak kekecilan/kebesaran)
            val printer = EscPosPrinter(selectedConnection, 203, 48f, 32)

            val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            // Hapus 'Rp' dan ',00' agar lebih hemat tempat di kertas kecil
            formatRp.maximumFractionDigits = 0

            val dateNow = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date())

            // Header Struk
            // Menggunakan [C] untuk Center, [L] Left, [R] Right
            // <b>Bold</b> untuk judul
            var textToPrint = """
                [C]<img>${PrinterTextParserImg.bitmapToHexadecimalString(printer, context.resources.getDrawableForDensity(R.drawable.ic_launcher_foreground, 0))}</img>
                [C]<b>POS UMKM F&B</b>
                [C]Jl. Raya Bisnis No. 1
                [C]================================
                [L]Tgl : $dateNow
                [C]--------------------------------
            """.trimIndent()

            // Loop Item
            transactions.forEach {
                val totalItem = it.quantity * it.price
                val priceString = formatRp.format(it.price).replace("Rp", "").trim()
                val totalString = formatRp.format(totalItem).replace("Rp", "").trim()

                // Baris 1: Nama Produk (Bold)
                textToPrint += "\n[L]<b>${it.productName}</b>"

                // Baris 2: Qty x Harga ...... Total (Rata Kanan)
                // Contoh: 2 x 15.000 ...... 30.000
                textToPrint += "\n[L]${it.quantity} x $priceString [R]$totalString"
            }

            val totalFinal = formatRp.format(total).replace("Rp", "Rp ").trim()

            // Footer
            textToPrint += """
                
                [C]--------------------------------
                [L]TOTAL :[R]<b>$totalFinal</b>
                [C]================================
                [C]Terima Kasih
                [C]Simpan struk ini sebagai
                [C]bukti pembayaran sah
                
            """.trimIndent()

            // Eksekusi Print
            printer.printFormattedText(textToPrint)

        } catch (e: Exception) {
            e.printStackTrace()
            // Error handling jika printer mati/out of range
            Toast.makeText(context, "Gagal print: Cek koneksi / Kertas", Toast.LENGTH_LONG).show()
        }
    }
}