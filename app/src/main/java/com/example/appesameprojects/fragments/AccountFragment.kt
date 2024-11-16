package com.example.appesameprojects.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appesameprojects.R
import com.example.appesameprojects.adapters.SkillsAdapter
import com.example.appesameprojects.dialogs.AddSkillDialog
import com.example.appesameprojects.models.SkillModel
import com.example.appesameprojects.models.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AccountFragment : Fragment() {

    private lateinit var usersDatabase: DatabaseReference
    private lateinit var currentUser: UserModel

    private lateinit var skillsDatabase: DatabaseReference
    private lateinit var skillsRecyclerView: RecyclerView
    private lateinit var skillsAdapter: SkillsAdapter
    private val skillsList = mutableListOf<SkillModel>()

    private lateinit var addButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_account, container, false)

        // Ottieni utente corrente
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Inizializzazione AddButton
        addButton = view.findViewById(R.id.add_button)

        // Inizializzazione RecyclerView
        skillsRecyclerView = view.findViewById(R.id.skills_recycler_view)

        // Configurazione layout RecyclerView
        skillsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Inizializzazione adapter
        skillsAdapter = SkillsAdapter(skillsList)

        // Imposta adapter alla RecyclerView
        skillsRecyclerView.adapter = skillsAdapter

        // Inizializza il database
        usersDatabase = FirebaseDatabase.getInstance().getReference("User")
        skillsDatabase = FirebaseDatabase.getInstance().getReference("Skills")

        // Verifica che l'utente sia loggato e richiama il metodo per ottenere i dati utente
        if (currentUserId != null) {
            retrieveUserData(currentUserId, view)
        } else {
            Toast.makeText(requireContext(), "Utente non loggato", Toast.LENGTH_SHORT).show()
        }

        // Quando addButton viene premuto
        addButton.setOnClickListener {
            addSkill()
        }

        return view
    }

    // Funzione che recupera i dati utente dal db
    private fun retrieveUserData(userId: String, view: View) {
        usersDatabase.child(userId).get().addOnSuccessListener { snapshot ->
            val user = snapshot.getValue(UserModel::class.java)

            if (user != null) {
                currentUser = user
                displayUserData(view, user)

                when (user.accountType) {
                    "Project Manager" -> {
                        view.findViewById<LinearLayout>(R.id.tasks_line).visibility = View.GONE
                        retrieveProjectsForProjectManager(userId, view)
                    }
                    "Project Leader" -> {
                        view.findViewById<LinearLayout>(R.id.tasks_line).visibility = View.GONE
                        retrieveProjectsAndTasksForProjectLeader(userId, view)
                    }
                    "Developer" -> {
                        view.findViewById<LinearLayout>(R.id.projects_line).visibility = View.GONE
                        retrieveTasksForDeveloper(userId, view)
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Impossibile caricare i dati utente", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Errore nel recupero dei dati", Toast.LENGTH_SHORT).show()
        }
    }


    // Funzione che mostra i dati utente (aggiornamento TextView)
    private fun displayUserData(view: View, user: UserModel) {
        view.findViewById<TextView>(R.id.user_name).text = user.name ?: "Nome non disponibile"
        view.findViewById<TextView>(R.id.user_email).text = user.email ?: "Email non disponibile"
        view.findViewById<TextView>(R.id.user_account_type).text = user.accountType ?: "Tipo di account non disponibile"
    }

    private fun retrieveProjectsForProjectManager(userId: String, view: View) {
        val projectsDatabase = FirebaseDatabase.getInstance().getReference("Projects")
        val projectNames = mutableListOf<String>()

        projectsDatabase.orderByChild("projectManagerId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (projectSnapshot in snapshot.children) {
                        val projectName = projectSnapshot.child("name").getValue(String::class.java)
                        projectName?.let { projectNames.add(it) }
                    }

                    // Mostra i nomi dei progetti nella TextView
                    val projectsTextView = view.findViewById<TextView>(R.id.user_projects)
                    if (projectNames.isEmpty()) {
                        projectsTextView.text = "Nessun progetto associato"
                    } else {
                        projectsTextView.text = projectNames.joinToString(", ") { it }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Errore nel recupero dei progetti.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun retrieveProjectsAndTasksForProjectLeader(userId: String, view: View) {
        val projectsDatabase = FirebaseDatabase.getInstance().getReference("Projects")
        val tasksDatabase = FirebaseDatabase.getInstance().getReference("Tasks")

        val projectNames = mutableListOf<String>()
        val taskNames = mutableListOf<String>()

        // Recupera i progetti associati
        projectsDatabase.orderByChild("projectLeaderId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (projectSnapshot in snapshot.children) {
                        val projectName = projectSnapshot.child("name").getValue(String::class.java)
                        projectName?.let { projectNames.add(it) }
                    }

                    // Mostra i nomi dei progetti nella TextView
                    val projectsTextView = view.findViewById<TextView>(R.id.user_projects)
                    if (projectNames.isEmpty()) {
                        projectsTextView.text = "Nessun progetto associato"
                    } else {
                        projectsTextView.text = projectNames.joinToString(", ") { it }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Errore nel recupero dei progetti.", Toast.LENGTH_SHORT).show()
                }
            })

        // Recupera i task associati
        tasksDatabase.orderByChild("projectLeaderId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (taskSnapshot in snapshot.children) {
                        val taskName = taskSnapshot.child("name").getValue(String::class.java)
                        taskName?.let { taskNames.add(it) }
                    }

                    // Mostra i nomi dei task nella TextView
                    val tasksTextView = view.findViewById<TextView>(R.id.user_tasks)
                    tasksTextView.text = taskNames.joinToString(", ") { it }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Errore nel recupero dei task.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun retrieveTasksForDeveloper(userId: String, view: View) {
        val tasksDatabase = FirebaseDatabase.getInstance().getReference("Tasks")
        val taskNames = mutableListOf<String>()

        tasksDatabase.orderByChild("developerId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (taskSnapshot in snapshot.children) {
                        val taskName = taskSnapshot.child("name").getValue(String::class.java)
                        taskName?.let { taskNames.add(it) }
                    }

                    // Mostra i nomi dei task nella TextView
                    val tasksTextView = view.findViewById<TextView>(R.id.user_tasks)
                    if (taskNames.isEmpty()) {
                        tasksTextView.text = "Nessun task associato"
                    } else {
                        tasksTextView.text = taskNames.joinToString(", ") { it }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Errore nel recupero dei task.", Toast.LENGTH_SHORT).show()
                }
            })
    }


    // Funzione che recupera le skills dell'utente dal db
    private fun retrieveUserSkills(currentUserId: String) {
        skillsDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                skillsList.clear()
                for (skillSnapshot in snapshot.children) {
                    val skill = skillSnapshot.getValue(SkillModel::class.java)
                    if (skill != null && skill.userId == currentUserId) {
                        skillsList.add(skill)
                    }
                }
                skillsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Errore nel recupero dei task.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Funzione che apre la finestra per creare una skill
    private fun addSkill() {
        val addSkillDialog = AddSkillDialog()
        addSkillDialog.show(childFragmentManager, "AddSkillDialog")
    }
}