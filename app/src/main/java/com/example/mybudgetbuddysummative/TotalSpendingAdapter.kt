package com.example.mybudgetbuddysummative

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.min

class TotalSpendingAdapter(
    private val items: List<SpendingItem>,
    private val onItemClicked: (SpendingItem) -> Unit
) : RecyclerView.Adapter<TotalSpendingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rowSpendingTitle: TextView = view.findViewById(R.id.rowSpendingTitle)
        val rowSpendingProgressBar: ProgressBar = view.findViewById(R.id.rowSpendingProgressBar)
        val rowSpendingCurrentSpent: TextView = view.findViewById(R.id.rowSpendingCurrentSpent)
        val rowSpendingTotalBudget: TextView = view.findViewById(R.id.rowSpendingTotalBudget)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_total_spending_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // 1. Bind structural text labels
        holder.rowSpendingTitle.text = item.envelopeName
        holder.rowSpendingCurrentSpent.text = CurrencyHelper.formatAmount(item.totalSpent, UserSession.currency)
        holder.rowSpendingTotalBudget.text = CurrencyHelper.formatAmount(item.goalAmount, UserSession.currency)

        // 2. MATHEMATICAL PROGRESS CALCULATOR: Prevents division by zero crashes if the goal amount is unset or zero
        if (item.goalAmount > 0.0) {
            val progressPercentage = ((item.totalSpent / item.goalAmount) * 100).toInt()

            // Constrain progress indicator value inside maximum limits safely
            holder.rowSpendingProgressBar.progress = min(progressPercentage, 100)
        } else {
            // Default to empty state metrics if no budgeting target constraints exist
            if (item.totalSpent > 0.0) {
                holder.rowSpendingProgressBar.progress = 100 // Full bar state if money spent without limit boundaries
            } else {
                holder.rowSpendingProgressBar.progress = 0
            }
        }

        // 3. ROUTER LINK TRANSACTION: Launches detailed transaction log summaries when rows are tapped
        holder.itemView.setOnClickListener {
            onItemClicked(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
