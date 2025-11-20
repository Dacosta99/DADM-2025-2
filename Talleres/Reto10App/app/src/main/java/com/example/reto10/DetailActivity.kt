package com.example.reto10

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.widget.TextView

class DetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val record = intent.getParcelableExtra<Record>("record")

        findViewById<TextView>(R.id.titleDetail).text = record?.title ?: "Sin título"
        findViewById<TextView>(R.id.descDetail).text = record?.description ?: "Sin descripción"
    }
}
