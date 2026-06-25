// app/src/main/java/com/appriyo/repairmanager/data/model/PrinterDevice.kt
package com.appriyo.repairmanager.data.model

/**
 * Lightweight wrapper for a paired Bluetooth device shown in the printer-picker UI.
 */
data class PrinterDevice(
    val name: String,
    val address: String
)

/**
 * Firestore-compatible model for the persisted printer selection.
 *
 * Collection : appSettings
 * Document   : global
 *
 * Fields stored alongside any existing fields (e.g. SMS settings)
 * using merge writes so nothing is overwritten.
 */
data class PrinterSettings(
    val selectedPrinterName: String = "",
    val selectedPrinterAddress: String = ""
)

/**
 * Firestore path constants for printer / store settings.
 */
object PrinterFirestorePaths {
    const val APP_SETTINGS_COLLECTION = "appSettings"
    const val GLOBAL_DOC = "global"
    const val STORE_INFO_COLLECTION = "storeInfo"
    const val STORE_INFO_DOC = "main"

    // Field names inside appSettings/global
    const val FIELD_PRINTER_NAME = "selectedPrinterName"
    const val FIELD_PRINTER_ADDRESS = "selectedPrinterAddress"
    const val FIELD_UPDATED_AT = "updatedAt"
}