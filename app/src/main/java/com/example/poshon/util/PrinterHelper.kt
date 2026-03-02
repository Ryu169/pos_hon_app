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

    // 1. UPDATE FUNGSI PRINT (Menambah parameter customerName)
    fun printReceipt(
        transactions: List<TransactionEntity>,
        total: Int,
        paymentMethod: String,
        cashReceived: Int,
        change: Int,
        customerName: String // ⬅️ Parameter Baru: Nama Pelanggan
    ) {
        if (selectedConnection == null) {
            Toast.makeText(context, "Tekan tahan tombol Print untuk pilih printer!", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val printer = EscPosPrinter(selectedConnection, 203, 48f, 32)
            val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            formatRp.maximumFractionDigits = 0

            val dateNow = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date())

            // Header Struk (Ditambah Baris Nama Pelanggan)
            var textToPrint = """
                [C]<img>${PrinterTextParserImg.bitmapToHexadecimalString(printer, context.resources.getDrawableForDensity(R.drawable.ic_launcher_foreground, 0))}</img>
                [C]<b>POS UMKM F&B</b>
                [C]Jl. Raya Bisnis No. 1
                [C]================================
                [L]Tgl  : $dateNow
                [L]Nama : $customerName
                [C]--------------------------------
            """.trimIndent()

            // Loop Barang
            transactions.forEach {
                val totalItem = it.quantity * it.price
                val priceString = formatRp.format(it.price).replace("Rp", "").trim()
                val totalString = formatRp.format(totalItem).replace("Rp", "").trim()

                textToPrint += "\n[L]<b>${it.productName}</b>"
                textToPrint += "\n[L]${it.quantity} x $priceString [R]$totalString"
            }

            val totalFinal = formatRp.format(total).replace("Rp", "Rp ").trim()

            // Footer Total
            textToPrint += """
                
                [C]--------------------------------
                [L]TOTAL :[R]<b>$totalFinal</b>
            """.trimIndent()

            // RINCIAN PEMBAYARAN
            if (paymentMethod == "CASH") {
                val cashString = formatRp.format(cashReceived).replace("Rp", "Rp ").trim()
                val changeString = formatRp.format(change).replace("Rp", "Rp ").trim()

                textToPrint += """
                    
                    [L]Tunai :[R]$cashString
                    [L]Kembali :[R]$changeString
                """.trimIndent()
            } else {
                // Jika QRIS
                textToPrint += """
                    
                    [L]Bayar :[R]$paymentMethod
                    [L]Status :[R]LUNAS
                """.trimIndent()
            }

            textToPrint += """
                
                [C]================================
                [C]Terima Kasih
                [C]Simpan struk ini sebagai
                [C]bukti pembayaran sah.
                
            """.trimIndent()

            printer.printFormattedText(textToPrint)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal print: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 2. UPDATE FUNGSI PREVIEW (Menambah parameter customerName)
    fun getReceiptPreview(
        transactions: List<TransactionEntity>,
        total: Int,
        paymentMethod: String,
        cashReceived: Int,
        change: Int,
        customerName: String // ⬅️ Parameter Baru
    ): String {
        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        formatRp.maximumFractionDigits = 0
        val dateNow = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date())

        var text = ""
        text += "================================\n"
        text += "        POS UMKM F&B\n"
        text += "    Jl. Raya Bisnis No. 1\n"
        text += "================================\n"
        text += "Tgl  : $dateNow\n"
        text += "Nama : $customerName\n" // ⬅️ Tampilkan Nama di Preview
        text += "--------------------------------\n"

        transactions.forEach {
            val totalItem = it.quantity * it.price
            text += "${it.productName}\n"
            text += "${it.quantity} x ${formatRp.format(it.price)} = ${formatRp.format(totalItem)}\n"
        }

        text += "--------------------------------\n"
        text += "TOTAL : ${formatRp.format(total)}\n"

        if (paymentMethod == "CASH") {
            text += "Tunai : ${formatRp.format(cashReceived)}\n"
            text += "Kembali : ${formatRp.format(change)}\n"
        } else {
            text += "Bayar : $paymentMethod\n"
            text += "Status : LUNAS\n"
        }

        text += "================================\n"
        text += "          Terima Kasih\n"

        return text
    }
}