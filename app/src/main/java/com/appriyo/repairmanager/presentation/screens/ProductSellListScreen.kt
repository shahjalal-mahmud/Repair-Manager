// app/src/main/java/com/appriyo/repairmanager/presentation/screens/ProductSellListScreen.kt
package com.appriyo.repairmanager.presentation.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appriyo.repairmanager.data.model.ProductSell
import com.appriyo.repairmanager.presentation.viewmodel.PrintViewModel
import com.appriyo.repairmanager.presentation.viewmodel.ProductSellViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Single screen that hosts the "Product Sell" feature.
 *
 * **Layout**
 *  1. Top app bar with the screen title.
 *  2. Live search bar over the saved-sales list.
 *  3. A new-sale form with the three required inputs (product name, price,
 *     payment amount) plus the optional warranty fields and a "Save" / a
 *     "Save & Print" button - matching the Add Repair pattern.
 *  4. A scrollable list of all saved sales as cards, each with its own
 *     "Print with POS" button that re-prints the existing invoice.
 *
 * **Free text everywhere:** Product price, payment amount, and warranty
 * months are NOT restricted to digits. The keyboard is hinted to numeric
 * (via [KeyboardType.Decimal] / [KeyboardType.Number]) purely as a
 * convenience so most users get a number pad by default, but nothing
 * filters or blocks what actually gets typed - Bangla digits, English
 * digits, or plain words like "Free" all pass straight through, since
 * this feature only needs to produce a printed invoice.
 *
 * Add Repair is intentionally kept exactly as it was - this screen only
 * adds a new feature.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSellListScreen(
    navController: NavHostController,
    viewModel: ProductSellViewModel = koinViewModel(),
    printViewModel: PrintViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val printUiState by printViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // --- Form state ------------------------------------------------------
    var productName by rememberSaveable { mutableStateOf("") }
    var productPrice by rememberSaveable { mutableStateOf("") }
    var paymentAmount by rememberSaveable { mutableStateOf("") }
    var warrantyMonths by rememberSaveable { mutableStateOf("") }
    var warrantyStartDate by rememberSaveable { mutableStateOf(todayFormatted()) }
    var productSerial by rememberSaveable { mutableStateOf("") }
    var warrantyDetails by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                warrantyStartDate = String.format(
                    Locale.US, "%02d/%02d/%04d", dayOfMonth, month + 1, year
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365 * 5
        }
    }

    fun clearForm() {
        productName = ""
        productPrice = ""
        paymentAmount = ""
        warrantyMonths = ""
        warrantyStartDate = todayFormatted()
        productSerial = ""
        warrantyDetails = ""
        notes = ""
    }

    // --- Bluetooth permission launcher (for print) -----------------------
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* user can retry Save & Print regardless of result */ }

    LaunchedEffect(uiState.missingPermissions) {
        if (uiState.missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(uiState.missingPermissions.toTypedArray())
            viewModel.consumeMissingPermissions()
        }
    }

    // Surface a one-shot message after a save completes
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            val serial = uiState.generatedSerialNumber.orEmpty()
            val message = if (uiState.printSuccess == true) {
                "Sale saved and invoice printed! Invoice: $serial"
            } else {
                "Sale saved! Invoice: $serial"
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            clearForm()
            viewModel.consumeOneTimeEvents()
        }
    }

    LaunchedEffect(uiState.printErrorMessage) {
        uiState.printErrorMessage?.let { message ->
            coroutineScope.launch { snackbarHostState.showSnackbar("Print Error: $message") }
            viewModel.consumePrintError()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
            viewModel.consumeOneTimeEvents()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Product Sell", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Issue a sale invoice and print on POS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
                // Search bar lives directly under the title so it is always
                // visible regardless of scroll position.
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search by product, invoice, serial") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // -------- New-sale form section --------
            item {
                NewSaleCard(
                    productName = productName,
                    onProductNameChange = { productName = it },
                    productPrice = productPrice,
                    onProductPriceChange = { productPrice = it },
                    paymentAmount = paymentAmount,
                    onPaymentAmountChange = { paymentAmount = it },
                    warrantyMonths = warrantyMonths,
                    onWarrantyMonthsChange = { warrantyMonths = it },
                    warrantyStartDate = warrantyStartDate,
                    onWarrantyStartDateClick = { datePickerDialog.show() },
                    productSerial = productSerial,
                    onProductSerialChange = { productSerial = it },
                    warrantyDetails = warrantyDetails,
                    onWarrantyDetailsChange = { warrantyDetails = it },
                    notes = notes,
                    onNotesChange = { notes = it },
                    fieldErrors = uiState.fieldErrors,
                    isLoading = uiState.isLoading,
                    onSaveOnly = {
                        viewModel.saveSellOnly(
                            productName, productPrice, paymentAmount,
                            warrantyMonths, warrantyStartDate,
                            productSerial, warrantyDetails, notes
                        )
                    },
                    onSaveAndPrint = {
                        viewModel.saveSellAndPrint(
                            productName, productPrice, paymentAmount,
                            warrantyMonths, warrantyStartDate,
                            productSerial, warrantyDetails, notes
                        )
                    }
                )
            }

            // -------- Section header for the list --------
            item {
                Text(
                    text = "Recent Sales",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            val filtered = filterSales(uiState.productSells, uiState.searchQuery)

            if (filtered.isEmpty()) {
                item {
                    EmptySalesState(isSearching = uiState.searchQuery.isNotBlank())
                }
            } else {
                items(filtered, key = { it.id }) { sell ->
                    ProductSellCard(
                        sell = sell,
                        onPrint = { printViewModel.printProductSell(sell) },
                        isPrinting = printUiState.isPrinting
                    )
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

// =============================================================================
// New-sale form card
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewSaleCard(
    productName: String,
    onProductNameChange: (String) -> Unit,
    productPrice: String,
    onProductPriceChange: (String) -> Unit,
    paymentAmount: String,
    onPaymentAmountChange: (String) -> Unit,
    warrantyMonths: String,
    onWarrantyMonthsChange: (String) -> Unit,
    warrantyStartDate: String,
    onWarrantyStartDateClick: () -> Unit,
    productSerial: String,
    onProductSerialChange: (String) -> Unit,
    warrantyDetails: String,
    onWarrantyDetailsChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    fieldErrors: Map<String, String>,
    isLoading: Boolean,
    onSaveOnly: () -> Unit,
    onSaveAndPrint: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "New Sale",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Three primary fields explicitly requested: product name, price, payment
            OutlinedTextField(
                value = productName,
                onValueChange = onProductNameChange,
                label = { Text("Product Name *") },
                singleLine = true,
                isError = fieldErrors.containsKey("productName"),
                supportingText = { fieldErrors["productName"]?.let { Text(it) } },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Free text: keyboard hints numeric/decimal for convenience,
                // but any input (digits in any language, or words) is allowed.
                OutlinedTextField(
                    value = productPrice,
                    onValueChange = onProductPriceChange,
                    label = { Text("Product Price *") },
                    placeholder = { Text("BDT") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = fieldErrors.containsKey("productPrice"),
                    supportingText = { fieldErrors["productPrice"]?.let { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                )
                OutlinedTextField(
                    value = paymentAmount,
                    onValueChange = onPaymentAmountChange,
                    label = { Text("Payment Amount *") },
                    placeholder = { Text("BDT") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = fieldErrors.containsKey("paymentAmount"),
                    supportingText = { fieldErrors["paymentAmount"]?.let { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                )
            }

            // Warranty block - optional
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Warranty (optional)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Free text: keyboard hints numeric for convenience, but any
                // input (digits in any language, or words) is allowed.
                OutlinedTextField(
                    value = warrantyMonths,
                    onValueChange = onWarrantyMonthsChange,
                    label = { Text("Warranty (months)") },
                    placeholder = { Text("e.g. 6") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                )
                OutlinedTextField(
                    value = warrantyStartDate,
                    onValueChange = { },
                    label = { Text("Warranty Start") },
                    singleLine = true,
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = onWarrantyStartDateClick) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = "Pick date")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = productSerial,
                onValueChange = onProductSerialChange,
                label = { Text("Product S/N / IMEI") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = warrantyDetails,
                onValueChange = onWarrantyDetailsChange,
                label = { Text("Warranty Terms / Conditions") },
                placeholder = { Text("e.g. Service warranty, no physical damage") },
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Notes (optional)") },
                minLines = 1,
                maxLines = 2,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Spacer(Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // Bottom action bar mirrors Add Repair: Save only | Save & Print
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSaveOnly,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    } else {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Only")
                    }
                }
                Button(
                    onClick = onSaveAndPrint,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Print")
                    }
                }
            }
        }
    }
}

// =============================================================================
// Saved-sale card
// =============================================================================

@Composable
private fun ProductSellCard(
    sell: ProductSell,
    onPrint: () -> Unit,
    isPrinting: Boolean
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sell.serialNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatDateTime(sell.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = sell.productName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                // productPrice / paymentAmount are free text - shown exactly
                // as the shopkeeper typed them, no numeric formatting applied.
                Text(
                    text = "Price ${sell.productPrice}   •   Paid ${sell.paymentAmount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (sell.warrantyMonths.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Warranty: ${sell.warrantyMonths}" +
                                if (sell.warrantyStartDate.isNotBlank())
                                    "  (from ${sell.warrantyStartDate})" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (sell.productSerial.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "S/N: ${sell.productSerial}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalIconButton(onClick = onPrint, enabled = !isPrinting) {
                    if (isPrinting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Print, contentDescription = "Print invoice")
                    }
                }
            }
        }
    }
}

// =============================================================================
// Empty state
// =============================================================================

@Composable
private fun EmptySalesState(isSearching: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isSearching) "No sales match your search."
                else "No sales yet. Fill the form above to create one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =============================================================================
// Helpers
// =============================================================================

private val DATE_TIME_FORMATTER = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US)

private fun todayFormatted(): String {
    val cal = Calendar.getInstance()
    return String.format(
        Locale.US, "%02d/%02d/%04d",
        cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.YEAR)
    )
}

private fun formatDateTime(date: Date?): String {
    return date?.let { DATE_TIME_FORMATTER.format(it) } ?: "-"
}

private fun filterSales(sales: List<ProductSell>, query: String): List<ProductSell> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return sales
    val needle = trimmed.lowercase(Locale.US)
    return sales.filter { sell ->
        sell.productName.lowercase(Locale.US).contains(needle) ||
                sell.serialNumber.lowercase(Locale.US).contains(needle) ||
                sell.productSerial.lowercase(Locale.US).contains(needle) ||
                sell.notes.lowercase(Locale.US).contains(needle)
    }
}