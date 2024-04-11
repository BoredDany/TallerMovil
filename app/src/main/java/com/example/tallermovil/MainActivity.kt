package com.example.tallermovil

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val camBtn = findViewById<ImageButton>(R.id.camBtn)
        val contactsBtn = findViewById<ImageButton>(R.id.contactBtn)
        val mapBtn = findViewById<ImageButton>(R.id.mapBtn)

        camBtn.setOnClickListener {

        }

        contactsBtn.setOnClickListener {
            val intent = Intent(this, ContactsActivity::class.java)
            startActivity(intent)
        }

        mapBtn.setOnClickListener {

        }

    }
}