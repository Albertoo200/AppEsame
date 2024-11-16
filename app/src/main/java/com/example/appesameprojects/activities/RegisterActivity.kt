package com.example.appesameprojects.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.appesameprojects.R
import com.example.appesameprojects.models.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var spinnerAccountType: Spinner

    private lateinit var btnRegister: Button
    private lateinit var btnLogin: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inizializza Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Inizializzazione le View
        etName = findViewById(R.id.name)
        etEmail = findViewById(R.id.email)
        etPassword = findViewById(R.id.password)
        spinnerAccountType = findViewById(R.id.spinner_account_type)
        btnRegister = findViewById(R.id.btn_register)
        btnLogin = findViewById(R.id.btn_login)

        // Popola il dropdown (spinner) dei tipi di account
        val accountTypes = arrayOf("Project Manager", "Project Leader", "Developer")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accountTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAccountType.adapter = adapter

        // Funzione che gestisce il click sul btnRegister
        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val accountType = spinnerAccountType.selectedItem.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa tutti i campi.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Creazione dell'account Firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Ottieni il token FCM e salvalo nel database
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                            if (tokenTask.isSuccessful) {
                                val fcmToken = tokenTask.result
                                val userId = auth.currentUser?.uid ?: ""
                                val user = UserModel(fcmToken, userId, name, email, accountType)
                                user.fcmToken = fcmToken ?: ""

                                // Ottieni il token FCM e salvalo nel database
                                FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                                    if (tokenTask.isSuccessful) {
                                        val fcmToken = tokenTask.result
                                        user.fcmToken = fcmToken ?: ""

                                        // Salva i dati dell'utente nel db
                                        FirebaseDatabase.getInstance().getReference("User")
                                            .child(userId)
                                            .setValue(user)
                                            .addOnCompleteListener { dbTask ->
                                                if (dbTask.isSuccessful) {
                                                    Toast.makeText(
                                                        this,
                                                        "Registrazione completata.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    // Torna alla MainActivity
                                                    val intent =
                                                        Intent(this, MainActivity::class.java)
                                                    startActivity(intent)
                                                    finish()
                                                } else {
                                                    Toast.makeText(
                                                        this,
                                                        "Errore durante il salvataggio dei dati utente.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "Errore nel recupero del token FCM.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    } else {
                        // Errore durante la registrazione
                        Toast.makeText(this, "Registrazione fallita: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Funzione che gestisce il click sul btnLogin
        btnLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}
