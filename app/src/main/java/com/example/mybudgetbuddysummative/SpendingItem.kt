package com.example.mybudgetbuddysummative

data class SpendingItem(
    val envelopeId: String,
    val envelopeName: String,
    val totalSpent: Double,
    val goalAmount: Double
)
