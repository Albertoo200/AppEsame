package com.example.appesameprojects.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.appesameprojects.R
import com.example.appesameprojects.fragments.HomeFragment
import com.example.appesameprojects.models.ProjectModel
import com.example.appesameprojects.models.TaskModel
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.auth.FirebaseAuth

class AddTaskDialog : DialogFragment() {

    private lateinit var tasksDatabase: DatabaseReference

    private lateinit var editProjectName: EditText
    private lateinit var editTaskName: EditText
    private lateinit var editTaskDescription: EditText
    private lateinit var editDeveloper: EditText
    private lateinit var editExpirationDate: EditText

    private lateinit var auth: FirebaseAuth

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_add_task, null)

        // Inizializzazione EditText
        editProjectName = view.findViewById(R.id.edit_project_name)
        editTaskName = view.findViewById(R.id.edit_task_name)
        editTaskDescription = view.findViewById(R.id.edit_task_description)
        editDeveloper = view.findViewById(R.id.edit_developer)
        editExpirationDate = view.findViewById(R.id.edit_expiration_date)

        // Inizializza il database
        tasksDatabase = FirebaseDatabase.getInstance().getReference("Tasks")
        auth = FirebaseAuth.getInstance()

        builder.setView(view)
            .setTitle("Aggiungi Task")
            .setNegativeButton("Annulla") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Aggiungi") { dialog, _ -> addTask() }

        return builder.create()
    }

    // Funzione che crea un nuovo task
    private fun addTask() {
        val projectName = editProjectName.text.toString().trim()
        val taskName = editTaskName.text.toString().trim()
        val taskDescription = editTaskDescription.text.toString().trim()
        val developerName = editDeveloper.text.toString().trim()
        val expirationDate = editExpirationDate.text.toString().trim()

        // Verifica che tutti i campi siano compilati
        if (projectName.isNotEmpty() && taskName.isNotEmpty() && taskDescription.isNotEmpty() && developerName.isNotEmpty() && expirationDate.isNotEmpty()) {
            // Recupera l'id del progetto basato sul nome
            val projectRef = FirebaseDatabase.getInstance().getReference("Projects")
            val query: Query = projectRef.orderByChild("name").equalTo(projectName)

            val developerRef = FirebaseDatabase.getInstance().getReference("User")

            // Query per trovare l'id dello sviluppatore basato sul nome
            developerRef.orderByChild("name").equalTo(developerName).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(developerSnapshot: DataSnapshot) {
                    if (developerSnapshot.exists()) {
                        // Ottieni il primo sviluppatore trovato
                        val developerData = developerSnapshot.children.first()
                        val developerId = developerData.key ?: ""

                        // Query per trovare il progetto
                        query.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    // Ottieni il primo progetto trovato
                                    val projectSnapshot = dataSnapshot.children.first()
                                    val projectId = projectSnapshot.key ?: ""
                                    val projectLeaderId = projectSnapshot.child("projectLeaderId").getValue(String::class.java) ?: ""

                                    val taskId = tasksDatabase.push().key ?: ""
                                    val task = TaskModel(
                                        taskId = taskId,
                                        projectName = projectName,
                                        name = taskName,
                                        description = taskDescription,
                                        developerName = developerName,
                                        expirationDate = expirationDate,
                                        taskProgress = "0%",
                                        projectId = projectId,
                                        projectLeaderId = projectLeaderId,
                                        developerId = developerId
                                    )

                                    // Salvataggio nel database
                                    tasksDatabase.child(taskId).setValue(task).addOnCompleteListener { taskResult ->
                                        if (taskResult.isSuccessful) {
                                            dismiss()
                                        } else {
                                            Toast.makeText(requireContext(), "Errore nel salvataggio del task.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(requireContext(), "Progetto non trovato.", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(requireContext(), "Errore nel recupero del progetto.", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {
                        Toast.makeText(requireContext(), "Sviluppatore non trovato.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Errore nel recupero degli sviluppatori.", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(requireContext(), "Compila tutti i campi.", Toast.LENGTH_SHORT).show()
        }
    }
}
