package com.example.mybudgetbuddysummative

object CurrencyHelper {
    fun formatAmount(amount: Double, currency: String): String {
        return when (currency) {
            "ZAR" -> "R${String.format("%.2f", amount)}"
            "USD" -> "$${String.format("%.2f", amount)}"
            "GBP" -> "£${String.format("%.2f", amount)}"
            else -> "R${String.format("%.2f", amount)}"
        }
    }
}

