// -----------------------------
// db/DatabaseHelper.kt
// -----------------------------
package co.edu.unal.sqlite.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import co.edu.unal.sqlite.model.Company

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "software_directory.db"
        private const val TABLE_COMPANIES = "companies"

        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_URL = "url"
        private const val KEY_PHONE = "phone"
        private const val KEY_EMAIL = "email"
        private const val KEY_SERVICES = "services"
        private const val KEY_CATEGORY = "category"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_COMPANIES (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_NAME TEXT NOT NULL,
                $KEY_URL TEXT,
                $KEY_PHONE TEXT,
                $KEY_EMAIL TEXT,
                $KEY_SERVICES TEXT,
                $KEY_CATEGORY TEXT
            );
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COMPANIES")
        onCreate(db)
    }

    // Insertar compañía
    fun insertCompany(company: Company): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_NAME, company.name)
            put(KEY_URL, company.url)
            put(KEY_PHONE, company.phone)
            put(KEY_EMAIL, company.email)
            put(KEY_SERVICES, company.services)
            put(KEY_CATEGORY, company.category)
        }
        val id = db.insert(TABLE_COMPANIES, null, values)
        db.close()
        return id
    }

    // Obtener todas las compañías
    fun getAllCompanies(): List<Company> {
        val list = mutableListOf<Company>()
        val selectQuery = "SELECT * FROM $TABLE_COMPANIES ORDER BY $KEY_NAME COLLATE NOCASE"
        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    list.add(cursorToCompany(it))
                } while (it.moveToNext())
            }
        }
        db.close()
        return list
    }

    // Obtener compañías filtradas por nombre y/o categoría
    fun getFilteredCompanies(nameQuery: String?, category: String?): List<Company> {
        val list = mutableListOf<Company>()
        val args = mutableListOf<String>()
        val where = StringBuilder()

        if (!nameQuery.isNullOrBlank()) {
            where.append("$KEY_NAME LIKE ?")
            args.add("%${nameQuery.trim()}%")
        }
        if (!category.isNullOrBlank() && category != "Todas") {
            if (where.isNotEmpty()) where.append(" AND ")
            where.append("$KEY_CATEGORY = ?")
            args.add(category)
        }

        val db = readableDatabase
        val cursor: Cursor = if (where.isEmpty()) {
            db.rawQuery("SELECT * FROM $TABLE_COMPANIES ORDER BY $KEY_NAME COLLATE NOCASE", null)
        } else {
            db.query(TABLE_COMPANIES, null, where.toString(), args.toTypedArray(), null, null, "$KEY_NAME COLLATE NOCASE")
        }

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    list.add(cursorToCompany(it))
                } while (it.moveToNext())
            }
        }
        db.close()
        return list
    }

    // Obtener compañía por id
    fun getCompanyById(id: Int): Company? {
        val db = readableDatabase
        val cursor = db.query(TABLE_COMPANIES, null, "$KEY_ID = ?", arrayOf(id.toString()), null, null, null)
        var company: Company? = null
        cursor.use {
            if (it.moveToFirst()) {
                company = cursorToCompany(it)
            }
        }
        db.close()
        return company
    }

    // Actualizar compañía
    fun updateCompany(company: Company): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_NAME, company.name)
            put(KEY_URL, company.url)
            put(KEY_PHONE, company.phone)
            put(KEY_EMAIL, company.email)
            put(KEY_SERVICES, company.services)
            put(KEY_CATEGORY, company.category)
        }
        val rows = db.update(TABLE_COMPANIES, values, "$KEY_ID = ?", arrayOf(company.id.toString()))
        db.close()
        return rows
    }

    // Eliminar compañía
    fun deleteCompany(id: Int): Int {
        val db = writableDatabase
        val rows = db.delete(TABLE_COMPANIES, "$KEY_ID = ?", arrayOf(id.toString()))
        db.close()
        return rows
    }

    private fun cursorToCompany(c: Cursor): Company {
        return Company(
            id = c.getInt(c.getColumnIndexOrThrow(KEY_ID)),
            name = c.getString(c.getColumnIndexOrThrow(KEY_NAME)),
            url = c.getString(c.getColumnIndexOrThrow(KEY_URL)) ?: "",
            phone = c.getString(c.getColumnIndexOrThrow(KEY_PHONE)) ?: "",
            email = c.getString(c.getColumnIndexOrThrow(KEY_EMAIL)) ?: "",
            services = c.getString(c.getColumnIndexOrThrow(KEY_SERVICES)) ?: "",
            category = c.getString(c.getColumnIndexOrThrow(KEY_CATEGORY)) ?: ""
        )
    }
}

