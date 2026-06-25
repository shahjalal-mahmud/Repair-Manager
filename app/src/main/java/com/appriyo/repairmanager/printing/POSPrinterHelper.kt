package com.appriyo.repairmanager.printing

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.OutputStream
import java.lang.reflect.Method
import java.util.UUID

/**
 * Handles Bluetooth SPP connection and raw ESC/POS text printing.
 * Hardcoded for a specific POS printer.
 */
class POSPrinterHelper(private val context: Context) {

    companion object {
        private const val TAG = "POSPrinterHelper"
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // HARDCODED PRINTER DETAILS - Change these to match your printer
        const val PRINTER_NAME = "PT-210"  // Change to your printer name
        const val PRINTER_ADDRESS = "00:11:22:33:44:55"  // Change to your printer MAC address
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private fun getBluetoothAdapter(): BluetoothAdapter? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                bluetoothManager.adapter
            } else {
                @Suppress("DEPRECATION")
                BluetoothAdapter.getDefaultAdapter()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Bluetooth adapter", e)
            null
        }
    }

    fun connectToPrinter(): Boolean {
        return try {
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                Log.e(TAG, "Device doesn't support Bluetooth")
                return false
            }

            bluetoothAdapter = getBluetoothAdapter()
            if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
                Log.e(TAG, "Bluetooth not available or disabled")
                return false
            }

            // Check permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "BLUETOOTH_CONNECT permission not granted")
                    return false
                }
            }

            // Find the hardcoded printer
            val device = bluetoothAdapter?.bondedDevices?.firstOrNull {
                it.address.equals(PRINTER_ADDRESS, ignoreCase = true)
            } ?: run {
                Log.e(TAG, "Printer not found: $PRINTER_ADDRESS")
                return false
            }

            // Try standard connection first, fallback to alternative
            if (tryStandardConnection(device)) {
                true
            } else {
                tryAlternativeConnection(device)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Bluetooth permission denied", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            false
        }
    }

    private fun tryStandardConnection(device: BluetoothDevice): Boolean {
        return try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            bluetoothAdapter?.cancelDiscovery()
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            true
        } catch (e: IOException) {
            Log.e(TAG, "Standard connection failed", e)
            safeCloseSocket()
            false
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException - permission denied", e)
            false
        }
    }

    private fun tryAlternativeConnection(device: BluetoothDevice): Boolean {
        return try {
            val method: Method = device.javaClass.getMethod(
                "createInsecureRfcommSocketToServiceRecord",
                UUID::class.java
            )
            bluetoothSocket = method.invoke(device, SPP_UUID) as BluetoothSocket
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            true
        } catch (e: Exception) {
            Log.e(TAG, "Alternative connection failed", e)
            safeCloseSocket()
            false
        }
    }

    fun printText(text: String): Boolean {
        return try {
            outputStream?.write(text.toByteArray(Charsets.UTF_8))
            outputStream?.flush()
            true
        } catch (e: IOException) {
            Log.e(TAG, "Print failed", e)
            false
        } catch (e: NullPointerException) {
            Log.e(TAG, "Output stream not initialized", e)
            false
        }
    }

    fun isConnected(): Boolean = bluetoothSocket?.isConnected == true

    fun disconnect() {
        try {
            outputStream?.close()
        } catch (_: IOException) {}
        safeCloseSocket()
        outputStream = null
        bluetoothSocket = null
    }

    private fun safeCloseSocket() {
        try {
            bluetoothSocket?.close()
        } catch (_: IOException) {}
        bluetoothSocket = null
    }
}