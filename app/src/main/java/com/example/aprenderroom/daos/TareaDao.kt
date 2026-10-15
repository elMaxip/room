package com.example.aprenderroom.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.aprenderroom.entities.Tarea
import kotlinx.coroutines.flow.Flow

@Dao
interface TareaDao {
    @Insert
    suspend fun insertar(tarea: Tarea)

    @Query("SELECT * FROM tareas")
    fun obtenerTodas(): Flow<List<Tarea>>

    @Query("SELECT * FROM tareas WHERE id = :id")
    suspend fun obtenerPorId(id: Int): Tarea?

    @Delete
    suspend fun eliminar(tarea: Tarea)
}