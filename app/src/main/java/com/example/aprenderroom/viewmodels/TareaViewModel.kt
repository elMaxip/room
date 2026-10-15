package com.example.aprenderroom.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.aprenderroom.TareasApplication
import com.example.aprenderroom.repositories.TareaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TareaViewModel(
    private val repository: TareaRepository
) : ViewModel() {
    val tareas = repository.tareas
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun agregarTarea(titulo: String) {
        viewModelScope.launch {
            repository.agregar(titulo)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TareasApplication
                TareaViewModel(TareaRepository(app.tareaDao))
            }
        }
    }
}
