// app/src/main/java/com/appriyo/repairmanager/data/repository/CashBoxRepository.kt
package com.appriyo.repairmanager.data.repository

import com.appriyo.repairmanager.domain.cashbox.CashBoxSummary
import com.appriyo.repairmanager.domain.cashbox.CashBoxType
import com.appriyo.repairmanager.domain.cashbox.CashTransaction
import com.appriyo.repairmanager.domain.cashbox.TransactionType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CashBoxRepository(
private val firestore: FirebaseFirestore,
private val userProvider: FirestoreUserProvider
) {

private fun boxDocument(type: CashBoxType) = userProvider
.currentUserDocument()
.collection(CASH_BOXES_COLLECTION)
.document(type.firestoreId)

private fun transactionsCollection(type: CashBoxType) =
boxDocument(type).collection(TRANSACTIONS_SUBCOLLECTION)

/** Realtime summary (balance / totals / count) for the given account. */
fun observeSummary(type: CashBoxType): Flow<CashBoxSummary> = callbackFlow {
val registration = boxDocument(type).addSnapshotListener { snapshot, error ->
if (error != null) {
close(error)
return@addSnapshotListener
}
val summary = if (snapshot != null && snapshot.exists()) {
CashBoxSummary(
currentBalance = snapshot.getDouble(FIELD_CURRENT_BALANCE) ?: 0.0,
totalIncome = snapshot.getDouble(FIELD_TOTAL_INCOME) ?: 0.0,
totalExpense = snapshot.getDouble(FIELD_TOTAL_EXPENSE) ?: 0.0,
transactionCount = (snapshot.getLong(FIELD_TRANSACTION_COUNT) ?: 0L).toInt(),
updatedAt = snapshot.getTimestamp(FIELD_UPDATED_AT)?.toDate()
)
} else {
CashBoxSummary()
}
trySend(summary)
}
awaitClose { registration.remove() }
}

/** Realtime transaction history, newest first. */
fun observeTransactions(type: CashBoxType): Flow<List<CashTransaction>> = callbackFlow {
val registration = transactionsCollection(type)
.orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
.addSnapshotListener { snapshot, error ->
if (error != null) {
close(error)
return@addSnapshotListener
}
val transactions = snapshot?.documents.orEmpty().map { doc ->
CashTransaction(
id = doc.id,
title = doc.getString(FIELD_TITLE).orEmpty(),
description = doc.getString(FIELD_DESCRIPTION).orEmpty(),
amount = doc.getDouble(FIELD_AMOUNT) ?: 0.0,
type = runCatching {
TransactionType.valueOf(doc.getString(FIELD_TYPE) ?: TransactionType.INCOME.name)
}.getOrDefault(TransactionType.INCOME),
createdAt = doc.getTimestamp(FIELD_CREATED_AT)?.toDate()
)
}
trySend(transactions)
}
awaitClose { registration.remove() }
}

/** Adds a transaction and atomically updates the account summary. */
suspend fun addTransaction(
type: CashBoxType,
title: String,
description: String,
amount: Double,
transactionType: TransactionType
): Result<Unit> = runCatching {
require(title.isNotBlank()) { "Title is required" }
require(amount > 0) { "Amount must be greater than zero" }

val boxDoc = boxDocument(type)
val newTransactionRef = transactionsCollection(type).document()

firestore.runTransaction { txn ->
val snapshot = txn.get(boxDoc)
val currentBalance = snapshot.getDouble(FIELD_CURRENT_BALANCE) ?: 0.0
val totalIncome = snapshot.getDouble(FIELD_TOTAL_INCOME) ?: 0.0
val totalExpense = snapshot.getDouble(FIELD_TOTAL_EXPENSE) ?: 0.0
val transactionCount = snapshot.getLong(FIELD_TRANSACTION_COUNT) ?: 0L

val signedAmount = if (transactionType == TransactionType.INCOME) amount else -amount

txn.set(
boxDoc,
mapOf(
FIELD_CURRENT_BALANCE to currentBalance + signedAmount,
FIELD_TOTAL_INCOME to if (transactionType == TransactionType.INCOME) totalIncome + amount else totalIncome,
FIELD_TOTAL_EXPENSE to if (transactionType == TransactionType.EXPENSE) totalExpense + amount else totalExpense,
FIELD_TRANSACTION_COUNT to transactionCount + 1,
FIELD_UPDATED_AT to FieldValue.serverTimestamp()
),
SetOptions.merge()
)

txn.set(
newTransactionRef,
mapOf(
FIELD_TITLE to title,
FIELD_DESCRIPTION to description,
FIELD_AMOUNT to amount,
FIELD_TYPE to transactionType.name,
FIELD_CREATED_AT to FieldValue.serverTimestamp()
)
)
null
}.await()
}

/** Edits an existing transaction and rebalances the summary by the delta. */
suspend fun updateTransaction(
type: CashBoxType,
original: CashTransaction,
newTitle: String,
newDescription: String,
newAmount: Double,
newType: TransactionType
): Result<Unit> = runCatching {
require(newTitle.isNotBlank()) { "Title is required" }
require(newAmount > 0) { "Amount must be greater than zero" }
require(original.id.isNotBlank()) { "Transaction id is required" }

val boxDoc = boxDocument(type)
val transactionDoc = transactionsCollection(type).document(original.id)

val oldSigned = original.signedAmount
val newSigned = if (newType == TransactionType.INCOME) newAmount else -newAmount
val balanceDelta = newSigned - oldSigned

val oldIncomeContribution = if (original.type == TransactionType.INCOME) original.amount else 0.0
val oldExpenseContribution = if (original.type == TransactionType.EXPENSE) original.amount else 0.0
val newIncomeContribution = if (newType == TransactionType.INCOME) newAmount else 0.0
val newExpenseContribution = if (newType == TransactionType.EXPENSE) newAmount else 0.0

firestore.runTransaction { txn ->
val snapshot = txn.get(boxDoc)
val currentBalance = snapshot.getDouble(FIELD_CURRENT_BALANCE) ?: 0.0
val totalIncome = snapshot.getDouble(FIELD_TOTAL_INCOME) ?: 0.0
val totalExpense = snapshot.getDouble(FIELD_TOTAL_EXPENSE) ?: 0.0

txn.set(
boxDoc,
mapOf(
FIELD_CURRENT_BALANCE to currentBalance + balanceDelta,
FIELD_TOTAL_INCOME to totalIncome - oldIncomeContribution + newIncomeContribution,
FIELD_TOTAL_EXPENSE to totalExpense - oldExpenseContribution + newExpenseContribution,
FIELD_UPDATED_AT to FieldValue.serverTimestamp()
),
SetOptions.merge()
)

txn.update(
transactionDoc,
mapOf(
FIELD_TITLE to newTitle,
FIELD_DESCRIPTION to newDescription,
FIELD_AMOUNT to newAmount,
FIELD_TYPE to newType.name
)
)
null
}.await()
}

/** Deletes a transaction and reverses its effect on the summary. */
suspend fun deleteTransaction(type: CashBoxType, transaction: CashTransaction): Result<Unit> = runCatching {
require(transaction.id.isNotBlank()) { "Transaction id is required" }

val boxDoc = boxDocument(type)
val transactionDoc = transactionsCollection(type).document(transaction.id)
val removedSigned = transaction.signedAmount
val removedIncome = if (transaction.type == TransactionType.INCOME) transaction.amount else 0.0
val removedExpense = if (transaction.type == TransactionType.EXPENSE) transaction.amount else 0.0

firestore.runTransaction { txn ->
val snapshot = txn.get(boxDoc)
val currentBalance = snapshot.getDouble(FIELD_CURRENT_BALANCE) ?: 0.0
val totalIncome = snapshot.getDouble(FIELD_TOTAL_INCOME) ?: 0.0
val totalExpense = snapshot.getDouble(FIELD_TOTAL_EXPENSE) ?: 0.0
val transactionCount = snapshot.getLong(FIELD_TRANSACTION_COUNT) ?: 0L

txn.set(
boxDoc,
mapOf(
FIELD_CURRENT_BALANCE to currentBalance - removedSigned,
FIELD_TOTAL_INCOME to totalIncome - removedIncome,
FIELD_TOTAL_EXPENSE to totalExpense - removedExpense,
FIELD_TRANSACTION_COUNT to (transactionCount - 1).coerceAtLeast(0L),
FIELD_UPDATED_AT to FieldValue.serverTimestamp()
),
SetOptions.merge()
)
txn.delete(transactionDoc)
null
}.await()
}

private companion object {
const val CASH_BOXES_COLLECTION = "cashBoxes"
const val TRANSACTIONS_SUBCOLLECTION = "transactions"

const val FIELD_CURRENT_BALANCE = "currentBalance"
const val FIELD_TOTAL_INCOME = "totalIncome"
const val FIELD_TOTAL_EXPENSE = "totalExpense"
const val FIELD_TRANSACTION_COUNT = "transactionCount"
const val FIELD_UPDATED_AT = "updatedAt"

const val FIELD_TITLE = "title"
const val FIELD_DESCRIPTION = "description"
const val FIELD_AMOUNT = "amount"
const val FIELD_TYPE = "type"
const val FIELD_CREATED_AT = "createdAt"
}
}