package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.MainActivity

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
    }

    fun startDetectionButtonClick(view: View?) {
        // Start the main activity
        val intent = Intent(this, MainActivity::class.java) // Replace MainActivity with your actual main activity class
        startActivity(intent)
        finish() // Optional: finish the current activity to prevent going back to it
    }
}