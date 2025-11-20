package co.edu.unal.webservice

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val title = intent.getStringExtra("title")
        val description = intent.getStringExtra("description")

        findViewById<TextView>(R.id.titleDetail).text = title ?: "Sin título"
        findViewById<TextView>(R.id.descDetail).text = description ?: "Sin descripción"
    }
}
