package com.example.aprenderroom.repositories

import com.example.aprenderroom.daos.TareaDao
import com.example.aprenderroom.entities.Tarea

class TareaRepository(
    private val dao: TareaDao
) {
    val tareas = dao.obtenerTodas()

    suspend fun agregar(titulo: String) {
        dao.insertar(Tarea(titulo = titulo))
    }
}