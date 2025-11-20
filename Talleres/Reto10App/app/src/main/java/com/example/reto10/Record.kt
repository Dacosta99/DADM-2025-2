package com.example.reto10

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Record(
    val title: String,
    val description: String
) : Parcelable
