package com.appriyo.repairmanager.presentation.screens

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appriyo.repairmanager.data.model.SecurityType
import com.appriyo.repairmanager.presentation.components.LabeledCheckbox
import com.appriyo.repairmanager.presentation.components.OptionDropdown
import com.appriyo.repairmanager.presentation.viewmodel.AddRepairViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.util.Calendar

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRepairScreen(
    navController: NavHostController,
    viewModel: AddRepairViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Form fields
    var customerName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var deviceModel by remember { mutableStateOf("") }
    var problemDescription by remember { mutableStateOf("") }
    var expectedDeliveryDate by remember { mutableStateOf("") }
    var paymentInfo by remember { mutableStateOf("") }
    var additionalDetails by remember { mutableStateOf("") }
    var boxNumber by remember { mutableStateOf("") }

    var securityType by remember { mutableStateOf(SecurityType.NONE) }
    var password by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("") }

    var batteryIncluded by remember { mutableStateOf(true) }
    var simIncluded by remember { mutableStateOf(true) }
    var memoryCardIncluded by remember { mutableStateOf(false) }
    var simTrayIncluded by remember { mutableStateOf(true) }
    var backCoverIncluded by remember { mutableStateOf(true) }
    var deadPhonePermission by remember { mutableStateOf(false) }

    val calendar = remember { Calendar.getInstance() }
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                expectedDeliveryDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
        }
    }

    // Bluetooth permission launcher (for printing)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* user can retry Save & Print regardless of result */ }

    LaunchedEffect(uiState.missingPermissions) {
        if (uiState.missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(uiState.missingPermissions.toTypedArray())
            viewModel.consumeMissingPermissions()
        }
    }

    fun clearFields() {
        customerName = ""
        phoneNumber = ""
        deviceModel = ""
        problemDescription = ""
        expectedDeliveryDate = ""
        paymentInfo = ""
        additionalDetails = ""
        boxNumber = ""
        securityType = SecurityType.NONE
        password = ""
        pattern = ""
        batteryIncluded = true
        simIncluded = true
        memoryCardIncluded = false
        simTrayIncluded = true
        backCoverIncluded = true
        deadPhonePermission = false
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            val serial = uiState.generatedSerialNumber.orEmpty()
            val message = if (uiState.printSuccess == true) {
                "Repair saved and printed successfully! Serial: $serial"
            } else {
                "Repair saved successfully! Serial: $serial"
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            clearFields()
            viewModel.consumeOneTimeEvents()
        }
    }

    LaunchedEffect(uiState.printErrorMessage) {
        uiState.printErrorMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Print Error: $message")
            }
            viewModel.consumePrintError()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
            }
            viewModel.consumeOneTimeEvents()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("New Repair", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Serial number is generated automatically",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.saveRepairOnly(
                                customerName, phoneNumber, deviceModel, problemDescription,
                                expectedDeliveryDate, paymentInfo, additionalDetails, boxNumber,
                                securityType, password, pattern, batteryIncluded, simIncluded,
                                memoryCardIncluded, simTrayIncluded, backCoverIncluded, deadPhonePermission
                            )
                        },
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        if (uiState.isLoading) {
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
                        onClick = {
                            viewModel.saveAndPrintRepair(
                                customerName, phoneNumber, deviceModel, problemDescription,
                                expectedDeliveryDate, paymentInfo, additionalDetails, boxNumber,
                                securityType, password, pattern, batteryIncluded, simIncluded,
                                memoryCardIncluded, simTrayIncluded, backCoverIncluded, deadPhonePermission
                            )
                        },
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        if (uiState.isLoading) {
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            SectionCard(title = "Customer", icon = Icons.Filled.Person) {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Name *") },
                    singleLine = true,
                    isError = uiState.fieldErrors.containsKey("customerName"),
                    supportingText = { uiState.fieldErrors["customerName"]?.let { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number * (11 digits)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = uiState.fieldErrors.containsKey("phoneNumber"),
                    supportingText = { uiState.fieldErrors["phoneNumber"]?.let { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )
            }

            SectionCard(title = "Device & Issue", icon = Icons.Filled.Smartphone) {
                OutlinedTextField(
                    value = deviceModel,
                    onValueChange = { deviceModel = it },
                    label = { Text("Device Model") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = problemDescription,
                    onValueChange = { problemDescription = it },
                    label = { Text("Problem Description") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = expectedDeliveryDate,
                    onValueChange = { },
                    label = { Text("Expected Delivery Date") },
                    singleLine = true,
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { if (!uiState.isLoading) datePickerDialog.show() }) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = "Pick date")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = boxNumber,
                    onValueChange = { boxNumber = it },
                    label = { Text("Box Number") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = additionalDetails,
                    onValueChange = { additionalDetails = it },
                    label = { Text("Additional Details") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )
            }

            SectionCard(title = "Payment", icon = Icons.Filled.Payments) {
                OutlinedTextField(
                    value = paymentInfo,
                    onValueChange = { paymentInfo = it },
                    label = { Text("Payment Information") },
                    placeholder = { Text("e.g. Advance ৳500, Due ৳1000") },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )
            }

            SectionCard(title = "Security", icon = Icons.Filled.Lock) {
                OptionDropdown(
                    label = "Security Type",
                    options = SecurityType.ALL,
                    selectedOption = securityType,
                    onOptionSelected = { securityType = it },
                    enabled = !uiState.isLoading
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Pattern (e.g. 1-2-3-6-9)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )
            }

            SectionCard(title = "Accessories Received", icon = Icons.Filled.Inventory2) {
                LabeledCheckbox("Battery Included", batteryIncluded, { batteryIncluded = it }, !uiState.isLoading)
                LabeledCheckbox("SIM Included", simIncluded, { simIncluded = it }, !uiState.isLoading)
                LabeledCheckbox("Memory Card Included", memoryCardIncluded, { memoryCardIncluded = it }, !uiState.isLoading)
                LabeledCheckbox("SIM Tray Included", simTrayIncluded, { simTrayIncluded = it }, !uiState.isLoading)
                LabeledCheckbox("Back Cover Included", backCoverIncluded, { backCoverIncluded = it }, !uiState.isLoading)
            }

            // Dead phone permission - styled as a callout, not buried in a generic card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Dead phone authorization",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LabeledCheckbox(
                            label = "Customer permits repair attempt even if phone cannot be powered on",
                            checked = deadPhonePermission,
                            onCheckedChange = { deadPhonePermission = it },
                            enabled = !uiState.isLoading
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}