package com.appriyo.repairmanager.presentation.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.model.Repair
import com.appriyo.repairmanager.printing.InvoiceFormatter
import com.appriyo.repairmanager.printing.POSPrinterHelper
import com.appriyo.repairmanager.presentation.state.PrintUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for print operations using hardcoded printer.
 */
class PrintViewModel(private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(PrintUiState())
    val uiState = _uiState.asStateFlow()

    private val printerHelper = POSPrinterHelper(context)

    // ── Print ───────────────────────────────────────────────────────────────

    /**
     * Generates an invoice for [repair] and prints it using the hardcoded printer.
     */
    fun printRepair(repair: Repair) {
        // Check Bluetooth permissions
        val missingPermissions = getRequiredPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                missingPermissions = missingPermissions,
                errorMessage = "Bluetooth permissions required"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPrinting = true, errorMessage = null)

            val invoiceText = InvoiceFormatter.buildInvoiceText(repair)

            val printResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    if (!printerHelper.connectToPrinter()) {
                        return@withContext Result.failure(Exception("Could not connect to printer. Make sure it's paired, powered on, and in range."))
                    }

                    if (!printerHelper.printText(invoiceText)) {
                        return@withContext Result.failure(Exception("Printer connected but failed to send data."))
                    }

                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                } finally {
                    printerHelper.disconnect()
                }
            }

            printResult.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        successMessage = "Invoice printed successfully."
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isPrinting = false,
                        errorMessage = e.localizedMessage ?: "Print failed."
                    )
                }
            )
        }
    }

    fun onPermissionsGranted() {
        _uiState.value = _uiState.value.copy(missingPermissions = emptyList())
    }

    fun consumeSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun consumeError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun getRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}