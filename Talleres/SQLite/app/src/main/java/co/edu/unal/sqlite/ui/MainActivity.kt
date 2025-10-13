// -----------------------------
// ui/MainActivity.kt
// -----------------------------
package co.edu.unal.sqlite.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.edu.unal.sqlite.R
import co.edu.unal.sqlite.db.DatabaseHelper
import co.edu.unal.sqlite.model.Company

class MainActivity : AppCompatActivity(), CompanyAdapter.OnItemClickListener {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CompanyAdapter
    private lateinit var edtSearch: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnAdd: Button

    companion object {
        const val REQ_ADD = 100
        const val REQ_EDIT = 101
        const val EXTRA_COMPANY_ID = "company_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)

        recyclerView = findViewById(R.id.recyclerCompanies)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CompanyAdapter(mutableListOf(), this)
        recyclerView.adapter = adapter

        edtSearch = findViewById(R.id.edtSearch)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        btnAdd = findViewById(R.id.btnAdd)

        // spinner categories
        val categories = arrayOf("Todas", "Consultoría", "Desarrollo a la medida", "Fábrica de software")
        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        btnAdd.setOnClickListener {
            val intent = Intent(this, AddEditCompanyActivity::class.java)
            startActivityForResult(intent, REQ_ADD)
        }

        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { loadCompanies() }
            override fun afterTextChanged(s: Editable?) {}
        })

        spinnerCategory.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                loadCompanies()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        })

        loadCompanies()
    }

    private fun loadCompanies() {
        val nameQuery = edtSearch.text.toString()
        val category = spinnerCategory.selectedItem as String
        val companies = dbHelper.getFilteredCompanies(nameQuery, category)
        adapter.updateData(companies)
    }

    override fun onItemClick(company: Company) {
        val intent = Intent(this, AddEditCompanyActivity::class.java)
        intent.putExtra(EXTRA_COMPANY_ID, company.id)
        startActivityForResult(intent, REQ_EDIT)
    }

    override fun onItemLongClick(company: Company) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar empresa")
            .setMessage("¿Desea eliminar la empresa '${company.name}'?")
            .setPositiveButton("Sí") { _, _ ->
                dbHelper.deleteCompany(company.id)
                loadCompanies()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            loadCompanies()
        }
    }
}