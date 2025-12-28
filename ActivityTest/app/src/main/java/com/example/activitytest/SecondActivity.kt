package com.example.activitytest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SecondActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_second)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.second)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val extraData = intent.getStringExtra("extra_data")
        Log.d("Data====>", "extra data is $extraData")

        findViewById<Button>(R.id.button2).setOnClickListener {
            val intent = Intent().apply {
                putExtra("name", "jim")
                putExtra("age", 16)
            }

            setResult(RESULT_OK, intent)
            finish()
        }
    }
}