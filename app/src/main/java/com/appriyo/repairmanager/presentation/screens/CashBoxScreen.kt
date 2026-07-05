// app/src/main/java/com/appriyo/repairmanager/presentation/screens/CashBoxScreen.kt
package com.appriyo.repairmanager.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appriyo.repairmanager.domain.cashbox.CashBoxType
import com.appriyo.repairmanager.domain.cashbox.CashTransaction
import com.appriyo.repairmanager.presentation.components.AddTransactionDialog
import com.appriyo.repairmanager.presentation.components.CashBoxBalanceCard
import com.appriyo.repairmanager.presentation.components.CashBoxEmptyState
import com.appriyo.repairmanager.presentation.components.CashBoxLoadingState
import com.appriyo.repairmanager.presentation.components.CashBoxStatsRow
import com.appriyo.repairmanager.presentation.components.CashTransactionCard
import com.appriyo.repairmanager.presentation.components.DeleteConfirmationDialog
import com.appriyo.repairmanager.presentation.components.PinProtectedAction
import com.appriyo.repairmanager.presentation.viewmodel.CashBoxViewModel
import com.appriyo.repairmanager.presentation.viewmodel.SecurityViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * One reusable screen for both cash accounts. Which account it shows is
 * decided entirely by [accountType] - do not create a second screen for
 * Market Box.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashBoxScreen(
    navController: NavHostController,
    accountType: CashBoxType,
    viewModel: CashBoxViewModel = koinViewModel(
        key = "cashbox_${accountType.firestoreId}",
        parameters = { parametersOf(accountType) }
    ),
    securityViewModel: SecurityViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<CashTransaction?>(null) }
    var pendingDelete by remember { mutableStateOf<CashTransaction?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // PIN gate. Each sensitive action (add / edit / delete) prompts the owner
    // for their 6-digit PIN independently. The dialog and verification are
    // owned by PinProtectedAction / SecurityViewModel.
    val pinGate = PinProtectedAction(securityViewModel)

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(accountType.displayName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                pinGate.prompt { showAddDialog = true }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CashBoxBalanceCard(summary = uiState.summary, accountLabel = accountType.displayName)
            }
            item {
                CashBoxStatsRow(summary = uiState.summary)
            }
            item {
                Text(
                    text = "Transaction History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            when {
                uiState.isLoading -> item { CashBoxLoadingState() }
                uiState.transactions.isEmpty() -> item { CashBoxEmptyState(accountLabel = accountType.displayName) }
                else -> items(uiState.transactions, key = { it.id }) { transaction ->
                    CashTransactionCard(
                        transaction = transaction,
                        onClick = {
                            pinGate.prompt { editingTransaction = transaction }
                        },
                        onDeleteClick = {
                            pinGate.prompt { pendingDelete = transaction }
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            accountLabel = accountType.displayName,
            isSaving = uiState.isSaving,
            onDismiss = { showAddDialog = false },
            onSave = { title, description, amount, type ->
                viewModel.addTransaction(title, description, amount, type)
                showAddDialog = false
            }
        )
    }

    editingTransaction?.let { transaction ->
        AddTransactionDialog(
            accountLabel = accountType.displayName,
            existingTransaction = transaction,
            isSaving = uiState.isSaving,
            onDismiss = { editingTransaction = null },
            onSave = { title, description, amount, type ->
                viewModel.updateTransaction(transaction, title, description, amount, type)
                editingTransaction = null
            }
        )
    }

    pendingDelete?.let { transaction ->
        DeleteConfirmationDialog(
            title = "Delete Transaction",
            message = "Delete \"${transaction.title}\"? This will update the ${accountType.displayName} balance.",
            onConfirm = {
                viewModel.deleteTransaction(transaction)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}