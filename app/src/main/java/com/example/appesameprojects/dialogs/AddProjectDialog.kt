package com.example.appesameprojects.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.appesameprojects.R
import com.example.appesameprojects.models.ProjectModel
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.auth.FirebaseAuth

class AddProjectDialog : DialogFragment() {

    private lateinit var projectsDatabase: DatabaseReference

    private lateinit var editProjectName: EditText
    private lateinit var editProjectLeader: EditText
    private lateinit var editExpirationDate: EditText

    private lateinit var auth: FirebaseAuth

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_add_project, null)

        auth = FirebaseAuth.getInstance()

        // Inizializzazione EditText
        editProjectName = view.findViewById(R.id.edit_project_name)
        editProjectLeader = view.findViewById(R.id.edit_project_leader)
        editExpirationDate = view.findViewById(R.id.edit_expiration_date)

        // Inizializzazione database
        projectsDatabase = FirebaseDatabase.getInstance().getReference("Projects")

        builder.setView(view)
            .setTitle("Aggiungi Progetto")
            .setNegativeButton("Annulla") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Aggiungi") { dialog, _ ->
                addProject()
            }

        return builder.create()
    }

    // Funzione che crea un nuovo progetto
    private fun addProject() {
        val projectName = editProjectName.text.toString().trim()
        val projectLeaderName = editProjectLeader.text.toString().trim()
        val expirationDate = editExpirationDate.text.toString().trim()

        if (projectName.isNotEmpty() && projectLeaderName.isNotEmpty() && expirationDate.isNotEmpty()) {
            val projectId = projectsDatabase.push().key ?: ""

            val projectManagerId = auth.currentUser?.uid ?: ""

            // Recupera l'id del Project Leader
            val userDatabase = FirebaseDatabase.getInstance().getReference("User")
            val query: Query = userDatabase.orderByChild("name").equalTo(projectLeaderName)

            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Ottiene il primo risultato
                        val projectLeaderId = dataSnapshot.children.first().key ?: ""

                        val project = ProjectModel(
                            projectId = projectId,
                            name = projectName,
                            projectLeaderName = projectLeaderName,
                            expirationDate = expirationDate,
                            projectProgress = "0%",
                            projectLeaderId = projectLeaderId,
                            projectManagerId = projectManagerId
                        )

                        // Salvataggio nel database
                        projectsDatabase.child(projectId).setValue(project).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                dismiss()
                            } else {
                                Toast.makeText(requireContext(), "Errore nel salvataggio del progetto.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Project Leader non trovato.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Errore nel recupero dell'ID del Project Leader.", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(requireContext(), "Compila tutti i campi.", Toast.LENGTH_SHORT).show()
        }
    }
}
