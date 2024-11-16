package com.example.appesameprojects.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appesameprojects.R
import com.example.appesameprojects.adapters.MessageAdapter
import com.example.appesameprojects.models.ChatModel
import com.example.appesameprojects.models.MessageModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class ChatDialog(private val chatId: String) : DialogFragment() {

    private lateinit var messagesDatabase: DatabaseReference
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<MessageModel>()

    private lateinit var auth: FirebaseAuth

    private lateinit var sendButton: Button
    private lateinit var messageInput: EditText


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_chat, container, false)

        auth = FirebaseAuth.getInstance()

        // Inizializzazione sendButton
        sendButton = view.findViewById(R.id.send_button)

        // Inizializzazione EditText
        messageInput = view.findViewById(R.id.message_text)

        // Inizializza RecyclerView
        messagesRecyclerView = view.findViewById(R.id.messages_recycler_view)

        // Configurazione layout RecyclerView
        messagesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Inizializzazione dell'adapter
        messageAdapter = MessageAdapter(messageList)

        // Imposta l'adapter alla RecyclerView
        messagesRecyclerView.adapter = messageAdapter

        // Inizializza il database
        messagesDatabase = FirebaseDatabase.getInstance().getReference("Messages")

        // Carica i messaggi della chat
        retrieveMessages()

        // Gestisci il click su Invia
        sendButton.setOnClickListener {
            val messageText = messageInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                messageInput.text.clear()
            } else {
                Toast.makeText(requireContext(), "Inserisci un messaggio", Toast.LENGTH_SHORT).show()
            }
        }


        return view
    }

    // Funzione di recupero messages
    private fun retrieveMessages() {
        messagesDatabase.orderByChild("chatId").equalTo(chatId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()
                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(MessageModel::class.java)
                        if (message != null) {
                            messageList.add(message)
                        }
                    }
                    // Ordina i messaggi per timestamp crescente
                    messageList.sortBy { it.timestamp }

                    messageAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Errore nel recupero dei messaggi.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Funzione di recupero dei dati della chat
    private fun getChatDataAndSenderReceiverIds(callback: (senderId: String, receiverId: String, chatterOneId: String, chatterTwoId: String) -> Unit) {
        val chatDatabase = FirebaseDatabase.getInstance().getReference("Chats")
        chatDatabase.child(chatId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chat = snapshot.getValue(ChatModel::class.java)
                if (chat != null) {
                    // Id dell'utente corrente
                    val currentUserId = auth.currentUser?.uid
                    val senderId: String
                    val receiverId: String

                    // Se currentUserId è chatterOneId, allora senderId è chatterOneId, altrimenti è chatterTwoId
                    if (currentUserId == chat.chatterOneId) {
                        senderId = chat.chatterOneId
                        receiverId = chat.chatterTwoId
                    } else {
                        senderId = chat.chatterTwoId
                        receiverId = chat.chatterOneId
                    }

                    // Restituzione i valori tramite callback
                    callback(senderId, receiverId, chat.chatterOneId, chat.chatterTwoId)
                } else {
                    Toast.makeText(requireContext(), "Chat non trovata.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Errore nel recupero dei dati della chat.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Funzione che invia un messaggio
    private fun sendMessage(messageText: String) {
        getChatDataAndSenderReceiverIds { senderId, receiverId, _, _ ->
            // Creazione nuovo messaggio
            val messageId = messagesDatabase.push().key ?: return@getChatDataAndSenderReceiverIds
            val message = MessageModel(
                messageId = messageId,
                senderId = senderId,
                message = messageText,
                timestamp = System.currentTimeMillis(),
                receiverId = receiverId,
                chatId = chatId
            )

            // Salvataggio nel database
            messagesDatabase.child(messageId).setValue(message).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Messaggio inviato", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Errore nell'invio del messaggio", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}

