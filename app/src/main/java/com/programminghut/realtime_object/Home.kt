package com.programminghut.realtime_object

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class Home : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val nextButton = findViewById<Button>(R.id.button)
        nextButton.setOnClickListener {
            // Start the next activity when the button is clicked.
            startActivity(Intent(this, MainActivity::class.java))
        }

    }
}