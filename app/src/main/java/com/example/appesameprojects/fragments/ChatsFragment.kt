package com.example.appesameprojects.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appesameprojects.R
import com.example.appesameprojects.adapters.ChatsAdapter
import com.example.appesameprojects.dialogs.ChatDialog
import com.example.appesameprojects.dialogs.CreateChatDialog
import com.example.appesameprojects.models.ChatModel
import com.example.appesameprojects.models.ProjectModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ChatsFragment : Fragment() {

    private lateinit var usersDatabase: DatabaseReference

    private lateinit var chatsDatabase: DatabaseReference
    private lateinit var chatsRecyclerView: RecyclerView
    private lateinit var chatsAdapter: ChatsAdapter
    private val chatList = mutableListOf<ChatModel>()

    private lateinit var newChatButton: FloatingActionButton

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        auth = FirebaseAuth.getInstance()

        // Inizializzazione newChatButton
        newChatButton = view.findViewById(R.id.new_chat_button)

        // Inizializzazione RecyclerView
        chatsRecyclerView = view.findViewById(R.id.chats_recycler_view)

        // Configurazione layout RecyclerView
        chatsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Inizializzazione degli adapter
        chatsAdapter = ChatsAdapter(chatList, auth.currentUser?.uid ?: "",
            onChatClick = { chat -> onChatClick(chat) },
        )

        // Imposta gli adapter alle RecyclerView
        chatsRecyclerView.adapter = chatsAdapter

        // Inizializza il database
        usersDatabase = FirebaseDatabase.getInstance().reference.child("User")
        chatsDatabase = FirebaseDatabase.getInstance().getReference("Chats")

        // Recupera i dati dal database in base all'account
        displayContentBasedOnAccountId()

        // Quando newChatButton viene premuto
        newChatButton.setOnClickListener {
            showCreateChatDialog()
        }

        return view
    }

    // Funzione per gestire la visualizzazione dei contenuti in base all'utente loggato
    private fun displayContentBasedOnAccountId() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            retrieveChats(currentUserId)
        }
    }

    // Funzione di recupero delle chat
    private fun retrieveChats(currentUserId: String) {
        chatsDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()
                for (chatSnapshot in snapshot.children) {
                    val chat = chatSnapshot.getValue(ChatModel::class.java)
                    if (chat != null) {
                        if (chat.chatterOneId == currentUserId || chat.chatterTwoId == currentUserId) {
                            chatList.add(chat)
                        }
                    }
                }
                chatsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Errore nel recupero delle chat.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Funzione di gestione del click sulla chat
    private fun onChatClick(chat: ChatModel) {
        showChatDialog(chat)
    }

    // Funzione che mostra una finestra con la chat
    private fun showChatDialog(chat: ChatModel) {
        val chatDialog = ChatDialog(chat.chatId)
        chatDialog.show(childFragmentManager, "ChatDialog")
    }

    // Funzione che apre la creazione di una nuova chat
    private fun showCreateChatDialog() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            usersDatabase.child(currentUserId).get().addOnSuccessListener { snapshot ->
                val accountType = snapshot.child("accountType").getValue(String::class.java)
                if (accountType != null) {
                    val createChatDialog = CreateChatDialog(accountType)
                    createChatDialog.show(childFragmentManager, "CreateChatDialog")
                }
            }
        }
    }
}