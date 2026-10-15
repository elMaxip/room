package com.example.aprenderroom.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aprenderroom.viewmodels.TareaViewModel

@Composable
fun TareasScreen(viewModel: TareaViewModel = viewModel(factory = TareaViewModel.Factory)) {

    val tareas by viewModel.tareas
        .collectAsStateWithLifecycle()

    Column(modifier = Modifier.systemBarsPadding()) {
        Button(
            onClick = { viewModel.agregarTarea("Sacar la basura")}
        ) {
            Text("Agregar")
        }

        LazyColumn {
            items(tareas, key = { it.id }) { tarea ->
                Text(tarea.titulo)
            }
        }
    }
}
