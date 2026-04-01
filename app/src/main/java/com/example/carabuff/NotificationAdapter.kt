package com.example.carabuff

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val list: MutableList<NotificationModel>,
    private val onClick: (NotificationModel) -> Unit,
    private val onLongClick: (NotificationModel) -> Unit,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    private val selectedIds = mutableSetOf<String>()
    var selectionMode: Boolean = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: TextView = view.findViewById(R.id.notifIcon)
        val title: TextView = view.findViewById(R.id.notifTitle)
        val message: TextView = view.findViewById(R.id.notifMessage)
        val time: TextView = view.findViewById(R.id.notifTime)
        val check: TextView = view.findViewById(R.id.notifCheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notif = list[position]

        holder.title.text = notif.title
        holder.message.text = notif.message
        holder.time.text = formatTime(notif.timestamp)

        holder.icon.text = when (notif.type) {
            "workout" -> "💪"
            "food" -> "🍔"
            "meal" -> "🍽"
            "achievement" -> "🏆"
            "daily_reminder" -> "⏰"
            "daily_summary" -> "📊"
            "welcome" -> "🎉"
            "security" -> "🔐"
            "profile_update" -> "👤"
            else -> "🔔"
        }

        if (!notif.isRead) {
            holder.itemView.setBackgroundColor(Color.parseColor("#3A4A5F"))
            holder.title.setTypeface(null, Typeface.BOLD)
            holder.message.setTypeface(null, Typeface.BOLD)
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            holder.title.setTypeface(null, Typeface.NORMAL)
            holder.message.setTypeface(null, Typeface.NORMAL)
        }

        if (selectionMode) {
            holder.check.visibility = View.VISIBLE
            holder.check.text = if (selectedIds.contains(notif.id)) "☑" else "☐"
        } else {
            holder.check.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val currentNotif = list[currentPosition]

                if (selectionMode) {
                    toggleSelection(currentNotif.id)
                } else {
                    onClick(currentNotif)
                }
            }
        }

        holder.itemView.setOnLongClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val currentNotif = list[currentPosition]

                if (!selectionMode) {
                    startSelection(currentNotif.id)
                    onLongClick(currentNotif)
                } else {
                    toggleSelection(currentNotif.id)
                }
            }
            true
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun updateItem(updatedNotif: NotificationModel) {
        val index = list.indexOfFirst { it.id == updatedNotif.id }
        if (index != -1) {
            list[index] = updatedNotif
            notifyItemChanged(index)
        }
    }

    fun toggleSelection(id: String) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
        } else {
            selectedIds.add(id)
        }
        notifyDataSetChanged()
        onSelectionChanged()
    }

    fun startSelection(firstId: String) {
        selectionMode = true
        selectedIds.clear()
        selectedIds.add(firstId)
        notifyDataSetChanged()
        onSelectionChanged()
    }

    fun clearSelection() {
        selectionMode = false
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged()
    }

    fun getSelectedIds(): List<String> {
        return selectedIds.toList()
    }

    fun getSelectedCount(): Int {
        return selectedIds.size
    }

    fun selectAll() {
        selectionMode = true
        selectedIds.clear()
        for (notif in list) {
            if (notif.id.isNotEmpty()) {
                selectedIds.add(notif.id)
            }
        }
        notifyDataSetChanged()
        onSelectionChanged()
    }

    fun areAllSelected(): Boolean {
        val validIds = list.map { it.id }.filter { it.isNotEmpty() }
        return validIds.isNotEmpty() && selectedIds.size == validIds.size
    }
}