package com.example.appesameprojects.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.appesameprojects.R
import com.example.appesameprojects.models.ProjectModel

class EditProjectDialog(
    private val project: ProjectModel,
    private val onProjectUpdated: (ProjectModel) -> Unit,
    private val onProjectDeleted: (String) -> Unit
) : DialogFragment() {

    private lateinit var projectNameEditText: EditText
    private lateinit var projectLeaderNameEditText: EditText
    private lateinit var expirationDateEditText: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_edit_project, null)

        // Inizializzazione EditText
        projectNameEditText = view.findViewById(R.id.edit_project_name)
        projectLeaderNameEditText = view.findViewById(R.id.edit_project_leader)
        expirationDateEditText = view.findViewById(R.id.edit_expiration_date)

        // Riempimento dei campi con i dati del progetto esistente
        projectNameEditText.setText(project.name)
        projectLeaderNameEditText.setText(project.projectLeaderName)
        expirationDateEditText.setText(project.expirationDate)

        builder.setView(view)
            .setTitle("Modifica Progetto")
            .setNegativeButton("Annulla") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Salva") { dialog, _ ->
                val updatedProject = project.copy(
                    name = projectNameEditText.text.toString(),
                    projectLeaderName = projectLeaderNameEditText.text.toString(),
                    expirationDate = expirationDateEditText.text.toString()
                )
                onProjectUpdated(updatedProject)
            }

        // Pulsante per eliminare il project
        builder.setNeutralButton("Elimina") { dialog, _ ->
            onProjectDeleted(project.projectId)
        }

        return builder.create()
    }
}
