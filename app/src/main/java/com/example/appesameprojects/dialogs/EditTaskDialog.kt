package com.example.appesameprojects.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import android.widget.EditText
import com.example.appesameprojects.R
import com.example.appesameprojects.models.TaskModel

class EditTaskDialog(
    private val task: TaskModel,
    private val onTaskUpdated: (TaskModel) -> Unit,
    private val onTaskDeleted: (String) -> Unit
) : DialogFragment() {

    private lateinit var taskNameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var developerNameEditText: EditText
    private lateinit var expirationDateEditText: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_edit_task, null)

        // Inizializzazione EditText
        taskNameEditText = view.findViewById(R.id.edit_task_name)
        descriptionEditText = view.findViewById(R.id.edit_task_description)
        developerNameEditText = view.findViewById(R.id.edit_developer)
        expirationDateEditText = view.findViewById(R.id.edit_expiration_date)

        // Riempimento dei campi con i dati del task esistente
        taskNameEditText.setText(task.name)
        descriptionEditText.setText(task.description)
        developerNameEditText.setText(task.developerName)
        expirationDateEditText.setText(task.expirationDate)

        builder.setView(view)
            .setTitle("Modifica Task")
            .setNegativeButton("Annulla") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Salva") { dialog, _ ->
                val updatedTask = task.copy(
                    name = taskNameEditText.text.toString(),
                    description = descriptionEditText.text.toString(),
                    developerName = developerNameEditText.text.toString(),
                    expirationDate = expirationDateEditText.text.toString()
                )
                onTaskUpdated(updatedTask)
            }

        // Pulsante per eliminare il task
        builder.setNeutralButton("Elimina") { dialog, _ ->
            onTaskDeleted(task.taskId)
        }

        return builder.create()
    }
}
