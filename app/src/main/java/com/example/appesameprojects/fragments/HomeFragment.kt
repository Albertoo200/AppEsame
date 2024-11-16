package com.example.appesameprojects.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appesameprojects.R
import com.example.appesameprojects.adapters.ProjectsAdapter
import com.example.appesameprojects.adapters.TasksAdapter
import com.example.appesameprojects.dialogs.AddProjectDialog
import com.example.appesameprojects.dialogs.AddTaskDialog
import com.example.appesameprojects.models.NotificationModel
import com.example.appesameprojects.models.ProjectModel
import com.example.appesameprojects.models.TaskModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var projectsDatabase: DatabaseReference
    private lateinit var projectsRecyclerView: RecyclerView
    private lateinit var projectsAdapter: ProjectsAdapter
    private val projectList = mutableListOf<ProjectModel>()
    private val filteredProjectList = mutableListOf<ProjectModel>()

    private lateinit var tasksDatabase: DatabaseReference
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var tasksAdapter: TasksAdapter
    private val taskList = mutableListOf<TaskModel>()

    private lateinit var subTasksDatabase: DatabaseReference
    private lateinit var notificationsDatabase: DatabaseReference

    private lateinit var searchView: SearchView
    private lateinit var addButton: FloatingActionButton

    private var accountType: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Ottieni il tipo di account dagli argomenti del fragment
        accountType = arguments?.getString("accountType")

        // Inizializzazione SearchView
        searchView = view.findViewById(R.id.search_view)

        // Inizializzazione AddButton
        addButton = view.findViewById(R.id.add_button)

        // Inizializzazione RecyclerView
        projectsRecyclerView = view.findViewById(R.id.projects_recycler_view)
        tasksRecyclerView = view.findViewById(R.id.tasks_recycler_view)

        // Configurazione layout RecyclerView
        projectsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        tasksRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Inizializzazione degli adapter
        projectsAdapter = ProjectsAdapter(projectList, accountType,
            onProjectClick = { project -> onProjectClick(project) },
            onReminderProjectClick = { project -> sendProjectNotification(project) },
        )
        tasksAdapter = TasksAdapter(taskList, accountType,
            onTaskClick = { task -> onTaskClick(task) },
            onReminderTaskClick = { task -> /* */ }
        )

        // Imposta gli adapter alle RecyclerView
        projectsRecyclerView.adapter = projectsAdapter
        tasksRecyclerView.adapter = tasksAdapter

        // Inizializza il database
        projectsDatabase = FirebaseDatabase.getInstance().getReference("Projects")
        tasksDatabase = FirebaseDatabase.getInstance().getReference("Tasks")
        subTasksDatabase = FirebaseDatabase.getInstance().getReference("SubTasks")
        notificationsDatabase = FirebaseDatabase.getInstance().getReference("Notifications")


        // Recupera i dati dal database in base al tipo di account
        displayContentBasedOnAccountType()

        // Aggiungi listener per i task
        addTaskListener()

        // Quando addButton viene premuto
        addButton.setOnClickListener {
            addNewEntity()
        }

        // Impostazione della funzionalità di ricerca
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Non facciamo nulla al submit
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterProjects(newText)
                return true
            }
        })

        return view
    }

    // Funzione di ricerca che mostra i projects in base ai filtri
    private fun filterProjects(query: String?) {
        filteredProjectList.clear()

        if (query.isNullOrEmpty()) {
            // Se non c'è testo di ricerca, mostra tutti i progetti
            filteredProjectList.addAll(projectList)
        } else {
            // Altrimenti filtra i progetti in base ai criteri
            val queryLower = query.toLowerCase(Locale.ROOT)

            for (project in projectList) {
                if (project.projectLeaderName?.toLowerCase(Locale.ROOT)?.contains(queryLower) == true ||
                    project.expirationDate?.toLowerCase(Locale.ROOT)?.contains(queryLower) == true ||
                    project.projectProgress?.toLowerCase(Locale.ROOT)?.contains(queryLower) == true) {
                    filteredProjectList.add(project)
                }
            }
        }
        projectsAdapter.notifyDataSetChanged()
    }

    // Funzione per gestire la visualizzazione dei contenuti in base al tipo di account
    private fun displayContentBasedOnAccountType() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserId != null) {
            when (accountType) {
                "Project Manager" -> {
                    searchView.visibility = View.VISIBLE
                    projectsRecyclerView.visibility = View.VISIBLE
                    tasksRecyclerView.visibility = View.GONE
                    addButton.visibility = View.VISIBLE
                    retrieveProjects(currentUserId)

                }
                "Project Leader" -> {
                    searchView.visibility = View.VISIBLE
                    projectsRecyclerView.visibility = View.VISIBLE
                    tasksRecyclerView.visibility = View.GONE
                    addButton.visibility = View.VISIBLE
                    retrieveProjects(currentUserId)
                }
                "Developer" -> {
                    searchView.visibility = View.GONE
                    projectsRecyclerView.visibility = View.GONE
                    tasksRecyclerView.visibility = View.VISIBLE
                    addButton.visibility = View.GONE
                    retrieveTasks(currentUserId)
                }
            }
        } else {
            Toast.makeText(requireContext(), "Utente non loggato.", Toast.LENGTH_SHORT).show()
        }
    }

    // Funzione di recupero projects per Project Manager e Project Leader
    private fun retrieveProjects(currentUserId: String) {
        projectsDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                projectList.clear()
                for (projectSnapshot in snapshot.children) {
                    val project = projectSnapshot.getValue(ProjectModel::class.java)
                    if (project != null) {
                        when (accountType) {
                            "Project Manager" -> {
                                if (project.projectManagerId == currentUserId && project.projectProgress != "100%") {
                                    projectList.add(project)
                                }
                            }
                            "Project Leader" -> {
                                if (project.projectLeaderId == currentUserId) {
                                    projectList.add(project)
                                }
                            }
                        }
                    }
                }
                filterProjects(null)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Errore nel recupero dei progetti.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Funzione che ricalcola il projectProgress quando un task viene creato
    private fun addTaskListener() {
        tasksDatabase.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // Quando un nuovo task viene aggiunto, ricalcola il progresso del progetto
                val projectId = snapshot.child("projectId").getValue(String::class.java)
                if (projectId != null) {
                    calculateProjectProgress(projectId)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Funzione che calcola il projectProgress per ogni taskProgress
    private fun calculateProjectProgress(projectId: String) {
        tasksDatabase.orderByChild("projectId").equalTo(projectId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalProgress = 0
                var taskCount = 0

                // Calcola la media del progresso dei task
                for (taskSnapshot in snapshot.children) {
                    val taskProgress = taskSnapshot.child("taskProgress").getValue(String::class.java)?.removeSuffix("%")?.toIntOrNull()
                    if (taskProgress != null) {
                        totalProgress += taskProgress
                        taskCount++
                    }
                }

                val averageProgress = if (taskCount > 0) {
                    totalProgress / taskCount
                } else {
                    0 // No tasks, set progress to 0
                }

                // Aggiorna il progresso del progetto
                updateProjectProgress(projectId, averageProgress)
            }

            override fun onCancelled(error: DatabaseError) {
                // Gestisci errori
            }
        })
    }

    // Funzione che salva il projectProgress modificato
    private fun updateProjectProgress(projectId: String, progress: Int) {
        val projectRef = projectsDatabase.child(projectId)
        projectRef.child("projectProgress").setValue("$progress%").addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Aggiornamento del progresso completato con successo
            } else {
                // Gestisci l'errore
            }
        }
    }

    // Funzione di recupero dei task per Developer
    private fun retrieveTasks(currentUserId: String) {
        tasksDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                taskList.clear()
                for (taskSnapshot in snapshot.children) {
                    val task = taskSnapshot.getValue(TaskModel::class.java)
                    if (task != null && task.developerId == currentUserId) {
                        taskList.add(task)
                    }
                }
                tasksAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Errore nel recupero dei task.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Funzione che gestisce il bottone "+" per  Project Manager e Project Leader
    private fun addNewEntity() {
        // Controlla il tipo di account e apri il dialogo appropriato
        when (accountType) {
            "Project Manager" -> {
                val addProjectDialog = AddProjectDialog()
                addProjectDialog.show(childFragmentManager, "AddProjectDialog")
            }
            "Project Leader" -> {
                val addTaskDialog = AddTaskDialog()
                addTaskDialog.show(childFragmentManager, "AddTaskDialog")
            }
            else -> {
                Toast.makeText(requireContext(), "Non puoi aggiungere elementi.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Funzione di gestione del click sul project
    private fun onProjectClick(project: ProjectModel) {
        // Controlla il tipo di account e decide se mostrare o meno il dialogo di modifica
        if (accountType == "Project Manager") {
            showEditProjectDialog(project)
        } else {
            // Per i Project Leader, non fare nulla
            Toast.makeText(requireContext(), "Non hai il permesso di modificare questo progetto.", Toast.LENGTH_SHORT).show()
        }
    }

    // Funzione che apre la l'edit per un project
    private fun showEditProjectDialog(project: ProjectModel) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_project, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.edit_project_name)
        val leaderEditText = dialogView.findViewById<EditText>(R.id.edit_project_leader)
        val expirationEditText = dialogView.findViewById<EditText>(R.id.edit_expiration_date)

        nameEditText.setText(project.name)
        leaderEditText.setText(project.projectLeaderName)
        expirationEditText.setText(project.expirationDate)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Modifica Progetto")
            .setView(dialogView)
            .setPositiveButton("Salva") { _, _ ->
                val updatedName = nameEditText.text.toString()
                val updatedLeader = leaderEditText.text.toString()
                val updatedExpiration = expirationEditText.text.toString()
                updateProject(project.projectId, updatedName, updatedLeader, updatedExpiration)
            }
            .setNegativeButton("Annulla", null)
            .setNeutralButton("Elimina") { _, _ ->
                confirmDeleteProjectWithTasksAndSubTasks(project.projectId, project.name)
            }
            .create()

        dialog.show()
    }

    // Funzione che mostra una finestra per confermare l'eliminazione del project, dei task e subtask associati
    private fun confirmDeleteProjectWithTasksAndSubTasks(projectId: String, projectName: String?) {
        AlertDialog.Builder(requireContext())
            .setTitle("Conferma eliminazione progetto")
            .setMessage("Sei sicuro di voler eliminare il progetto \"$projectName\" e tutti i task e subtask associati?")
            .setPositiveButton("Sì") { _, _ ->
                deleteProjectWithTasksAndSubTasks(projectId)
            }
            .setNegativeButton("No", null)
            .show()
    }

    // Funzione che elimina un project, i task e subtask associati
    private fun deleteProjectWithTasksAndSubTasks(projectId: String) {
        // Rimozione dei subtask associati
        subTasksDatabase.orderByChild("projectId").equalTo(projectId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(subTaskSnapshot: DataSnapshot) {
                    for (subTask in subTaskSnapshot.children) {
                        subTask.ref.removeValue()  // Rimuove ogni subtask
                    }
                    // Una volta rimossi i subtask, procediamo con la rimozione dei task
                    tasksDatabase.orderByChild("projectId").equalTo(projectId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(taskSnapshot: DataSnapshot) {
                                for (task in taskSnapshot.children) {
                                    task.ref.removeValue()  // Rimuove ogni task
                                }
                                // Una volta rimossi i task, rimuoviamo il progetto principale
                                projectsDatabase.child(projectId).removeValue()
                                    .addOnSuccessListener {
                                        Toast.makeText(requireContext(), "Progetto e i relativi task e subtask eliminati.", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(requireContext(), "Errore nell'eliminazione del progetto.", Toast.LENGTH_SHORT).show()
                                    }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(requireContext(), "Errore nell'eliminazione dei task.", Toast.LENGTH_SHORT).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Errore nell'eliminazione dei subtask.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Funzione che aggiorna il project
    private fun updateProject(projectId: String, name: String, leader: String, expirationDate: String) {
        val projectUpdates = mapOf(
            "name" to name,
            "projectLeaderName" to leader,
            "expirationDate" to expirationDate
        )
        projectsDatabase.child(projectId).updateChildren(projectUpdates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Progetto aggiornato.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Errore nell'aggiornamento del progetto.", Toast.LENGTH_SHORT).show()
            }
    }

    // Funzione che gestisce il click su un task
    private fun onTaskClick(task: TaskModel) {
        Toast.makeText(requireContext(), "Non hai il permesso di modificare questo task.", Toast.LENGTH_SHORT).show()
    }

    // Funzione che invia una notifica di sollecito
    private fun sendProjectNotification(project: ProjectModel) {
        val projectLeaderId = project.projectLeaderId
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (projectLeaderId != null && currentUserId != null) {
            val notificationMessage = "Promemoria: '${project.name}' richiede attenzione!"
            val notificationId = notificationsDatabase.push().key ?: ""

            val notification = NotificationModel(
                notificationId = notificationId,
                message = notificationMessage,
                receiverId = projectLeaderId,
                senderId = currentUserId
            )

            notificationsDatabase.child(notificationId).setValue(notification)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Notifica inviata.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Errore nell'invio della notifica.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}

