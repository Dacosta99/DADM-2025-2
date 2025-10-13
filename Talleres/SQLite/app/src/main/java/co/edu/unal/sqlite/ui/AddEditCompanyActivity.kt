// -----------------------------
// ui/AddEditCompanyActivity.kt
// -----------------------------
package co.edu.unal.sqlite.ui

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import co.edu.unal.sqlite.R
import co.edu.unal.sqlite.db.DatabaseHelper
import co.edu.unal.sqlite.model.Company

class AddEditCompanyActivity : AppCompatActivity() {

    private lateinit var edtName: EditText
    private lateinit var edtUrl: EditText
    private lateinit var edtPhone: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtServices: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnSave: Button

    private lateinit var dbHelper: DatabaseHelper
    private var editingCompanyId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_company)

        dbHelper = DatabaseHelper(this)

        edtName = findViewById(R.id.edtName)
        edtUrl = findViewById(R.id.edtUrl)
        edtPhone = findViewById(R.id.edtPhone)
        edtEmail = findViewById(R.id.edtEmail)
        edtServices = findViewById(R.id.edtServices)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        btnSave = findViewById(R.id.btnSave)

        val categories = arrayOf("Consultoría", "Desarrollo a la medida", "Fábrica de software")
        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        editingCompanyId = intent.getIntExtra(MainActivity.EXTRA_COMPANY_ID, 0)
        if (editingCompanyId != 0) {
            loadCompany(editingCompanyId)
        }

        btnSave.setOnClickListener {
            saveCompany()
        }
    }

    private fun loadCompany(id: Int) {
        val c = dbHelper.getCompanyById(id) ?: return
        edtName.setText(c.name)
        edtUrl.setText(c.url)
        edtPhone.setText(c.phone)
        edtEmail.setText(c.email)
        edtServices.setText(c.services)
        val pos = (spinnerCategory.adapter as ArrayAdapter<String>).getPosition(c.category)
        if (pos >= 0) spinnerCategory.setSelection(pos)
    }

    private fun saveCompany() {
        val name = edtName.text.toString().trim()
        if (name.isEmpty()) {
            edtName.error = "El nombre es obligatorio"
            return
        }
        val url = edtUrl.text.toString().trim()
        val phone = edtPhone.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val services = edtServices.text.toString().trim()
        val category = spinnerCategory.selectedItem as String

        val company = Company(
            id = editingCompanyId,
            name = name,
            url = url,
            phone = phone,
            email = email,
            services = services,
            category = category
        )

        if (editingCompanyId == 0) {
            val newId = dbHelper.insertCompany(company)
            if (newId > 0) {
                Toast.makeText(this, "Empresa agregada", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Error al agregar", Toast.LENGTH_SHORT).show()
            }
        } else {
            val rows = dbHelper.updateCompany(company)
            if (rows > 0) {
                Toast.makeText(this, "Empresa actualizada", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
            }
        }
    }
}