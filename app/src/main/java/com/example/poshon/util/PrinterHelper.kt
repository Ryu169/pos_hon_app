package com.example.poshon.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.OutputStream
import java.util.*

class PrinterHelper {

    private val printerName = "EP58" // EPPOS EP58M

    @Suppress("MissingPermission")
    fun print(text: String) {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                ?: throw Exception("Bluetooth tidak tersedia")

            val device: BluetoothDevice = bluetoothAdapter.bondedDevices
                .firstOrNull { it.name.contains(printerName, true) }
                ?: throw Exception("Printer tidak ditemukan")

            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            val socket: BluetoothSocket =
                device.createRfcommSocketToServiceRecord(uuid)

            bluetoothAdapter.cancelDiscovery()
            socket.connect()

            val outputStream: OutputStream = socket.outputStream
            outputStream.write(text.toByteArray())
            outputStream.flush()

            outputStream.close()
            socket.close()

        } catch (e: Exception) {
            Log.e("PRINTER", "Gagal cetak", e)
        }
    }
}
