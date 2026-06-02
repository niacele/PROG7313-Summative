package com.example.mybudgetbuddysummative

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TotalSpendingAdapter(private val items: List<SpendingItem>) :
    RecyclerView.Adapter<TotalSpendingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rowTitle: TextView = view.findViewById(R.id.rowSpendingTitle)
        val rowProgress: ProgressBar = view.findViewById(R.id.rowSpendingProgressBar)
        val rowSpent: TextView = view.findViewById(R.id.rowSpendingCurrentSpent)
        val rowGoal: TextView = view.findViewById(R.id.rowSpendingTotalBudget)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_total_spending_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.rowTitle.text = item.envelopeName
        holder.rowSpent.text = "R%.2f".format(item.spentAmount)
        holder.rowGoal.text = "R%.2f".format(item.goalAmount)

        val progress = if (item.goalAmount > 0) {
            ((item.spentAmount / item.goalAmount) * 100).toInt().coerceAtMost(100)
        } else 0
        holder.rowProgress.progress = progress
    }

    override fun getItemCount(): Int = items.size
}
