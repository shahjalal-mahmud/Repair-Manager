// app/src/main/java/com/appriyo/repairmanager/presentation/viewmodel/ProductSellViewModel.kt
package com.appriyo.repairmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.repairmanager.data.repository.AuthRepository
import com.appriyo.repairmanager.data.repository.ProductSellRepository
import com.appriyo.repairmanager.presentation.state.ProductSellUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Product Sell / Invoice screen.
 *
 * Mirrors [AddRepairViewModel] in spirit:
 *  - exposes [saveSellOnly] and [saveSellAndPrint]
 *  - delegates the actual POS printing to [PrintViewModel]
 *  - re-publishes print success / error / missing-permission events
 *
 * In addition, it owns the live product sell list and a free-text search
 * query so the screen can offer a single page that combines "create new
 * invoice" and "browse past invoices".
 *
 * **Validation note:** [productPrice], [paymentAmount], and
 * [warrantyMonths] are free text (see [com.appriyo.repairmanager.data.model.ProductSell]).
 * This screen only produces a printed invoice, so we deliberately do NOT
 * parse them as numbers or compare them to each other - we only require
 * that the fields marked with * are not left empty. A shopkeeper can type
 * digits, Bangla digits, or words like "Free" / "Negotiable" and it will
 * save and print exactly as typed.
 */
class ProductSellViewModel(
    private val productSellRepository: ProductSellRepository,
    private val authRepository: AuthRepository,
    private val printViewModel: PrintViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductSellUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Forward print events into our UI state so the screen can react
        // to success / error / missing-permissions with the same patterns
        // used by AddRepairViewModel.
        viewModelScope.launch {
            printViewModel.uiState.collect { printState ->
                _uiState.update { state ->
                    state.copy(
                        printSuccess = if (printState.successMessage != null) true
                        else if (printState.errorMessage != null) false
                        else state.printSuccess,
                        printErrorMessage = printState.errorMessage,
                        missingPermissions = printState.missingPermissions
                    )
                }
                if (printState.successMessage != null) printViewModel.consumeSuccess()
                if (printState.errorMessage != null) printViewModel.consumeError()
            }
        }

        // Live product-sell list for the on-screen list + search.
        viewModelScope.launch {
            productSellRepository.observeProductSells().collect { items ->
                _uiState.update { it.copy(productSells = items) }
            }
        }
    }

    // ── Create ────────────────────────────────────────────────────────────

    /** Saves a sell record to Firestore without printing. */
    fun saveSellOnly(
        productName: String,
        productPrice: String,
        paymentAmount: String,
        warrantyMonths: String,
        warrantyStartDate: String,
        productSerial: String,
        warrantyDetails: String,
        notes: String
    ) = saveSell(
        productName, productPrice, paymentAmount, warrantyMonths,
        warrantyStartDate, productSerial, warrantyDetails, notes,
        shouldPrint = false
    )

    /** Saves a sell record to Firestore and immediately prints the invoice. */
    fun saveSellAndPrint(
        productName: String,
        productPrice: String,
        paymentAmount: String,
        warrantyMonths: String,
        warrantyStartDate: String,
        productSerial: String,
        warrantyDetails: String,
        notes: String
    ) = saveSell(
        productName, productPrice, paymentAmount, warrantyMonths,
        warrantyStartDate, productSerial, warrantyDetails, notes,
        shouldPrint = true
    )

    private fun saveSell(
        productName: String,
        productPrice: String,
        paymentAmount: String,
        warrantyMonths: String,
        warrantyStartDate: String,
        productSerial: String,
        warrantyDetails: String,
        notes: String,
        shouldPrint: Boolean
    ) {
        val errors = validateFields(
            productName = productName,
            productPrice = productPrice,
            paymentAmount = paymentAmount
        )
        if (errors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    fieldErrors = errors,
                    errorMessage = "Please fill in the highlighted fields before saving.",
                    isSuccess = false
                )
            }
            return
        }

        val currentUserId = authRepository.getCurrentUser()?.uid.orEmpty()
        if (currentUserId.isEmpty()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSuccess = false,
                    errorMessage = "You must be signed in to save a sale."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    fieldErrors = emptyMap()
                )
            }

            val result = productSellRepository.createProductSell(
                productName = productName.trim(),
                productPrice = productPrice.trim(),
                paymentAmount = paymentAmount.trim(),
                warrantyMonths = warrantyMonths.trim(),
                warrantyStartDate = warrantyStartDate.trim(),
                productSerial = productSerial.trim(),
                warrantyDetails = warrantyDetails.trim(),
                notes = notes.trim(),
                createdBy = currentUserId
            )

            result.fold(
                onSuccess = { sell ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            errorMessage = null,
                            generatedSerialNumber = sell.serialNumber
                        )
                    }
                    if (shouldPrint) printViewModel.printProductSell(sell)
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = false,
                            errorMessage = exception.localizedMessage
                                ?: "Failed to save the sale. Please try again."
                        )
                    }
                }
            )
        }
    }

    // ── Search ─────────────────────────────────────────────────────────────

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    // ── One-time events ───────────────────────────────────────────────────

    fun consumeOneTimeEvents() {
        _uiState.update {
            it.copy(
                isSuccess = false,
                errorMessage = null,
                printSuccess = null,
                printErrorMessage = null,
                fieldErrors = emptyMap()
            )
        }
    }

    fun consumePrintError() {
        _uiState.update { it.copy(printErrorMessage = null) }
    }

    fun consumeMissingPermissions() {
        _uiState.update { it.copy(missingPermissions = emptyList()) }
    }

    // ── Validation ────────────────────────────────────────────────────────

    /**
     * Only checks that the required (*) fields were not left empty.
     * Deliberately does NOT parse productPrice / paymentAmount as numbers
     * and does NOT compare them to each other - they are free text used
     * purely to print an invoice, and can be entered in any language or
     * even as words (e.g. "Free", "Negotiable").
     */
    private fun validateFields(
        productName: String,
        productPrice: String,
        paymentAmount: String
    ): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (productName.isBlank()) {
            errors["productName"] = "Product name is required."
        }
        if (productPrice.isBlank()) {
            errors["productPrice"] = "Product price is required."
        }
        if (paymentAmount.isBlank()) {
            errors["paymentAmount"] = "Payment amount is required."
        }
        return errors
    }
}