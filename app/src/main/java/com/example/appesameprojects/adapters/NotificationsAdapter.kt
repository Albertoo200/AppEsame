package com.example.appesameprojects.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appesameprojects.R
import com.example.appesameprojects.models.NotificationModel

class NotificationsAdapter(
    private val notificationsList: MutableList<NotificationModel>,
    private val onNotificationClick: (NotificationModel) -> Unit,
    private val onCloseNotificationClick: (NotificationModel) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val notificationMessage: TextView = itemView.findViewById(R.id.notification_message)
        val notificationTimestamp: TextView = itemView.findViewById(R.id.notification_timestamp)
        val closeNotification: ImageView = itemView.findViewById(R.id.close_notification)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val currentNotification = notificationsList[position]
        holder.notificationMessage.text = currentNotification.message
        holder.notificationTimestamp.text = android.text.format.DateFormat.format("dd/MM/yyyy hh:mm:ss", currentNotification.timestamp)

        // Gestione del click sulla notifica
        holder.itemView.setOnClickListener {
            onNotificationClick(currentNotification)
        }

        // Gestione del click sulla "X"
        holder.closeNotification.setOnClickListener {
            onCloseNotificationClick(currentNotification)
        }
    }

    override fun getItemCount(): Int = notificationsList.size
}


