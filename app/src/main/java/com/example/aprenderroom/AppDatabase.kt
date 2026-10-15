package com.example.aprenderroom

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.aprenderroom.daos.TareaDao
import com.example.aprenderroom.entities.Tarea

@Database(
    entities = [Tarea::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tareaDao(): TareaDao
}