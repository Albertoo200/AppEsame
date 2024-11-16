package com.example.appesameprojects.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.appesameprojects.R
import com.example.appesameprojects.models.MessageModel
import com.example.appesameprojects.models.ChatModel
import com.example.appesameprojects.models.TaskModel
import com.example.appesameprojects.models.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CreateChatDialog(private val accountType: String) : DialogFragment() {

    private lateinit var usersDatabase: DatabaseReference
    private lateinit var tasksDatabase: DatabaseReference
    private lateinit var messagesDatabase: DatabaseReference
    private lateinit var chatsDatabase: DatabaseReference
    private lateinit var currentUserId: String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        usersDatabase = FirebaseDatabase.getInstance().reference.child("User")
        tasksDatabase = FirebaseDatabase.getInstance().reference.child("Tasks")
        chatsDatabase = FirebaseDatabase.getInstance().reference.child("Chats")
        messagesDatabase = FirebaseDatabase.getInstance().reference.child("Messages")

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_create_chat, null)

        val spinnerReceiver: Spinner = view.findViewById(R.id.receiver)
        val messageEditText: EditText = view.findViewById(R.id.edit_message_text)

        // Carica i dati appropriati in base al tipo di account
        loadUsers(accountType, spinnerReceiver)

        builder.setView(view)
            .setTitle("Nuova Chat")
            .setNegativeButton("Annulla") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Invia") { dialog, _ ->
                // Prendi il nome selezionato nello spinner e il messaggio
                val selectedReceiverName = spinnerReceiver.selectedItem.toString()
                val messageText = messageEditText.text.toString()

                // Invia il messaggio se ci sono nome del destinatario e testo
                if (selectedReceiverName.isNotEmpty() && messageText.isNotEmpty()) {
                    createChat(selectedReceiverName, messageText)
                }
            }

        return builder.create()
    }

    private fun loadUsers(accountType: String, spinnerReceiver: Spinner) {
        val usersList = mutableListOf<String>()

        if (accountType == "Developer") {
            // Si cerca il task assegnato all'utente loggato
            tasksDatabase.orderByChild("developerId").equalTo(currentUserId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var projectId: String? = null

                        // Si recupera il projectId del task assegnato all'utente loggato
                        for (taskSnapshot in snapshot.children) {
                            val task = taskSnapshot.getValue(TaskModel::class.java)
                            projectId = task?.projectId
                            break
                        }

                        if (projectId != null) {
                            // Si cercano tutti gli altri task con lo stesso projectId
                            tasksDatabase.orderByChild("projectId").equalTo(projectId)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val developersList = mutableListOf<String>()

                                        // Per ogni task trovato, si recupera il developerId
                                        for (taskSnapshot in snapshot.children) {
                                            val task = taskSnapshot.getValue(TaskModel::class.java)
                                            val developerId = task?.developerId
                                            if (developerId != null && developerId != currentUserId) {
                                                // Si recupera il nome del developer
                                                usersDatabase.child(developerId).get()
                                                    .addOnSuccessListener { userSnapshot ->
                                                        val developerName = userSnapshot.child("name").getValue(String::class.java)
                                                        developerName?.let { developersList.add(it) }

                                                        // Si inseriscono i nomi dei developer nello spinner
                                                        if (developersList.size.toLong() == snapshot.childrenCount - 1) {
                                                            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, developersList)
                                                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                                            spinnerReceiver.adapter = adapter
                                                        }
                                                    }
                                            }
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        // Gestisci errore
                                    }
                                })
                        } else {
                            // Gestisci il caso in cui non troviamo alcun task
                            Log.e("CreateChatDialog", "Nessun task trovato per l'utente loggato")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Gestisci errore
                    }
                })
        } else {
            // Gestisci altri tipi di account (Project Manager, Project Leader, ecc.)
            val usersQuery = when (accountType) {
                "Project Manager" -> {
                    usersDatabase.orderByChild("accountType").equalTo("Project Leader")
                }
                "Project Leader" -> {
                    usersDatabase.orderByChild("accountType").equalTo("Developer")
                    usersDatabase.orderByChild("accountType").equalTo("Project Manager")
                }
                else -> {
                    return
                }
            }

            usersQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(UserModel::class.java)
                        if (user != null) {
                            usersList.add(user.name ?: "Sconosciuto")
                        }
                    }

                    // Popola lo spinner con i nomi degli utenti
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, usersList)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerReceiver.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {
                    // Gestisci errore
                }
            })
        }
    }

    // Funzione per creare la chat
    private fun createChat(receiverName: String, messageText: String) {
        // Trova l'ID dell'utente destinatario tramite il nome selezionato
        usersDatabase.orderByChild("name").equalTo(receiverName).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val receiverId = snapshot.children.firstOrNull()?.key

                if (receiverId != null) {
                    // Crea un nuovo ID per la chat
                    val chatId = chatsDatabase.push().key

                    if (chatId != null) {
                        // Recupera il nome dell'utente loggato (chatterOneId)
                        usersDatabase.child(currentUserId).get().addOnSuccessListener { currentUserSnapshot ->
                            val currentUserName = currentUserSnapshot.child("name").getValue(String::class.java) ?: "Sconosciuto"

                            // Recupera il nome dell'utente destinatario (chatterTwoId)
                            usersDatabase.child(receiverId).get().addOnSuccessListener { receiverUserSnapshot ->
                                val receiverUserName = receiverUserSnapshot.child("name").getValue(String::class.java) ?: "Sconosciuto"

                                // Crea l'oggetto della chat con i nomi
                                val newChat = ChatModel(
                                    chatId = chatId,
                                    chatterOneId = currentUserId,
                                    chatterTwoId = receiverId,
                                    chatterOneName = currentUserName,
                                    chatterTwoName = receiverUserName
                                )

                                // Salva la chat nel database
                                chatsDatabase.child(chatId).setValue(newChat)
                                    .addOnSuccessListener {
                                        Log.d("CreateChatDialog", "Chat creata con successo")
                                        // Una volta creata la chat, invia il messaggio
                                        sendMessage(chatId, messageText, receiverId)
                                    }
                                    .addOnFailureListener {
                                        Log.e("CreateChatDialog", "Errore nella creazione della chat", it)
                                    }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Gestisci errore
            }
        })
    }

    // Invia il primo messaggio
    private fun sendMessage(chatId: String, messageText: String, receiverId: String) {
        // Crea un ID univoco per il messaggio
        val messageId = messagesDatabase.push().key

        if (messageId != null) {
            // Crea il messaggio
            val chatMessage = MessageModel(
                messageId = messageId,
                senderId = currentUserId,
                message = messageText,
                receiverId = receiverId,
                chatId = chatId
            )

            // Salva il messaggio nel database
            messagesDatabase.child(messageId).setValue(chatMessage)
                .addOnSuccessListener {
                    Log.d("CreateChatDialog", "Messaggio inviato con successo")
                }
                .addOnFailureListener {
                    Log.e("CreateChatDialog", "Errore nell'invio del messaggio", it)
                }
        }
    }
}

