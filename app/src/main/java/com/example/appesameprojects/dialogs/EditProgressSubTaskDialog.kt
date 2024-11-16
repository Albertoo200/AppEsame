package com.example.appesameprojects.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.appesameprojects.R
import com.example.appesameprojects.models.SubTaskModel

class EditProgressSubTaskDialog(
    private val subTask: SubTaskModel,
    private val onSubTaskProgressUpdated: (SubTaskModel) -> Unit,
) : DialogFragment() {

    private lateinit var subTaskProgressEditText: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_edit_progress_subtask, null)

        // Inizializzazione EditText
        subTaskProgressEditText = view.findViewById(R.id.edit_subtask_progress)

        // Riempimento dei campi con il subTaskProgress esistente
        subTaskProgressEditText.setText(subTask.subTaskProgress)

        builder.setView(view)
            .setTitle("Modifica Progresso")
            .setNegativeButton("Annulla") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Salva") { dialog, _ ->
                val updatedSubTaskProgress = subTask.copy(
                    subTaskProgress = subTaskProgressEditText.text.toString()
                )
                onSubTaskProgressUpdated(updatedSubTaskProgress)
            }

        return builder.create()
    }
}