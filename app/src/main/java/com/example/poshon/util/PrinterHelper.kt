package com.example.poshon.util

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.widget.Toast

// Import Library Printer
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.example.poshon.R
import com.example.poshon.data.entity.TransactionEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrinterHelper(private val context: Context) {

    private var selectedConnection: DeviceConnection? = null

    @SuppressLint("MissingPermission")
    fun browseBluetoothDevice(onDeviceSelected: (String) -> Unit) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(context, "Mohon nyalakan Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        val deviceList = ArrayList<BluetoothDevice>()
        val deviceNames = ArrayList<String>()

        if (pairedDevices.isNotEmpty()) {
            for (device in pairedDevices) {
                deviceList.add(device)
                deviceNames.add(device.name ?: "Unknown Device")
            }
        } else {
            Toast.makeText(context, "Pairing printer di Setting Bluetooth HP dulu ya!", Toast.LENGTH_LONG).show()
            return
        }

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Pilih Printer Thermal")
        builder.setItems(deviceNames.toTypedArray()) { _, which ->
            val device = deviceList[which]
            selectedConnection = BluetoothConnection(device)
            onDeviceSelected(device.name ?: "Printer")
            Toast.makeText(context, "Siap print ke: ${device.name}", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    fun printReceipt(transactions: List<TransactionEntity>, total: Int) {
        if (selectedConnection == null) {
            Toast.makeText(context, "Tekan tahan tombol Print untuk pilih printer!", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val printer = EscPosPrinter(selectedConnection, 203, 48f, 32)
            val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            formatRp.maximumFractionDigits = 0

            val dateNow = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date())

            var textToPrint = """
                [C]<img>${PrinterTextParserImg.bitmapToHexadecimalString(printer, context.resources.getDrawableForDensity(R.drawable.ic_launcher_foreground, 0))}</img>
                [C]<b>POS UMKM F&B</b>
                [C]Jl. Raya Bisnis No. 1
                [C]================================
                [L]Tgl : $dateNow
                [C]--------------------------------
            """.trimIndent()

            transactions.forEach {
                val totalItem = it.quantity * it.price
                val priceString = formatRp.format(it.price).replace("Rp", "").trim()
                val totalString = formatRp.format(totalItem).replace("Rp", "").trim()

                textToPrint += "\n[L]<b>${it.productName}</b>"
                textToPrint += "\n[L]${it.quantity} x $priceString [R]$totalString"
            }

            val totalFinal = formatRp.format(total).replace("Rp", "Rp ").trim()

            textToPrint += """
                
                [C]--------------------------------
                [L]TOTAL :[R]<b>$totalFinal</b>
                [C]================================
                [C]Terima Kasih
                
            """.trimIndent()

            printer.printFormattedText(textToPrint)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal print: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // --- FUNGSI BARU DITAMBAHKAN DI SINI ---
    // Fungsi ini menghasilkan String biasa untuk ditampilkan di layar HP (Preview)
    // Tanpa kode printer seperti [C], [L], atau <img> agar mudah dibaca manusia.
    fun getReceiptPreview(transactions: List<TransactionEntity>, total: Int): String {
        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        formatRp.maximumFractionDigits = 0
        val dateNow = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date())

        var text = ""
        text += "================================\n"
        text += "        POS UMKM F&B\n"
        text += "    Jl. Raya Bisnis No. 1\n"
        text += "================================\n"
        text += "Tgl : $dateNow\n"
        text += "--------------------------------\n"

        transactions.forEach {
            val totalItem = it.quantity * it.price
            text += "${it.productName}\n"
            text += "${it.quantity} x ${formatRp.format(it.price)} = ${formatRp.format(totalItem)}\n"
        }

        text += "--------------------------------\n"
        text += "TOTAL : ${formatRp.format(total)}\n"
        text += "================================\n"
        text += "          Terima Kasih\n"

        return text
    }
}