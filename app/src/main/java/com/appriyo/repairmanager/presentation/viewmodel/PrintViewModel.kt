// app/src/main/java/com/appriyo/repairmanager/presentation/viewmodel/PrintViewModel.kt
package com.appriyo.repairmanager.presentation.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.model.PrinterDevice
import com.appriyo.repairmanager.data.model.Repair
import com.appriyo.repairmanager.data.repository.PrinterRepository
import com.appriyo.repairmanager.printing.InvoiceFormatter
import com.appriyo.repairmanager.presentation.state.PrintUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared ViewModel for all printer-related actions:
 *  - Loading / saving the selected printer (Firestore sync)
 *  - Discovering paired Bluetooth devices
 *  - Printing an invoice for a [Repair] (Save & Print + Reprint)
 *  - Checking / communicating Bluetooth runtime permissions
 */
class PrintViewModel(
    private val printerRepository: PrinterRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrintUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    // ── Settings ────────────────────────────────────────────────────────────

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSettings = true)
            val result = printerRepository.loadPrinterSettings()
            _uiState.value = _uiState.value.copy(
                isLoadingSettings = false,
                selectedPrinter = result.getOrDefault(_uiState.value.selectedPrinter)
            )
        }
    }

    fun selectAndSavePrinter(device: PrinterDevice) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingPrinter = true)
            val result = printerRepository.savePrinterSettings(device)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isSavingPrinter = false,
                        selectedPrinter = com.appriyo.repairmanager.data.model.PrinterSettings(
                            selectedPrinterName = device.name,
                            selectedPrinterAddress = device.address
                        ),
                        successMessage = "Printer \"${device.name}\" saved."
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSavingPrinter = false,
                        errorMessage = "Failed to save printer: ${e.localizedMessage}"
                    )
                }
            )
        }
    }

    // ── Device discovery ────────────────────────────────────────────────────

    /**
     * Checks Bluetooth permissions and, if granted, populates [pairedDevices].
     * If permissions are missing, sets [missingPermissions] for the UI to request.
     */
    fun refreshPairedDevices() {
        val missing = requiredPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(missingPermissions = missing)
            return
        }

        _uiState.value = _uiState.value.copy(
            missingPermissions = emptyList(),
            pairedDevices = printerRepository.getPairedDevices()
        )
    }

    fun onPermissionsGranted() {
        _uiState.value = _uiState.value.copy(missingPermissions = emptyList())
        refreshPairedDevices()
    }

    // ── Print ───────────────────────────────────────────────────────────────

    /**
     * Generates an invoice for [repair] and prints it using the currently saved printer.
     * Call this from both "Save & Print" (after createRepair) and "Reprint" flows.
     */
    fun printRepair(repair: Repair) {
        val address = _uiState.value.selectedPrinter.selectedPrinterAddress
        if (address.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "No printer selected. Go to Printer Settings to choose a printer."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPrinting = true, errorMessage = null)

            // Load store info fresh each time so invoice header is always up to date.
            val storeInfoResult = printerRepository.loadStoreInfo()
            val storeInfo = storeInfoResult.getOrElse {
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    errorMessage = "Could not load store information: ${it.localizedMessage}"
                )
                return@launch
            }

            val invoiceText = InvoiceFormatter.buildInvoiceText(repair, storeInfo)

            val printResult = printerRepository.connectAndPrint(address, invoiceText)
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

    // ── One-shot message consumption ────────────────────────────────────────

    fun consumeSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun consumeError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun requiredPermissions(): List<String> {
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