// app/src/main/java/com/appriyo/repairmanager/data/repository/PrinterRepository.kt
package com.appriyo.repairmanager.data.repository

import com.appriyo.repairmanager.data.model.PrinterDevice
import com.appriyo.repairmanager.data.model.PrinterFirestorePaths
import com.appriyo.repairmanager.data.model.PrinterSettings
import com.appriyo.repairmanager.data.model.StoreInfo
import com.appriyo.repairmanager.printing.POSPrinterHelper
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for:
 *  - Reading / writing the selected printer from Firestore (appSettings/global)
 *  - Reading store info from Firestore (storeInfo/main)
 *  - Discovering paired Bluetooth devices via [POSPrinterHelper]
 *  - Connecting and printing via [POSPrinterHelper]
 *
 * All Firestore writes use merge so co-existing fields (e.g. SMS settings) are untouched.
 */
class PrinterRepository(
    private val firestore: FirebaseFirestore,
    private val printerHelper: POSPrinterHelper
) {

    private val settingsDocRef
        get() = firestore
            .collection(PrinterFirestorePaths.APP_SETTINGS_COLLECTION)
            .document(PrinterFirestorePaths.GLOBAL_DOC)

    private val storeInfoDocRef
        get() = firestore
            .collection(PrinterFirestorePaths.STORE_INFO_COLLECTION)
            .document(PrinterFirestorePaths.STORE_INFO_DOC)

    // ── Printer settings ────────────────────────────────────────────────────

    suspend fun loadPrinterSettings(): Result<PrinterSettings> {
        return try {
            val snapshot = settingsDocRef.get().await()
            val name = snapshot.getString(PrinterFirestorePaths.FIELD_PRINTER_NAME) ?: ""
            val address = snapshot.getString(PrinterFirestorePaths.FIELD_PRINTER_ADDRESS) ?: ""
            Result.success(PrinterSettings(name, address))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePrinterSettings(device: PrinterDevice): Result<Unit> {
        return try {
            val updates = mapOf(
                PrinterFirestorePaths.FIELD_PRINTER_NAME to device.name,
                PrinterFirestorePaths.FIELD_PRINTER_ADDRESS to device.address,
                PrinterFirestorePaths.FIELD_UPDATED_AT to FieldValue.serverTimestamp()
            )
            settingsDocRef.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Store information ───────────────────────────────────────────────────

    suspend fun loadStoreInfo(): Result<StoreInfo> {
        return try {
            val snapshot = storeInfoDocRef.get().await()
            val storeInfo = snapshot.toObject(StoreInfo::class.java) ?: StoreInfo()
            Result.success(storeInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Bluetooth device discovery ──────────────────────────────────────────

    /**
     * Returns all currently paired (bonded) Bluetooth devices.
     * Requires BLUETOOTH_CONNECT on Android 12+.
     */
    fun getPairedDevices(): List<PrinterDevice> {
        return printerHelper.getBondedDevices().map { device ->
            PrinterDevice(
                name = try { device.name } catch (_: SecurityException) { "Unknown" },
                address = device.address
            )
        }
    }

    // ── Print ───────────────────────────────────────────────────────────────

    /**
     * Connects to the printer at [address] and prints [invoiceText].
     * Disconnects afterwards regardless of success or failure.
     */
    suspend fun connectAndPrint(address: String, invoiceText: String): Result<Unit> {
        return try {
            val connected = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                printerHelper.connectToPrinter(address)
            }
            if (!connected) {
                return Result.failure(Exception("Could not connect to printer. Make sure it is paired, powered on, and in range."))
            }
            val printed = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                printerHelper.printText(invoiceText)
            }
            if (!printed) {
                return Result.failure(Exception("Printer connected but failed to send data. Please try again."))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Print error: ${e.localizedMessage ?: "Unknown error"}", e))
        } finally {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                printerHelper.disconnect()
            }
        }
    }
}