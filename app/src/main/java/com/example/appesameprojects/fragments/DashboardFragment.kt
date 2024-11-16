package com.example.appesameprojects.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appesameprojects.R
import com.example.appesameprojects.adapters.ProjectsAdapter
import com.example.appesameprojects.adapters.SubTasksAdapter
import com.example.appesameprojects.adapters.TasksAdapter
import com.example.appesameprojects.dialogs.AddSubTaskDialog
import com.example.appesameprojects.dialogs.EditTaskDialog
import com.example.appesameprojects.models.NotificationModel
import com.example.appesameprojects.models.ProjectModel
import com.example.appesameprojects.models.SubTaskModel
import com.example.appesameprojects.models.TaskModel
import com.example.appesameprojects.models.UserModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Locale

class DashboardFragment : Fragment() {

    private lateinit var projectsDatabase: DatabaseReference
    private lateinit var projectsRecyclerView: RecyclerView
    private lateinit var projectsAdapter: ProjectsAdapter
    private val projectList = mutableListOf<ProjectModel>()

    private lateinit var tasksDatabase: DatabaseReference
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var tasksAdapter: TasksAdapter
    private val taskList = mutableListOf<TaskModel>()
    private val filteredTaskList = mutableListOf<TaskModel>()


    private lateinit var subTasksDatabase: DatabaseReference
    private lateinit var subTasksRecyclerView: RecyclerView
    private lateinit var subTasksDeveloperRecyclerView: RecyclerView
    private lateinit var subTasksAdapter: SubTasksAdapter
    private val subTaskList = mutableListOf<SubTaskModel>()

    private lateinit var notificationsDatabase: DatabaseReference

    private lateinit var searchTaskView: SearchView

    private lateinit var addButton: FloatingActionButton

    private var accountType: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // Ottenere il tipo di account dagli argomenti del fragment
        accountType = arguments?.getString("accountType")

        // Inizializzazione SearchView
        searchTaskView = view.findViewById(R.id.search_task_view)

        // Inizializzazione AddButton
        addButton = view.findViewById(R.id.add_button)

        // Inizializzazione RecyclerView
        projectsRecyclerView = view.findViewById(R.id.projects_recycler_view)
        tasksRecyclerView = view.findViewById(R.id.tasks_recycler_view)
        subTasksRecyclerView = view.findViewById(R.id.subtasks_recycler_view)
        subTasksDeveloperRecyclerView = view.findViewById(R.id.subtasks_developer_recycler_view)

        // Configurazione layout RecyclerView
        projectsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        tasksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        subTasksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        subTasksDeveloperRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Inizializzazione degli adapter
        projectsAdapter = ProjectsAdapter(projectList, accountType,
            onProjectClick = { project -> onProjectClick(project) },
            onReminderProjectClick = { project -> /* */ },
        )
        tasksAdapter = TasksAdapter(filteredTaskList, accountType = "Project Leader",
            onTaskClick = { task -> onTaskClick(task) },
            onReminderTaskClick = { task -> sendTaskNotification(task) }
        )
        subTasksAdapter = SubTasksAdapter(subTaskList) { subTask -> onSubTaskClick(subTask) }

        // Imposta gli adapter alle RecyclerView
        projectsRecyclerView.adapter = projectsAdapter
        tasksRecyclerView.adapter = tasksAdapter
        subTasksRecyclerView.adapter = subTasksAdapter
        subTasksDeveloperRecyclerView.adapter = subTasksAdapter

        // Inizializza il database
        projectsDatabase = FirebaseDatabase.getInstance().getReference("Projects")
        tasksDatabase = FirebaseDatabase.getInstance().getReference("Tasks")
        subTasksDatabase = FirebaseDatabase.getInstance().getReference("SubTasks")
        notificationsDatabase = FirebaseDatabase.getInstance().getReference("Notifications")

