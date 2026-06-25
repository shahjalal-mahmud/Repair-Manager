// app/src/main/java/com/appriyo/repairmanager/printing/POSPrinterHelper.kt
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
 * Supports PT-210, GOOJPRT, Rongta, XPrinter, and any generic ESC/POS Bluetooth printer.
 * Unchanged from the original implementation — only the package path has been updated.
 */
class POSPrinterHelper(private val context: Context) {

    companion object {
        private const val TAG = "POSPrinterHelper"
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private fun getBluetoothAdapter(): BluetoothAdapter? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val bluetoothManager =
                    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
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

    /**
     * Connects to a printer by name (partial match, case-insensitive).
     * Tries standard RFCOMM first, falls back to insecure RFCOMM.
     */
    fun connectToPrinter(printerAddress: String): Boolean {
        return try {
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                Log.e(TAG, "Device doesn't support Bluetooth")
                return false
            }

            bluetoothAdapter = getBluetoothAdapter()

            if (bluetoothAdapter == null) {
                Log.e(TAG, "Bluetooth not available")
                return false
            }

            if (!bluetoothAdapter!!.isEnabled) {
                Log.e(TAG, "Bluetooth is disabled")
                return false
            }

            val device = findPrinterDeviceByAddress(printerAddress) ?: run {
                Log.e(TAG, "Printer device not found: $printerAddress")
                return false
            }

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

    private fun findPrinterDeviceByAddress(address: String): BluetoothDevice? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "BLUETOOTH_CONNECT permission required")
            return null
        }
        return bluetoothAdapter?.bondedDevices?.firstOrNull { device ->
            device.address.equals(address, ignoreCase = true)
        }
    }

    /** Returns all bonded (paired) Bluetooth devices. Caller must hold BLUETOOTH_CONNECT. */
    fun getBondedDevices(): List<BluetoothDevice> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    private fun tryStandardConnection(device: BluetoothDevice): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "BLUETOOTH_CONNECT permission not granted")
                return false
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "ACCESS_FINE_LOCATION permission not granted")
                return false
            }
        }
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