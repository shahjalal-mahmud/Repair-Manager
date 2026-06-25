// app/src/main/java/com/appriyo/repairmanager/data/sms/DeviceIdProvider.kt
package com.appriyo.repairmanager.data.sms

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import java.util.UUID

/**
 * Generates and persists a stable, unique ID for this physical device/install.
 * Used to know which device is the designated SMS sender.
 */
class DeviceIdProvider(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getDeviceId(): String {
        prefs.getString(KEY_DEVICE_ID, null)?.let { return it }
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }

    /** Human-readable label shown in the SMS settings screen, e.g. "Samsung SM-A047F". */
    fun getDeviceDisplayName(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val model = Build.MODEL.orEmpty().ifBlank { "Device" }
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model".trim()
        }
    }

    companion object {
        private const val PREFS_NAME = "repair_manager_device_prefs"
        private const val KEY_DEVICE_ID = "device_id"
    }
}