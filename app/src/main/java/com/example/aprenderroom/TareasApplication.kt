package com.example.aprenderroom

import android.app.Application
import androidx.room.Room

class TareasApplication : Application() {

    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "tareas_db"
        ).build()
    }

    val tareaDao by lazy { database.tareaDao() }
}