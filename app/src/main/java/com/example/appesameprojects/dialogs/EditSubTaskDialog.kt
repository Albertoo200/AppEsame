package com.example.appesameprojects.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.appesameprojects.R
import com.example.appesameprojects.models.SubTaskModel

class EditSubTaskDialog(
    private val subTask: SubTaskModel,
    private val onSubTaskUpdated: (SubTaskModel) -> Unit,
    private val onSubTaskDeleted: (String) -> Unit
) : DialogFragment() {

    private lateinit var subTaskNameEditText: EditText
    private lateinit var spinnerPriority: Spinner
    private lateinit var spinnerStatus: Spinner
    private lateinit var expirationDateEditText: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_edit_subtask, null)

        // Inizializzazione EditText
        subTaskNameEditText = view.findViewById(R.id.edit_subtask_name)
        spinnerPriority = view.findViewById(R.id.spinner_priority)
        spinnerStatus = view.findViewById(R.id.spinner_status)
        expirationDateEditText = view.findViewById(R.id.edit_expiration_date)

        // Riempimento dei campi con i dati del subtask esistente
        subTaskNameEditText.setText(subTask.name)
        expirationDateEditText.setText(subTask.expirationDate)
        // SpinnerPriority
        val priorityAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.priority_options,
            android.R.layout.simple_spinner_item
        )
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = priorityAdapter
        val priorityPosition = priorityAdapter.getPosition(subTask.priority)
        spinnerPriority.setSelection(priorityPosition)
        // SpinnerStatus
        val statusAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.status_options,
            android.R.layout.simple_spinner_item
        )
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = statusAdapter
        val statusPosition = statusAdapter.getPosition(subTask.status)
        spinnerStatus.setSelection(statusPosition)

        builder.setView(view)
            .setTitle("Modifica Subtask")
            .setNegativeButton("Annulla") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Salva") { dialog, _ ->
                val updatedSubTask = subTask.copy(
                    name = subTaskNameEditText.text.toString(),
                    priority = spinnerPriority.selectedItem.toString(),
                    status = spinnerStatus.selectedItem.toString(),
                    expirationDate = expirationDateEditText.text.toString()
                )
                onSubTaskUpdated(updatedSubTask)
            }

        // Pulsante per eliminare il subtask
        builder.setNeutralButton("Elimina") { dialog, _ ->
            onSubTaskDeleted(subTask.subTaskId)
        }

        return builder.create()
    }
}