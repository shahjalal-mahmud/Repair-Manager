package com.appriyo.repairmanager.presentation.screens

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appriyo.repairmanager.data.model.SecurityType
import com.appriyo.repairmanager.navigation.Screen
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

    // React to success: show a toast with the serial number then navigate back to Dashboard.
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            val serial = uiState.generatedSerialNumber.orEmpty()
            Toast.makeText(
                context,
                "Repair saved successfully! Serial: $serial",
                Toast.LENGTH_LONG
            ).show()
            viewModel.consumeOneTimeEvents()
            navController.navigate(Screen.AddRepair.route) {
                popUpTo(Screen.AddRepair.route) { inclusive = true }
            }
        }
    }

    // React to errors: show a snackbar.
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Repair") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "New Repair Record",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Fill in the details below. A unique serial number will be generated automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(height = 16.dp)

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Name *") },
                    singleLine = true,
                    isError = uiState.fieldErrors.containsKey("customerName"),
                    supportingText = {
                        uiState.fieldErrors["customerName"]?.let { Text(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )

                Spacer(height = 8.dp)

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number * (11 digits)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = uiState.fieldErrors.containsKey("phoneNumber"),
                    supportingText = {
                        uiState.fieldErrors["phoneNumber"]?.let { Text(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )

                Spacer(height = 16.dp)
                HorizontalDivider()
                Spacer(height = 16.dp)

                OutlinedTextField(
                    value = deviceModel,
                    onValueChange = { deviceModel = it },
                    label = { Text("Device Model") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )

                Spacer(height = 8.dp)

                OutlinedTextField(
                    value = problemDescription,
                    onValueChange = { problemDescription = it },
                    label = { Text("Problem Description") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )

                Spacer(height = 8.dp)

                OutlinedTextField(
                    value = expectedDeliveryDate,
                    onValueChange = { },
                    label = { Text("Expected Delivery Date") },
                    singleLine = true,
                    readOnly = true,
                    trailingIcon = {
                        IconButton(
                            onClick = { if (!uiState.isLoading) datePickerDialog.show() }
                        ) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = "Pick date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(height = 8.dp)

                OutlinedTextField(
                    value = paymentInfo,
                    onValueChange = { paymentInfo = it },
                    label = { Text("Payment Information") },
                    placeholder = { Text("e.g. Advance ৳500, Due ৳1000") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )

                Spacer(height = 8.dp)

                OutlinedTextField(
                    value = boxNumber,
                    onValueChange = { boxNumber = it },
                    label = { Text("Box Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )

                Spacer(height = 8.dp)

                OutlinedTextField(
                    value = additionalDetails,
                    onValueChange = { additionalDetails = it },
                    label = { Text("Additional Details") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )

                Spacer(height = 16.dp)
                HorizontalDivider()
                Spacer(height = 16.dp)

                Text(
                    text = "Security Information",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(height = 8.dp)

                OptionDropdown(
                    label = "Security Type",
                    options = SecurityType.ALL,
                    selectedOption = securityType,
                    onOptionSelected = { securityType = it },
                    enabled = !uiState.isLoading
                )

                Spacer(height = 8.dp)

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )

                Spacer(height = 8.dp)

                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Pattern (e.g. 1-2-3-6-9)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )

                Spacer(height = 16.dp)
                HorizontalDivider()
                Spacer(height = 16.dp)

                Text(
                    text = "Accessories Received",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                LabeledCheckbox("Battery Included", batteryIncluded, { batteryIncluded = it }, !uiState.isLoading)
                LabeledCheckbox("SIM Included", simIncluded, { simIncluded = it }, !uiState.isLoading)
                LabeledCheckbox("Memory Card Included", memoryCardIncluded, { memoryCardIncluded = it }, !uiState.isLoading)
                LabeledCheckbox("SIM Tray Included", simTrayIncluded, { simTrayIncluded = it }, !uiState.isLoading)
                LabeledCheckbox("Back Cover Included", backCoverIncluded, { backCoverIncluded = it }, !uiState.isLoading)

                Spacer(height = 16.dp)
                HorizontalDivider()
                Spacer(height = 16.dp)

                LabeledCheckbox(
                    label = "Customer permits repair attempt even if phone cannot be powered on (dead phone)",
                    checked = deadPhonePermission,
                    onCheckedChange = { deadPhonePermission = it },
                    enabled = !uiState.isLoading
                )

                Spacer(height = 24.dp)

                Button(
                    onClick = {
                        viewModel.saveRepair(
                            customerName = customerName,
                            phoneNumber = phoneNumber,
                            deviceModel = deviceModel,
                            problemDescription = problemDescription,
                            expectedDeliveryDate = expectedDeliveryDate,
                            paymentInfo = paymentInfo,
                            additionalDetails = additionalDetails,
                            boxNumber = boxNumber,
                            securityType = securityType,
                            password = password,
                            pattern = pattern,
                            batteryIncluded = batteryIncluded,
                            simIncluded = simIncluded,
                            memoryCardIncluded = memoryCardIncluded,
                            simTrayIncluded = simTrayIncluded,
                            backCoverIncluded = backCoverIncluded,
                            deadPhonePermission = deadPhonePermission
                        )
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save Repair Record")
                    }
                }

                Spacer(height = 8.dp)

                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Cancel")
                }

                Spacer(height = 24.dp)
            }
        }
    }
}

@Composable
private fun Spacer(height: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(height))
}