// app/src/main/java/com/appriyo/repairmanager/presentation/screens/EditRepairScreen.kt
package com.appriyo.repairmanager.presentation.screens

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appriyo.repairmanager.data.model.RepairStatus
import com.appriyo.repairmanager.data.model.SecurityType
import com.appriyo.repairmanager.presentation.components.LabeledCheckbox
import com.appriyo.repairmanager.presentation.components.OptionDropdown
import com.appriyo.repairmanager.presentation.components.SectionCard
import com.appriyo.repairmanager.presentation.components.StatusChip
import com.appriyo.repairmanager.presentation.viewmodel.EditRepairViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.util.Calendar

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRepairScreen(
    navController: NavHostController,
    repairId: String,
    viewModel: EditRepairViewModel = koinViewModel()
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
    var status by remember { mutableStateOf(RepairStatus.PENDING) }

    var securityType by remember { mutableStateOf(SecurityType.NONE) }
    var password by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("") }

    var batteryIncluded by remember { mutableStateOf(false) }
    var simIncluded by remember { mutableStateOf(false) }
    var memoryCardIncluded by remember { mutableStateOf(false) }
    var simTrayIncluded by remember { mutableStateOf(false) }
    var backCoverIncluded by remember { mutableStateOf(false) }
    var deadPhonePermission by remember { mutableStateOf(false) }

    var fieldsInitialized by remember { mutableStateOf(false) }

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
        )
    }

    LaunchedEffect(repairId) {
        viewModel.loadRepair(repairId)
    }

    LaunchedEffect(uiState.repair) {
        val repair = uiState.repair
        if (repair != null && !fieldsInitialized) {
            customerName = repair.customerName
            phoneNumber = repair.phoneNumber
            deviceModel = repair.deviceModel
            problemDescription = repair.problemDescription
            expectedDeliveryDate = repair.expectedDeliveryDate
            paymentInfo = repair.paymentInfo
            additionalDetails = repair.additionalDetails
            boxNumber = repair.boxNumber
            status = repair.status
            securityType = repair.securityType
            password = repair.password
            pattern = repair.pattern
            batteryIncluded = repair.batteryIncluded
            simIncluded = repair.simIncluded
            memoryCardIncluded = repair.memoryCardIncluded
            simTrayIncluded = repair.simTrayIncluded
            backCoverIncluded = repair.backCoverIncluded
            deadPhonePermission = repair.deadPhonePermission
            fieldsInitialized = true
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            Toast.makeText(context, "Repair record updated.", Toast.LENGTH_SHORT).show()
            viewModel.consumeOneTimeEvents()
            navController.popBackStack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit repair", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (!uiState.isLoadingInitialData) {
                Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            enabled = !uiState.isLoading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) { Text("Cancel") }

                        Button(
                            onClick = {
                                viewModel.updateRepair(
                                    repairId = repairId,
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
                                    deadPhonePermission = deadPhonePermission,
                                    status = status
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
                                Text("Save changes")
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoadingInitialData) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    uiState.repair?.let { repair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Serial: ${repair.serialNumber}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            StatusChip(status = status, onStatusSelected = { status = it })
                        }
                    }

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
                        Spacer(modifier = Modifier.height(6.dp))
                        LabeledCheckbox(
                            label = "Dead phone repair permitted",
                            checked = deadPhonePermission,
                            onCheckedChange = { deadPhonePermission = it },
                            enabled = !uiState.isLoading
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}