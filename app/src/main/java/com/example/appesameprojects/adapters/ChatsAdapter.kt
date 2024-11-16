package com.example.appesameprojects.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appesameprojects.R
import com.example.appesameprojects.models.ChatModel

class ChatsAdapter (
    private val chatList: List<ChatModel>,
    private val currentUserId: String,
    private val onChatClick: (ChatModel) -> Unit,
    ): RecyclerView.Adapter<ChatsAdapter.ChatsViewHolder>() {

    inner class ChatsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val chatterOne: TextView = view.findViewById(R.id.chatter_one)
        val chatterTwo: TextView = view.findViewById(R.id.chatter_two)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return ChatsViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatsViewHolder, position: Int) {
        val currentChat = chatList[position]

        // Verifica se il currentUserId corrisponde a chatterOneId o chatterTwoId
        if (currentChat.chatterOneId == currentUserId) {
            // Se l'ID dell'utente corrente è chatterOneId, mostra chatterTwoName
            holder.chatterOne.visibility = View.GONE
            holder.chatterTwo.text = currentChat.chatterTwoName
            holder.chatterTwo.visibility = View.VISIBLE
        } else if (currentChat.chatterTwoId == currentUserId) {
            // Se l'ID dell'utente corrente è chatterTwoId, mostra chatterOneName
            holder.chatterTwo.visibility = View.GONE
            holder.chatterOne.text = currentChat.chatterOneName
            holder.chatterOne.visibility = View.VISIBLE
        }

        // Gestione del click sulla chat
        holder.itemView.setOnClickListener {
            onChatClick(currentChat)
        }
    }

    override fun getItemCount() = chatList.size

}