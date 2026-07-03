package com.appriyo.repairmanager.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appriyo.repairmanager.navigation.Screen
import com.appriyo.repairmanager.presentation.utils.buildStatusUpdateSms
import com.appriyo.repairmanager.presentation.utils.openSmsComposer
import com.appriyo.repairmanager.presentation.viewmodel.DeliveryListViewModel
import com.appriyo.repairmanager.presentation.viewmodel.PrintViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryListScreen(
    navController: NavHostController,
    filterType: String,
    printViewModel: PrintViewModel = koinViewModel()
) {
    val viewModel: DeliveryListViewModel = koinViewModel(
        key = "delivery_list_$filterType",
        parameters = { parametersOf(filterType) }
    )
    val uiState by viewModel.uiState.collectAsState()
    val printUiState by printViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(printUiState.successMessage) {
        printUiState.successMessage?.let {
            coroutineScope.launch { snackbarHostState.showSnackbar(it) }
            printViewModel.consumeSuccess()
        }
    }
    LaunchedEffect(printUiState.errorMessage) {
        printUiState.errorMessage?.let {
            coroutineScope.launch { snackbarHostState.showSnackbar(it) }
            printViewModel.consumeError()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            coroutineScope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.consumeError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.filter.displayTitle, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${uiState.repairs.size} repair${if (uiState.repairs.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.repairs.isEmpty() -> DeliveryEmptyState(filterTitle = uiState.filter.displayTitle)
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.repairs, key = { it.id }) { repair ->
                            RepairCard(
                                repair = repair,
                                thumbnail = null,
                                onClick = { navController.navigate(Screen.CustomerDetails.passId(repair.id)) },
                                onStatusSelected = { newStatus -> viewModel.updateStatus(repair.id, newStatus) },
                                onSendSms = {
                                    openSmsComposer(context, repair.phoneNumber, buildStatusUpdateSms(repair))
                                },
                                onPrint = { printViewModel.printRepair(repair) },
                                onEdit = { navController.navigate(Screen.EditRepair.passId(repair.id)) },
                                isPrinting = printUiState.isPrinting
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryEmptyState(filterTitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Filled.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
            Text(
                text = "No repairs in \"$filterTitle\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}