package com.example.pulsecheck

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AlertHistoryAdapter(private val items: List<AlertHistoryItem>) :
    RecyclerView.Adapter<AlertHistoryAdapter.HistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_event, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = items[position]
        holder.tvDate.text = item.getDateDisplay()
        holder.tvTime.text = item.getTimeDisplay()
        holder.tvTrigger.text = item.getTriggerType()
        holder.tvLocation.text = item.getLocation()
        holder.tvStatus.text = item.getStatus()
        var contactsText = "${item.getContactsSent()} contact(s) alerted"
        if (item.hasPathData()) {
            contactsText += " • Path: ${item.getFormattedDistance()} in ${item.getFormattedDuration()}"
        }
        holder.tvContacts.text = contactsText
    }

    override fun getItemCount() = items.size

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tv_history_date)
        val tvTime: TextView = itemView.findViewById(R.id.tv_history_time)
        val tvTrigger: TextView = itemView.findViewById(R.id.tv_history_trigger)
        val tvLocation: TextView = itemView.findViewById(R.id.tv_history_location)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_history_status)
        val tvContacts: TextView = itemView.findViewById(R.id.tv_history_contacts)
    }
}