        // Recupera il tipo di account dell'utente loggato
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            fetchAccountType(currentUserId) // Recupera accountType dal DB
        } else {
            Toast.makeText(requireContext(), "Utente non loggato.", Toast.LENGTH_SHORT).show()
        }

        // Aggiungi listener per i task
        addSubTaskListener()

        // Quando addButton viene premuto
        addButton.setOnClickListener {
            addNewEntity()
        }

        // Impostazione della funzionalità di ricerca
        searchTaskView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Non facciamo nulla al submit
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterTasks(newText)
                return true
            }
        })

        return view
    }

    // Funzione di ricerca che mostra i task in base ai filtri
    private fun filterTasks(query: String?) {
        filteredTaskList.clear()

        if (query.isNullOrEmpty()) {
            // Se non c'è testo di ricerca, mostra tutti i progetti
            filteredTaskList.addAll(taskList)
        } else {
            // Altrimenti filtra i task in base ai criteri
            val queryLower = query.toLowerCase(Locale.ROOT)

            for (task in taskList) {
                if (task.developerName?.toLowerCase(Locale.ROOT)?.contains(queryLower) == true ||
                    task.expirationDate?.toLowerCase(Locale.ROOT)?.contains(queryLower) == true ||
                    task.taskProgress?.toLowerCase(Locale.ROOT)?.contains(queryLower) == true) {
                    filteredTaskList.add(task)
                }
            }
        }
        tasksAdapter.notifyDataSetChanged()
    }

    // Funzione per recuperare il tipo di account dal db
    private fun fetchAccountType(userId: String) {
        val usersDatabase = FirebaseDatabase.getInstance().getReference("User")

        usersDatabase.child(userId).get().addOnSuccessListener { snapshot ->
            val user = snapshot.getValue(UserModel::class.java)

            if (user != null) {
                // Assegnazione dell'accountType alla variabile del fragment
                accountType = user.accountType
                displayContentBasedOnAccountType()
            } else {
                Toast.makeText(requireContext(), "Utente non trovato.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Errore nel recupero del tipo di account.", Toast.LENGTH_SHORT).show()
        }
    }

    // Funzione per gestire la visualizzazione dei contenuti in base al tipo di account
    private fun displayContentBasedOnAccountType() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserId != null) {
            when (accountType) {
                "Project Manager" -> {
                    // Mostra projects se l'utente è Project Manager o Project Leader
                    projectsRecyclerView.visibility = View.VISIBLE
                    tasksRecyclerView.visibility = View.GONE
                    subTasksRecyclerView.visibility = View.GONE
                    subTasksDeveloperRecyclerView.visibility = View.GONE
                    searchTaskView.visibility = View.GONE
                    addButton.visibility = View.GONE

                    retrieveProjects(currentUserId)
                }
                "Project Leader" -> {
                    projectsRecyclerView.visibility = View.GONE
                    tasksRecyclerView.visibility = View.VISIBLE
                    subTasksRecyclerView.visibility = View.VISIBLE
                    subTasksDeveloperRecyclerView.visibility = View.GONE
                    searchTaskView.visibility = View.VISIBLE
                    addButton.visibility = View.VISIBLE

                    retrieveTasks(currentUserId)
                    retrieveSubTasks(currentUserId)

                }
                "Developer" -> {
                    projectsRecyclerView.visibility = View.GONE
                    tasksRecyclerView.visibility = View.GONE
                    subTasksDeveloperRecyclerView.visibility = View.VISIBLE
                    searchTaskView.visibility = View.GONE
                    addButton.visibility = View.GONE

                    retrieveSubTasks(currentUserId)
                }
                else -> {
                    projectsRecyclerView.visibility = View.GONE
                    tasksRecyclerView.visibility = View.GONE
                    subTasksRecyclerView.visibility = View.GONE
                    subTasksDeveloperRecyclerView.visibility = View.GONE

                    Toast.makeText(requireContext(), "Tipo di account non riconosciuto.", Toast.LENGTH_SHORT).show()
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
                                if (project.projectManagerId == currentUserId && project.projectProgress == "100%") {
                                    projectList.add(project)
                                }
                            }
                        }
                    }
                }
                projectsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Errore nel recupero dei progetti.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Funzione di recupero dei task per Project Leader
    private fun retrieveTasks(currentUserId: String) {
        tasksDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                taskList.clear()
                for (taskSnapshot in snapshot.children) {
                    val task = taskSnapshot.getValue(TaskModel::class.java)
                    if (task != null && task.projectLeaderId == currentUserId) {
                        taskList.add(task)
                    }
                }
                filterTasks(null)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Errore nel recupero dei task.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Funzione di recupero dei sub-task per Project Leader e Developer
    private fun retrieveSubTasks(currentUserId: String) {
        subTasksDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                subTaskList.clear()
                for (subTaskSnapshot in snapshot.children) {
                    val subTask = subTaskSnapshot.getValue(SubTaskModel::class.java)
                    if (subTask != null) {
                        when (accountType) {
                            "Project Leader" -> {
                                if (subTask.projectLeaderId == currentUserId) {
                                    subTaskList.add(subTask)
                                }
                            }
                            "Developer" -> {
                                if (subTask.developerId == currentUserId) {
                                    subTaskList.add(subTask)
                                }
                            }
                        }
                    }
                }
                subTasksAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Errore nel recupero dei sub-task.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Funzione di gestione del click sul project
    private fun onProjectClick(project: ProjectModel) {
        confirmDeleteProjectWithTasksAndSubTasks(project.projectId, project.name)
    }

    // Funzione che mostra una finestra per confermare l'eliminazione del progetto e i suoi task e subtask
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

    // Funzione per eliminare il progetto e tutti i task e subtask associati
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

    // Funzione di gestione del click sul task
    private fun onTaskClick(task: TaskModel) {
        // Controlla il tipo di account e decide se mostrare o meno il dialogo di modifica
        if (accountType == "Project Leader") {
            showEditTaskDialog(task)
        } else {
            Toast.makeText(requireContext(), "Non hai il permesso di modificare questo progetto.", Toast.LENGTH_SHORT).show()
        }
    }

    // Funzione che apre la l'edit per un task
    private fun showEditTaskDialog(task: TaskModel) {
        val dialog = EditTaskDialog(
            task = task,
            onTaskUpdated = { updatedTask ->
                updateTask(updatedTask)
            },
            onTaskDeleted = { taskId ->
                task.name?.let { confirmDeleteTaskWithSubTasks(taskId, it) }
            }
        )
        dialog.show(childFragmentManager, "EditTaskDialog")
    }

    // Funzione che aggiorna un task
    private fun updateTask(task: TaskModel) {
        // Verifica se è stato inserito un nome per il developer e cerca l'ID corrispondente
        if (!task.developerName.isNullOrEmpty()) {
            getDeveloperIdByName(task.developerName) { developerId ->
                val updatedTask = task.copy(developerId = developerId)
                // Aggiorna il task con l'ID del developer trovato
                saveTaskToDatabase(updatedTask)
            }
        } else {
            saveTaskToDatabase(task) // Salva direttamente se non c'è un developerName
        }
    }

    // Funzione che salva il task aggiornato nel database
    private fun saveTaskToDatabase(task: TaskModel) {
        tasksDatabase.child(task.taskId).setValue(task)
            .addOnSuccessListener {
                // Aggiorna i subtask del task con il nuovo developerName e developerId
                updateSubTasksDeveloperInfo(task)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Errore nell'aggiornamento del task.", Toast.LENGTH_SHORT).show()
            }
    }

    // Funzione che aggiorna i subtask con il nuovo developer
    private fun updateSubTasksDeveloperInfo(task: TaskModel) {
        // Cerca i subtask con lo stesso taskId del task modificato
        subTasksDatabase.orderByChild("taskId").equalTo(task.taskId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Aggiorna ciascun subtask con il developerName e developerId del task
                        for (subTaskSnapshot in snapshot.children) {
                            val subTaskId = subTaskSnapshot.key ?: continue
                            val updatedSubTask = subTaskSnapshot.getValue(SubTaskModel::class.java)?.copy(
                                developerId = task.developerId
                            )

                            // Salva il subtask aggiornato nel database
                            updatedSubTask?.let {
                                subTasksDatabase.child(subTaskId).setValue(it)
                                    .addOnFailureListener {
                                        Toast.makeText(requireContext(), "Errore nell'aggiornamento di un subtask.", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Errore di connessione durante l'aggiornamento dei subtask.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Funzione che recupera il developerName dalla tabella User
    private fun getDeveloperIdByName(developerName: String, callback: (String) -> Unit) {
        val usersDatabase = FirebaseDatabase.getInstance().getReference("User")
        usersDatabase.orderByChild("name").equalTo(developerName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Recupera l'ID del primo utente corrispondente
                        val developerId = snapshot.children.first().key ?: ""
                        callback(developerId)
                    } else {
                        Toast.makeText(requireContext(), "Developer non trovato.", Toast.LENGTH_SHORT).show()
                        callback("") // Ritorna un ID vuoto in caso di mancata corrispondenza
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Errore di connessione.", Toast.LENGTH_SHORT).show()
                    callback("") // Ritorna un ID vuoto in caso di errore
                }
            })
    }

    // Funzione che mostra una finestra per confermare l'eliminazione del task e dei subtask associati
    private fun confirmDeleteTaskWithSubTasks(taskId: String, taskName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Conferma eliminazione")
            .setMessage("Sei sicuro di voler eliminare il task \"$taskName\" e tutti i subtask a lui associati?")
            .setPositiveButton("Sì") { _, _ ->
                deleteTaskWithSubTasks(taskId)
            }
            .setNegativeButton("No", null)
            .show()
    }

    // Funzione che elimina un task e tutti i subtask a lui associati
    private fun deleteTaskWithSubTasks(taskId: String) {
        // Rimozione di tutti i subtasks associati al task
        subTasksDatabase.orderByChild("taskId").equalTo(taskId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (subTaskSnapshot in snapshot.children) {
                        subTaskSnapshot.ref.removeValue()
                    }
                    // Dopo aver rimosso i subtasks, si rimuove il task principale
                    tasksDatabase.child(taskId).removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Task e subtasks eliminati.", Toast.LENGTH_SHORT).show()

                            // Calcola il progresso del progetto dopo l'eliminazione del task
                            val projectId = snapshot.child("projectId").getValue(String::class.java)
                            if (projectId != null) {
                                calculateProjectProgress(projectId)
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Errore nell'eliminazione del task.", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Errore nell'eliminazione dei subtasks.", Toast.LENGTH_SHORT).show()
                }
            })
    }


    // Funzione che ricalcola il projectProgress quando un task viene creato
    private fun addSubTaskListener() {
        subTasksDatabase.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // Quando un nuovo task viene aggiunto, ricalcola il progresso del progetto
                val taskId = snapshot.child("taskId").getValue(String::class.java)
                if (taskId != null) {
                    calculateTaskProgress(taskId)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Funzione che apre la finestra per creare un subtask
    private fun addNewEntity() {
        val addSubTaskDialog = AddSubTaskDialog()
        addSubTaskDialog.show(childFragmentManager, "AddSubTaskDialog")

    }

    // Funzione che gestisce il click su un subtask
    private fun onSubTaskClick(subTask: SubTaskModel) {
        when (accountType) {
            "Project Leader" -> {
                showEditSubTaskDialog(subTask)
            }
            "Developer" -> {
                showEditProgressSubTaskDialog(subTask)
            }
            else -> {
                Toast.makeText(requireContext(), "Non hai il permesso di modificare questo progetto.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Funzione che apre la l'edit per un subtask
    private fun showEditSubTaskDialog(subTask: SubTaskModel) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_subtask, null)

        val subTaskNameEditText = dialogView.findViewById<EditText>(R.id.edit_subtask_name)
        val spinnerPriority = dialogView.findViewById<Spinner>(R.id.spinner_priority)
        val spinnerStatus = dialogView.findViewById<Spinner>(R.id.spinner_status)
        val expirationDateEditText = dialogView.findViewById<EditText>(R.id.edit_expiration_date)

        subTaskNameEditText.setText(subTask.name)
        val priorityAdapter = spinnerPriority.adapter
        for (i in 0 until priorityAdapter.count) {
            if (priorityAdapter.getItem(i) == subTask.priority) {
                spinnerPriority.setSelection(i)
                break
            }
        }
        val statusAdapter = spinnerStatus.adapter
        for (i in 0 until statusAdapter.count) {
            if (statusAdapter.getItem(i) == subTask.status) {
                spinnerStatus.setSelection(i)
                break
            }
        }
        expirationDateEditText.setText(subTask.expirationDate)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Modifica SubTask")
            .setView(dialogView)
            .setPositiveButton("Salva") { _, _ ->
                val updatedName = subTaskNameEditText.text.toString()
                val updatedPriority = spinnerPriority.selectedItem.toString()
                val updatedStatus = spinnerStatus.selectedItem.toString()
                val updatedExpiration = expirationDateEditText.text.toString()
                updateSubTask(subTask.subTaskId, updatedName, updatedPriority, updatedStatus, updatedExpiration)
            }
            .setNegativeButton("Annulla", null)
            .setNeutralButton("Elimina") { _, _ ->
                subTask.name?.let { confirmDeleteSubTask(subTask.subTaskId, it) }
            }
            .create()

        dialog.show()
    }

    // Funzione che gestisce l'aggiornamento di un subtask
    private fun updateSubTask(subTaskId: String, name: String, priority: String, status: String, expirationDate: String) {
        val subTaskUpdates = mapOf(
            "name" to name,
            "priority" to priority,
            "status" to status,
            "expirationDate" to expirationDate
        )
        subTasksDatabase.child(subTaskId).updateChildren(subTaskUpdates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Subtask aggiornato.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Errore nell'aggiornamento del subtask.", Toast.LENGTH_SHORT).show()
            }
    }

    // Funzione che apre la l'edit per il subtaskProgress
    private fun showEditProgressSubTaskDialog(subTask: SubTaskModel) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_progress_subtask, null)
        val subTaskProgressEditText = dialogView.findViewById<EditText>(R.id.edit_subtask_progress)
        subTaskProgressEditText.setText(subTask.subTaskProgress)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Modifica SubTask")
            .setView(dialogView)
            .setPositiveButton("Salva") { _, _ ->
                val updatedProgress = subTaskProgressEditText.text.toString()

                updateSubTaskProgress(subTask.subTaskId, updatedProgress)
            }
            .setNegativeButton("Annulla", null)
            .create()

        dialog.show()
    }

    // Funzione che salva il subTaskProgress modificato
    private fun updateSubTaskProgress(subTaskId: String, updatedProgress: String) {
        val subTaskProgressUpdate = mapOf("subTaskProgress" to updatedProgress)

        subTasksDatabase.child(subTaskId).updateChildren(subTaskProgressUpdate)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Subtask aggiornato.", Toast.LENGTH_SHORT).show()

                // Recupera il subtask dal database per verificare se serve inviare la notifica
                subTasksDatabase.child(subTaskId).get().addOnSuccessListener { snapshot ->
                    val taskId = snapshot.child("taskId").getValue(String::class.java)
                    val isNotified = snapshot.child("notified").getValue(Boolean::class.java) ?: false
                    val progress = updatedProgress.removeSuffix("%").toIntOrNull()

                    if (taskId != null) {
                        calculateTaskProgress(taskId)
                    }

                    // Invia la notifica solo se il progresso è al 100% e la notifica non è stata inviata
                    if (progress == 100 && !isNotified) {
                        // Aggiorna il valore dello stato dello spinner a "completed"
                        subTasksDatabase.child(subTaskId).child("status").setValue("Completed")

                        // Aggiorna isNotified a true e invia la notifica
                        subTasksDatabase.child(subTaskId).child("notified").setValue(true)
                        sendNotificationSubTaskCompleted(snapshot.getValue(SubTaskModel::class.java) ?: return@addOnSuccessListener)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Errore nell'aggiornamento del subtask.", Toast.LENGTH_SHORT).show()
            }
    }

    // Funzione che calcola il taskProgress per ogni subTaskProgress
    private fun calculateTaskProgress(taskId: String) {
        subTasksDatabase.orderByChild("taskId").equalTo(taskId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalProgress = 0
                var subTaskCount = 0

                for (subTaskSnapshot in snapshot.children) {
                    val subTaskProgress = subTaskSnapshot.child("subTaskProgress").getValue(String::class.java)?.removeSuffix("%")?.toIntOrNull()
                    if (subTaskProgress != null) {
                        totalProgress += subTaskProgress
                        subTaskCount++
                    }
                }

                val averageProgress = if (subTaskCount > 0) totalProgress / subTaskCount else 0
                updateTaskProgress(taskId, averageProgress)
            }

            override fun onCancelled(error: DatabaseError) {
                // Gestisci l'errore
            }
        })
    }

    // Funzione che salva il taskProgress modificato
    private fun updateTaskProgress(taskId: String, progress: Int) {
        val taskRef = tasksDatabase.child(taskId)
        taskRef.child("taskProgress").setValue("$progress%").addOnCompleteListener { taskUpdate ->
            if (taskUpdate.isSuccessful) {
                tasksDatabase.child(taskId).get().addOnSuccessListener { snapshot ->
                    val projectId = snapshot.child("projectId").getValue(String::class.java)
                    val isNotified = snapshot.child("notified").getValue(Boolean::class.java) ?: false
                    if (projectId != null) {
                        calculateProjectProgress(projectId)
                    }

                    if (progress == 100 && !isNotified) {
                        // Aggiorna isNotified a true e invia la notifica
                        tasksDatabase.child(taskId).child("notified").setValue(true)
                        sendNotificationTaskCompleted(snapshot.getValue(TaskModel::class.java) ?: return@addOnSuccessListener)
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Errore nell'aggiornamento del progresso del task.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Funzione che calcola il projectProgress per ogni taskProgress
    private fun calculateProjectProgress(projectId: String) {
        tasksDatabase.orderByChild("projectId").equalTo(projectId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalProgress = 0
                var taskCount = 0

                for (taskSnapshot in snapshot.children) {
                    val taskProgress = taskSnapshot.child("taskProgress").getValue(String::class.java)?.removeSuffix("%")?.toIntOrNull()
                    if (taskProgress != null) {
                        totalProgress += taskProgress
                        taskCount++
                    }
                }

                val averageProgress = if (taskCount > 0) totalProgress / taskCount else 0
                updateProjectProgress(projectId, averageProgress)
            }

            override fun onCancelled(error: DatabaseError) {
                // Gestisci l'errore
            }
        })
    }

    // Funzione che salva il projectProgress modificato
    private fun updateProjectProgress(projectId: String, progress: Int) {
        val projectRef = projectsDatabase.child(projectId)
        projectRef.child("projectProgress").setValue("$progress%").addOnCompleteListener { projectUpdate ->
            if (projectUpdate.isSuccessful) {
                projectsDatabase.child(projectId).get().addOnSuccessListener { snapshot ->
                    val isNotified = snapshot.child("notified").getValue(Boolean::class.java) ?: false

                    if (progress == 100 && !isNotified) {
                        // Aggiorna isNotified a true e invia la notifica
                        projectsDatabase.child(projectId).child("notified").setValue(true)
                        sendNotificationProjectCompleted(snapshot.getValue(ProjectModel::class.java) ?: return@addOnSuccessListener)
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Errore nell'aggiornamento del progresso del task.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Funzione che invia una notifica quando subTaskProgress = 100%
    private fun sendNotificationSubTaskCompleted(subTask: SubTaskModel) {
        val projectLeaderId = subTask.projectLeaderId
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserId != null) {
            when (accountType) {
                "Developer" -> {
                    val notificationMessage = "Il subtask '${subTask.name}' è stato completato al 100%."
                    val notificationId = notificationsDatabase.push().key ?: ""

                    val notification = NotificationModel(
                        notificationId = notificationId,
                        message = notificationMessage,
                        receiverId = projectLeaderId,
                        senderId = currentUserId
                    )

                    notificationsDatabase.child(notificationId).setValue(notification)
                        .addOnCompleteListener { subTask ->
                            if (subTask.isSuccessful) {
                                Toast.makeText(requireContext(), "Notifica inviata al Project Leader.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Errore nell'invio della notifica.", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
        }
    }

    // Funzione che invia una notifica quando taskProgress = 100%
    private fun sendNotificationTaskCompleted(task: TaskModel) {
        val projectLeaderId = task.projectLeaderId
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserId != null) {
            when (accountType) {
                "Developer" -> {
                    val notificationMessage = "Il task '${task.name}' è stato completato al 100%."
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
                                Toast.makeText(requireContext(), "Notifica inviata al Project Leader.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Errore nell'invio della notifica.", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
        }
    }

    // Funzione che invia una notifica quando projectProgress = 100%
    private fun sendNotificationProjectCompleted(project: ProjectModel) {
        val projectManagerId = project.projectManagerId
        val projectName = project.name
        val projectLeaderId = project.projectLeaderId
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserId != null) {
            when (accountType) {
                "Developer" -> {
                    val notificationMessage = "Il progetto '$projectName' è stato completato al 100%."
                    val notificationId = notificationsDatabase.push().key ?: ""

                    val notification = NotificationModel(
                        notificationId = notificationId,
                        message = notificationMessage,
                        receiverId = projectManagerId,
                        senderId = projectLeaderId
                    )

                    notificationsDatabase.child(notificationId).setValue(notification)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(requireContext(), "Notifica inviata al Project Manager.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Errore nell'invio della notifica.", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
        } else {
            Toast.makeText(requireContext(), "Utente non loggato.", Toast.LENGTH_SHORT).show()
        }
    }

    // Funzione che mostra una finestra per confermare l'eliminazione di un subtask
    private fun confirmDeleteSubTask(subTaskId: String, subTaskName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Conferma eliminazione")
            .setMessage("Sei sicuro di voler eliminare il subtask \"$subTaskName\"?")
            .setPositiveButton("Sì") { _, _ ->
                deleteSubTask(subTaskId)
            }
            .setNegativeButton("No", null)
            .show()
    }

    // Funzione elimina un subtask
    private fun deleteSubTask(subTaskId: String) {
        // Ottieni l'ID del task associato al subtask
        subTasksDatabase.child(subTaskId).get().addOnSuccessListener { snapshot ->
            val taskId = snapshot.child("taskId").getValue(String::class.java)

            // Elimina il subtask
            subTasksDatabase.child(subTaskId).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Subtask eliminato.", Toast.LENGTH_SHORT).show()

                    // Dopo aver eliminato il subtask, aggiorna il progresso del task
                    if (taskId != null) {
                        calculateTaskProgress(taskId)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Errore nell'eliminazione del subtask.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Funzione che invia una notifica di sollecito
    private fun sendTaskNotification(task: TaskModel) {
        val developerId = task.developerId
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (developerId != null && currentUserId != null) {
            val notificationMessage = "Promemoria: '${task.name}' richiede attenzione!"
            val notificationId = notificationsDatabase.push().key ?: ""

            val notification = NotificationModel(
                notificationId = notificationId,
                message = notificationMessage,
                receiverId = developerId,
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