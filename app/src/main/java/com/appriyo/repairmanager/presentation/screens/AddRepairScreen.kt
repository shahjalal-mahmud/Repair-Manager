// app/src/main/java/com/appriyo/repairmanager/presentation/screens/AddRepairScreen.kt
package com.appriyo.repairmanager.presentation.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.appriyo.repairmanager.data.media.MediaAttachment
import com.appriyo.repairmanager.data.media.MediaType
import com.appriyo.repairmanager.data.model.SecurityType
import com.appriyo.repairmanager.presentation.components.OptionDropdown
import com.appriyo.repairmanager.presentation.components.SectionCard
import com.appriyo.repairmanager.presentation.viewmodel.AddRepairViewModel
import com.appriyo.repairmanager.util.ContactsHelper
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.util.Calendar
import java.util.UUID
import kotlin.collections.flatMap
import androidx.core.net.toUri
import com.appriyo.repairmanager.presentation.components.MediaCaptureSection

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

    // Phonebook feature - local UI state only. The actual contact storage
    // happens in the device's Contacts app via ContactsHelper; nothing in
    // these booleans is persisted to Firestore.
    var saveContactToPhonebook by remember { mutableStateOf(false) }
    var showContactsDenied by remember { mutableStateOf(false) }
    var showNoPhoneSnackbar by remember { mutableStateOf(false) }

    var securityType by remember { mutableStateOf(SecurityType.NONE) }
    var password by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("") }

    // Accessories - unselected by default, the shop owner taps what was actually received
    var batteryIncluded by remember { mutableStateOf(false) }
    var simIncluded by remember { mutableStateOf(false) }
    var memoryCardIncluded by remember { mutableStateOf(false) }
    var simTrayIncluded by remember { mutableStateOf(false) }
    var backCoverIncluded by remember { mutableStateOf(false) }
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

    // WRITE_CONTACTS permission launcher - drives the
    // "Save this contact to my phonebook" Switch. We do NOT optimistically
    // flip `saveContactToPhonebook` to true on toggle; we wait for the
    // granted callback so the visual state stays accurate if the user
    // dismissed the dialog.
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            saveContactToPhonebook = true
        } else {
            saveContactToPhonebook = false
            showContactsDenied = true
        }
    }

    // System contact picker launcher - returns a Uri? for the picked
    // contact, or null if the user cancelled. No READ_CONTACTS permission
    // is needed; the picker grants our app a one-shot URI grant.
    val pickContactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val picked = ContactsHelper.queryPickedContact(context, uri)
        if (picked == null) {
            showNoPhoneSnackbar = true
        } else {
            customerName = picked.displayName
            phoneNumber = picked.phoneNumber
        }
    }

    val mediaAttachmentsSaver = listSaver<List<MediaAttachment>, String>(
        save = { list -> list.flatMap { listOf(it.uri.toString(), it.type.name) } },
        restore = { flat -> flat.chunked(2).map { (uriStr, typeName) ->
            MediaAttachment(uriStr.toUri(), MediaType.valueOf(typeName))
        } }
    )
    var mediaAttachments by rememberSaveable(stateSaver = mediaAttachmentsSaver) {
        mutableStateOf(emptyList())
    }
    var draftId by remember { mutableStateOf(UUID.randomUUID().toString()) }

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
        batteryIncluded = false
        simIncluded = false
        memoryCardIncluded = false
        simTrayIncluded = false
        backCoverIncluded = false
        deadPhonePermission = false
        mediaAttachments = emptyList()
        draftId = UUID.randomUUID().toString()
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

            // Phonebook side-effect: if the user opted in via the
            // "Save this contact to my phonebook" switch, persist the typed
            // name+number to the device's Contacts app now. This runs in
            // the screen's coroutineScope (not the ViewModel) so the VM
            // stays Firestore-only.
            //
            // Capture the values BEFORE clearFields() - clearFields() resets
            // customerName / phoneNumber to "" and would erase them.
            if (saveContactToPhonebook) {
                val nameToSave = customerName
                val phoneToSave = phoneNumber
                if (nameToSave.isNotBlank() && phoneToSave.isNotBlank()) {
                    coroutineScope.launch {
                        val ok = ContactsHelper.saveToPhonebook(
                            context = context,
                            displayName = nameToSave,
                            phoneNumber = phoneToSave
                        )
                        val msg = if (ok) {
                            "Contact saved to phonebook."
                        } else {
                            "Couldn't save contact to phonebook."
                        }
                        snackbarHostState.showSnackbar(msg)
                    }
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            "Add customer name and phone to save to phonebook."
                        )
                    }
                }
            }

            clearFields()
            viewModel.consumeOneTimeEvents()
        }
    }

    // WRAP_CONTACTS denied (incl. "don't ask again") - offer a "Settings"
    // action that opens the app-info page so the user can flip the grant on.
    LaunchedEffect(showContactsDenied) {
        if (showContactsDenied) {
            val result = snackbarHostState.showSnackbar(
                message = "Contacts permission denied - open Settings to grant it.",
                actionLabel = "Settings",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                )
            }
            showContactsDenied = false
        }
    }

    // Picked contact had no usable phone number - tell the user but leave
    // the existing Customer Name / Phone fields untouched.
    LaunchedEffect(showNoPhoneSnackbar) {
        if (showNoPhoneSnackbar) {
            snackbarHostState.showSnackbar("That contact has no phone number.")
            showNoPhoneSnackbar = false
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
                                customerName,
                                phoneNumber,
                                deviceModel,
                                problemDescription,
                                expectedDeliveryDate,
                                paymentInfo,
                                additionalDetails,
                                boxNumber,
                                securityType,
                                password,
                                pattern,
                                batteryIncluded,
                                simIncluded,
                                memoryCardIncluded,
                                simTrayIncluded,
                                backCoverIncluded,
                                deadPhonePermission,
                                draftId = draftId,
                                photoCount = mediaAttachments.count { it.type == MediaType.PHOTO },
                                videoCount = mediaAttachments.count { it.type == MediaType.VIDEO }
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
                                memoryCardIncluded, simTrayIncluded, backCoverIncluded, deadPhonePermission,
                                draftId = draftId,
                                photoCount = mediaAttachments.count { it.type == MediaType.PHOTO },
                                videoCount = mediaAttachments.count { it.type == MediaType.VIDEO }
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
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number (11 digits, optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = uiState.fieldErrors.containsKey("phoneNumber"),
                    supportingText = { uiState.fieldErrors["phoneNumber"]?.let { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Phonebook picker button - sits on its OWN line (not next
                // to the customer-name field) so the form reads cleanly and
                // the button gets full visual weight.
                OutlinedButton(
                    onClick = { pickContactLauncher.launch(null) },
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContactPhone,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick customer from phonebook")
                }

                Spacer(modifier = Modifier.height(14.dp))

                // HorizontalDivider separates the input fields above from
                // the save-to-phonebook preference below.
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Save to phonebook",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Add this customer to your device contacts when saving the repair.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = saveContactToPhonebook,
                        enabled = !uiState.isLoading,
                        onCheckedChange = { wantOn ->
                            if (!wantOn) {
                                saveContactToPhonebook = false
                                return@Switch
                            }
                            // Short-circuit if already granted (e.g. after
                            // a process restart where local state resets to
                            // OFF but the OS-level grant persists).
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.WRITE_CONTACTS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                saveContactToPhonebook = true
                            } else {
                                contactsPermissionLauncher.launch(
                                    Manifest.permission.WRITE_CONTACTS
                                )
                            }
                        }
                    )
                }
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

            SectionCard(title = "Photos & Videos", icon = Icons.Filled.PhotoLibrary) {
                MediaCaptureSection(
                    attachments = mediaAttachments,
                    onAdd = { mediaAttachments = mediaAttachments + it },
                    onRemove = { item ->
                        // physically delete the file on disk, then drop it from the list
                        com.appriyo.repairmanager.data.media.MediaStorageManager(context).delete(item.uri)
                        mediaAttachments = mediaAttachments - item
                    },
                    draftId = draftId,
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
                Text(
                    text = "Tap to mark what the customer handed over",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccessoryChip("Battery", batteryIncluded, { batteryIncluded = it }, !uiState.isLoading, Modifier.weight(1f))
                    AccessoryChip("SIM", simIncluded, { simIncluded = it }, !uiState.isLoading, Modifier.weight(1f))
                    AccessoryChip("Memory", memoryCardIncluded, { memoryCardIncluded = it }, !uiState.isLoading, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccessoryChip("SIM Tray", simTrayIncluded, { simTrayIncluded = it }, !uiState.isLoading, Modifier.weight(1f))
                    AccessoryChip("Back Cover", backCoverIncluded, { backCoverIncluded = it }, !uiState.isLoading, Modifier.weight(1f))
                }
            }

            // Dead phone permission - one compact, switch-driven line
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Allow repair attempt on a dead/unpowered phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = deadPhonePermission,
                        onCheckedChange = { deadPhonePermission = it },
                        enabled = !uiState.isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccessoryChip(
    label: String,
    selected: Boolean,
    onToggle: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = { onToggle(!selected) },
        enabled = enabled,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = modifier
    )
}