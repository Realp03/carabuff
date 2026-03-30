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
    private val onClick: (NotificationModel) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: TextView = view.findViewById(R.id.notifIcon)
        val title: TextView = view.findViewById(R.id.notifTitle)
        val message: TextView = view.findViewById(R.id.notifMessage)
        val time: TextView = view.findViewById(R.id.notifTime)
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

        // 🔥 ICON BASED SA TYPE
        holder.icon.text = when (notif.type) {
            "workout" -> "💪"
            "food" -> "🍔"
            "achievement" -> "🏆"
            "daily_reminder" -> "⏰"
            "daily_summary" -> "📊"
            "welcome" -> "🎉"
            "meal" -> "🍽"
            else -> "🔔"
        }

        // 🔥 UNREAD HIGHLIGHT SYSTEM (IMPROVED)
        if (!notif.isRead) {
            holder.itemView.setBackgroundColor(Color.parseColor("#3A4A5F"))
            holder.title.setTypeface(null, Typeface.BOLD)
            holder.message.setTypeface(null, Typeface.BOLD)
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            holder.title.setTypeface(null, Typeface.NORMAL)
            holder.message.setTypeface(null, Typeface.NORMAL)
        }

        // 🔥 CLICK EVENT (SAFE POSITION)
        holder.itemView.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                onClick(list[currentPosition])
            }
        }
    }

    // 🔥 FORMAT TIME
    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // 🔥 OPTIONAL: UPDATE SINGLE ITEM (SMOOTH UI)
    fun updateItem(updatedNotif: NotificationModel) {
        val index = list.indexOfFirst { it.id == updatedNotif.id }
        if (index != -1) {
            list[index] = updatedNotif
            notifyItemChanged(index)
        }
    }
}