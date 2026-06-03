package com.example.mybudgetbuddysummative

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AllocationAdapter(private val items: List<AllocationItem>) :
    RecyclerView.Adapter<AllocationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rowDate: TextView = view.findViewById(R.id.rowDate)
        val rowSource: TextView = view.findViewById(R.id.rowSource)
        val rowAmount: TextView = view.findViewById(R.id.rowAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense_row, parent, false) // ✅ matches your XML
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.rowDate.text = item.date
        holder.rowSource.text = item.source
        holder.rowAmount.text = CurrencyHelper.formatAmount(item.amount, UserSession.currency)
    }

    override fun getItemCount(): Int = items.size
}
