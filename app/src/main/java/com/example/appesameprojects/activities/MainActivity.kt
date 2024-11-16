package com.example.appesameprojects.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.example.appesameprojects.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var usersDatabase: DatabaseReference
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        // Inizializza il database
        usersDatabase = FirebaseDatabase.getInstance().getReference("User")

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        navController = findNavController(R.id.nav_host_fragment)

        // Listener per cambiare la visibilità del pulsante "Back" nella Toolbar
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_account) {
                supportActionBar?.setDisplayHomeAsUpEnabled(true) // Mostra il pulsante "Back"
                // Imposta il colore bianco per il pulsante "Back"
                supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back)
            } else {
                supportActionBar?.setDisplayHomeAsUpEnabled(false) // Nasconde il pulsante "Back"
            }
        }

        auth.currentUser?.let { user ->
            usersDatabase.child(user.uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val accountType = dataSnapshot.child("accountType").getValue(String::class.java)
                    setupNavigation(accountType)
                    supportActionBar?.title = accountType // Imposta il titolo
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Errore caricamento dati utente", Toast.LENGTH_SHORT).show()
                }
            })
        } ?: run {
            Toast.makeText(this, "Utente non loggato", Toast.LENGTH_SHORT).show()
        }

        NavigationUI.setupWithNavController(bottomNavigationView, navController)
    }

    private fun setupNavigation(accountType: String?) {
        val bundle = Bundle().apply {
            putString("accountType", accountType) // Passa il tipo di account al fragment
        }
        navController.navigate(R.id.navigation_home, bundle)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> { // Pulsante "Back" nella Toolbar
                if (navController.currentDestination?.id == R.id.navigation_account) {
                    navController.popBackStack() // Torna al Fragment precedente
                    return true
                }
            }
            R.id.btn_account -> { // Pulsante "Account" nella Toolbar
                val bundle = Bundle().apply {
                    val accountType = intent.getStringExtra("accountType")
                    putString("accountType", accountType)
                }
                navController.navigate(R.id.navigation_account, bundle)
                return true
            }
            R.id.btn_logout -> { // Pulsante "Logout" nella Toolbar
                Toast.makeText(this, "Logout...", Toast.LENGTH_SHORT).show()
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
