package com.example.appesameprojects.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.appesameprojects.R
import com.example.appesameprojects.models.SubTaskModel
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

class AddSubTaskDialog() : DialogFragment() {

    private lateinit var subTasksDatabase: DatabaseReference

    private lateinit var editTaskName: EditText
    private lateinit var editSubTaskName: EditText
    private lateinit var spinnerPriority: Spinner
    private lateinit var spinnerStatus: Spinner
    private lateinit var editExpirationDate: EditText

    private lateinit var auth: FirebaseAuth

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_add_subtask, null)

        auth = FirebaseAuth.getInstance()

        // Inizializzazione EditText
        editTaskName = view.findViewById(R.id.edit_task_name)
        editSubTaskName = view.findViewById(R.id.edit_subtask_name)
        spinnerPriority = view.findViewById(R.id.spinner_priority)
        spinnerStatus = view.findViewById(R.id.spinner_status)
        editExpirationDate = view.findViewById(R.id.edit_expiration_date)

        // Inizializza il database
        subTasksDatabase = FirebaseDatabase.getInstance().getReference("SubTasks")

        builder.setView(view)
            .setTitle("Aggiungi SubTask")
            .setNegativeButton("Annulla") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Aggiungi") { dialog, _ ->
                addSubTask()
            }

        return builder.create()
    }

    // Funzione che crea un nuovo subtask
    private fun addSubTask() {
        val taskName = editTaskName.text.toString().trim()
        val subTaskName = editSubTaskName.text.toString().trim()
        val priority = spinnerPriority.selectedItem.toString()
        val status = spinnerStatus.selectedItem.toString()
        val expirationDate = editExpirationDate.text.toString().trim()

        if (subTaskName.isNotEmpty() && priority.isNotEmpty() && status.isNotEmpty() && expirationDate.isNotEmpty()) {
            // Recupera l'id del progetto basato sul nome
            val taskRef = FirebaseDatabase.getInstance().getReference("Tasks")
            val query: Query = taskRef.orderByChild("name").equalTo(taskName)

            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Ottieni il primo task trovato
                        val taskSnapshot = dataSnapshot.children.first()
                        val taskId = taskSnapshot.key ?: ""
                        val projectId =
                            taskSnapshot.child("projectId").getValue(String::class.java)
                                ?: ""
                        val projectName =
                            taskSnapshot.child("projectName").getValue(String::class.java)
                                ?: ""
                        val projectLeaderId =
                            taskSnapshot.child("projectLeaderId").getValue(String::class.java)
                                ?: ""
                        val developerId =
                            taskSnapshot.child("developerId").getValue(String::class.java)
                                ?: ""

                        val subTaskId = subTasksDatabase.push().key ?: ""
                        val subTask = SubTaskModel(
                            subTaskId = subTaskId,
                            taskName = taskName,
                            name = subTaskName,
                            priority = priority,
                            status = status,
                            expirationDate = expirationDate,
                            subTaskProgress = "0%",  // Inizializza il progresso del task
                            taskId = taskId,
                            projectId = projectId,
                            projectName = projectName,
                            projectLeaderId = projectLeaderId,
                            developerId = developerId
                        )

                        // Salvataggio nel database
                        subTasksDatabase.child(subTaskId).setValue(subTask)
                            .addOnCompleteListener { subTaskResult ->
                                if (subTaskResult.isSuccessful) {
                                    dismiss() // Chiudi il dialogo
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        "Errore nel salvataggio del subtask.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        Toast.makeText(requireContext(), "Task non trovato.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Errore nel recupero del task.", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(requireContext(), "Compila tutti i campi.", Toast.LENGTH_SHORT).show()
        }
    }
}
