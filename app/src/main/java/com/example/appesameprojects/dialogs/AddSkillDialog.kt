package com.example.appesameprojects.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.appesameprojects.R
import com.example.appesameprojects.models.ProjectModel
import com.example.appesameprojects.models.SkillModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

class AddSkillDialog : DialogFragment() {

    private lateinit var skillsDatabase: DatabaseReference

    private lateinit var editSkillText: EditText

    private lateinit var auth: FirebaseAuth

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_add_skill, null)

        auth = FirebaseAuth.getInstance()

        // Inizializzazione EditText
        editSkillText = view.findViewById(R.id.edit_skill_text)

        // Inizializza il database
        skillsDatabase = FirebaseDatabase.getInstance().getReference("Skills")

        builder.setView(view)
            .setTitle("Aggiungi Skill")
            .setNegativeButton("Annulla") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Aggiungi") { dialog, _ ->
                addSkill()
            }

        return builder.create()
    }

    // Funzione che crea una nuova skill
    private fun addSkill() {
        val skillText = editSkillText.text.toString().trim()

        if (skillText.isNotEmpty()) {
            val skillId = skillsDatabase.push().key ?: ""
            val userId = auth.currentUser?.uid ?: ""

            val skill = SkillModel(
                skillId = skillId,
                text = skillText,
                userId = userId
            )

            // Salvataggio nel database
            skillsDatabase.child(skillId).setValue(skill).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "Errore nel salvataggio della skill.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "Compila il campo.", Toast.LENGTH_SHORT).show()
        }
    }
}