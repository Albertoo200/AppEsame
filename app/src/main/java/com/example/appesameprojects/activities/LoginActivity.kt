package com.example.appesameprojects.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appesameprojects.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inizializzazione Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Inizializzazione le View
        btnLogin = findViewById(R.id.btn_login)
        btnRegister = findViewById(R.id.btn_register)
        etEmail = findViewById(R.id.email)
        etPassword = findViewById(R.id.password)

        // Funzione che gestisce il click sul btnLogin
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email o Password sono vuoti.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Login con Firebase
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Ottieni il token FCM e salvalo nel database
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                            if (tokenTask.isSuccessful) {
                                val fcmToken = tokenTask.result
                                val userId = auth.currentUser?.uid ?: ""
                                if (userId.isNotEmpty() && fcmToken != null) {
                                    // Salva il token FCM nel database
                                    val userRef = FirebaseDatabase.getInstance().getReference("User").child(userId)
                                    userRef.child("fcmToken").setValue(fcmToken)
                                        .addOnCompleteListener {
                                            if (it.isSuccessful) {
                                                // Passa alla MainActivity
                                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                                startActivity(intent)
                                                finish()
                                            } else {
                                                Toast.makeText(this, "Errore nel salvataggio del token.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                }
                            }
                        }
                    } else {
                        // Login fallito
                        Toast.makeText(this, "Autenticazione fallita.", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Funzione che gestisce il click sul btnRegister
        btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // Funzione che gestisce l'apertura dell'app
    override fun onStart() {
        super.onStart()
        // Se l'utente è già loggato, passa alla MainActivity direttamente
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
