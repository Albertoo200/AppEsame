package com.example.appesameprojects.adapters
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appesameprojects.R
import com.example.appesameprojects.models.MessageModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val messageList: List<MessageModel>
) : RecyclerView.Adapter<MessageAdapter.ChatViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Costanti per identificare i tipi di vista
    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2

    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]
        return if (message.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = if (viewType == VIEW_TYPE_SENT) {
            LayoutInflater.from(parent.context).inflate(R.layout.item_message_sent, parent, false)
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.item_message_received, parent, false)
        }
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messageList[position]
        holder.messageText.text = message.message
        holder.timestampText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    override fun getItemCount() = messageList.size

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.message_text)
        val timestampText: TextView = view.findViewById(R.id.message_timestamp)
    }
}
