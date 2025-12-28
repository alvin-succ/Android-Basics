package com.example.activitytest

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class FirstActivity : AppCompatActivity() {

    // 使用 lazy 延迟初始化
    private val resultLauncher: ActivityResultLauncher<Intent> by lazy{
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ){result->
            when(result.resultCode){
                RESULT_OK->{
                    val data = result.data
                    val name = data?.getStringExtra("name")
                    val age = data?.getIntExtra("age", 0)
                    name?.let {
                        val userInfo = "姓名: $it, 年龄: $age"
                        findViewById<Button>(R.id.button1).text = userInfo
                    }
                }
                RESULT_CANCELED -> {
                    Toast.makeText(this, "用户取消了操作", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.first_layout)
        // 提前访问一次，触发初始化
        resultLauncher

        val button1: Button = findViewById(R.id.button1)


        button1.setOnClickListener {
            val intent = Intent(this, SecondActivity::class.java)
            resultLauncher.launch(intent)
        }
    }



}