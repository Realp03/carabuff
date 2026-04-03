package com.example.carabuff

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationAdapter(
    private val list: MutableList<NotificationModel>,
    private val onClick: (NotificationModel) -> Unit,
    private val onLongClick: (NotificationModel) -> Unit,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    private val selectedIds = mutableSetOf<String>()
    var selectionMode: Boolean = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = view.findViewById(R.id.notifRoot)
        val unreadDot: View = view.findViewById(R.id.notifUnreadDot)
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

        holder.icon.text = when (notif.type.lowercase()) {
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
            holder.unreadDot.visibility = View.VISIBLE
            holder.title.setTypeface(null, Typeface.BOLD)
            holder.message.setTypeface(null, Typeface.BOLD)
            holder.title.alpha = 1.0f
            holder.message.alpha = 1.0f
            holder.time.alpha = 1.0f
            holder.icon.alpha = 1.0f
        } else {
            holder.unreadDot.visibility = View.GONE
            holder.title.setTypeface(null, Typeface.NORMAL)
            holder.message.setTypeface(null, Typeface.NORMAL)
            holder.title.alpha = 0.92f
            holder.message.alpha = 0.82f
            holder.time.alpha = 0.72f
            holder.icon.alpha = 0.92f
        }

        holder.root.isActivated = !notif.isRead

        if (selectionMode) {
            holder.check.visibility = View.VISIBLE
            holder.check.text = if (selectedIds.contains(notif.id)) "☑" else "☐"
        } else {
            holder.check.visibility = View.GONE
        }

        holder.root.setOnClickListener {
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

        holder.root.setOnLongClickListener {
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
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        if (diff < 0) return "Just now"

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            minutes == 1L -> "1 min"
            minutes in 2..59 -> "$minutes mins"
            hours == 1L -> "1 hour"
            hours in 2..23 -> "$hours hours"
            else -> {
                val sdf = SimpleDateFormat("MMM dd yyyy", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
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

        if (selectedIds.isEmpty()) {
            selectionMode = false
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

    fun getSelectedIds(): List<String> = selectedIds.toList()

    fun getSelectedCount(): Int = selectedIds.size

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