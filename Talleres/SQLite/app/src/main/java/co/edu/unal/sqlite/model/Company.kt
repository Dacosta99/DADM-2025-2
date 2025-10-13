// -----------------------------
// model/Company.kt
// -----------------------------
package co.edu.unal.sqlite.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Company(
    var id: Int = 0,
    var name: String,
    var url: String,
    var phone: String,
    var email: String,
    var services: String,
    var category: String // "Consultoría", "Desarrollo a la medida", "Fábrica"
) : Parcelable