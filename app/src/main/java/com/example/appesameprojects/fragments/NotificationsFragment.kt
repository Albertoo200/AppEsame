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
import com.example.appesameprojects.adapters.NotificationsAdapter
import com.example.appesameprojects.models.NotificationModel
import com.example.appesameprojects.models.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class NotificationsFragment : Fragment() {

    private lateinit var notificationsDatabase: DatabaseReference
    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var notificationsAdapter: NotificationsAdapter
    private val notificationsList = mutableListOf<NotificationModel>()

    private var accountType: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        // Ottieni il tipo di account dagli argomenti del fragment
        accountType = arguments?.getString("accountType")

        // Inizializzazione RecyclerView
        notificationsRecyclerView = view.findViewById(R.id.notifications_recycler_view)

        // Configurazione layout RecyclerView
        notificationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Inizializzazione dell'adapter
        notificationsAdapter = NotificationsAdapter(
            notificationsList,
            onNotificationClick = { notification ->
                // Eventuale azione per il click sulla notifica
            },
            onCloseNotificationClick = { notification ->
                // Aggiorna isClosed a true nel database
                notificationsDatabase.child(notification.notificationId)
                    .child("closed").setValue("true")
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Notifica chiusa", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Errore nella chiusura della notifica", Toast.LENGTH_SHORT).show()
                    }
            }
        )

        // Imposta l'adapter alla RecyclerView
        notificationsRecyclerView.adapter = notificationsAdapter

        // Inizializza il database per le notifiche
        notificationsDatabase = FirebaseDatabase.getInstance().getReference("Notifications")

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            fetchAccountType(currentUserId) // Recupera accountType dal DB
        } else {
            Toast.makeText(requireContext(), "Utente non loggato.", Toast.LENGTH_SHORT).show()
        }

        // Recupera i dati dal database in base al tipo di account
        displayContentBasedOnAccountType()

        return view
    }

    // Funzione per recuperare il tipo di account dal database Firebase
    private fun fetchAccountType(userId: String) {
        val usersDatabase = FirebaseDatabase.getInstance().getReference("User")

        usersDatabase.child(userId).get().addOnSuccessListener { snapshot ->
            val user = snapshot.getValue(UserModel::class.java)

            if (user != null) {
                accountType = user.accountType
                displayContentBasedOnAccountType() // Recupera e mostra i dati
            } else {
                Toast.makeText(requireContext(), "Utente non trovato.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Errore nel recupero del tipo di account.", Toast.LENGTH_SHORT).show()
        }
    }

    // Funzione che mostra i dati in base al tipo di account
    private fun displayContentBasedOnAccountType() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            when (accountType) {
                "Project Manager" -> {
                    notificationsRecyclerView.visibility = View.VISIBLE
                    retrieveNotificationsForProjectManager(currentUserId)
                }
                "Project Leader" -> {
                    notificationsRecyclerView.visibility = View.VISIBLE
                    retrieveNotificationsForProjectLeader(currentUserId)
                }
                "Developer" -> {
                    notificationsRecyclerView.visibility = View.VISIBLE
                    retrieveNotificationsForDeveloper(currentUserId)
                }
            }
        } else {
            Toast.makeText(requireContext(), "Utente non loggato.", Toast.LENGTH_SHORT).show()
        }
    }

    // Recupero notifiche per Project Manager
    private fun retrieveNotificationsForProjectManager(currentUserId: String) {
        notificationsDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notificationsList.clear()
                for (notificationSnapshot in snapshot.children) {
                    val notification = notificationSnapshot.getValue(NotificationModel::class.java)
                    if (notification != null && notification.receiverId == currentUserId && notification.closed == "false") {
                        notificationsList.add(notification)
                    }
                }
                notificationsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Errore nel recupero delle notifiche.", Toast.LENGTH_SHORT).show()
            }
        })
    }


    // Metodo per recuperare le notifiche per Project Leader
    private fun retrieveNotificationsForProjectLeader(currentUserId: String) {
        notificationsDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notificationsList.clear()
                for (notificationSnapshot in snapshot.children) {
                    val notification = notificationSnapshot.getValue(NotificationModel::class.java)
                    if (notification != null && notification.receiverId == currentUserId && notification.closed == "false") {
                        notificationsList.add(notification)
                    }
                }
                notificationsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Errore nel recupero delle notifiche.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Metodo per recuperare le notifiche per Developer
    private fun retrieveNotificationsForDeveloper(currentUserId: String) {
        notificationsDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notificationsList.clear()
                for (notificationSnapshot in snapshot.children) {
                    val notification = notificationSnapshot.getValue(NotificationModel::class.java)
                    if (notification != null && notification.receiverId == currentUserId && notification.closed == "false") {
                        notificationsList.add(notification)
                    }
                }
                notificationsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Errore nel recupero delle notifiche.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
