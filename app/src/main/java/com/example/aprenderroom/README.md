# Room

Es una biblioteca que facilita usar bases de datos SQLite.

## Entity

Es la definición de una tabla para la base de datos

````kotlin
@Entity(tableName = "tareas")
data class Tarea(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val titulo: String,
    val completada: Boolean = false
)
````

## DAO

Es una interfaz que define qué operaciones podemos realizar sobre la base de datos.

````kotlin
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
````

- **@Dao**: Le indica a Room que esta interfaz contiene operaciones de acceso a datos
- **@Insert**: Room genera automáticamente la consulta SQL necesaria para insertar una fila
- **@Query**: Nos permite escribir nuestras propias consultas SQL

## DataBase

Sirve para conectar nuestras entidades con los DAO.

````kotlin
@Database(
    entities = [Tarea::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tareaDao(): TareaDao
}
````

- entities: indica qué entidades pertenecen a esta base de datos.
- version: versión del esquema. Es fundamental cuando posteriormente modifiquemos las tablas.
- exportSchema: permite exportar el historial del esquema, útil para migraciones.
- RoomDatabase: clase base que proporciona la infraestructura de Room.

Es una abstract class porque Room se encarga de generar una implementación durante la compilación.
Lo mismo para la función.

## Instanciar base de datos

Código de ejemplo:

````kotlin
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
````

### Application

Al abrir cada aplicación de android, lo primero que se crea es una "Application", que es una clase
que representa la aplicación completa, no a una pantalla. Es único, solo existe una por aplicación
y vive mientras el proceso de la app viva. Por eso es el lugar ideal para guardar cosas que deben
existir una sola vez y ser compartidas como la base de datos.

Heredar de "Application" significa que la clase hará todo lo que una Application normal y además
puede tener sus propias propiedades.

Solo puede haber un Application por app. Para que Android utilice la que creamos, hay que declararlo
en el manifest. Eso hará que Android no cree el Application genérico y cree el que nosotros le
dijimos.

### val database

Crea la base de datos Room. Abrir una base de datos es costoso y no conviene tener dos abiertas a 
la vez. Como TareasApplication es única, la base de datos también lo será.

### tareaDao

Da acceso al DAO

### by lazy

lazy significa "no lo crees ahora, créalo la primera vez que alguien lo use". Es importante porque
las propiedades normales se crean en cuanto se construye el objeto. En ese momento Android todavía
no le ha dado el applicationContext a la Application, y Room lo necesita. Por eso la app podía
crashear. Con lazy, la base de datos se crea más tarde, cuando el viewmodel la pide y el Context ya
existe.

## Repository

El repository (repositorio) no es una obligación, pero es lo que recomienda para que la app crezca.
Es una capa entre el viewmodel y la fuente de datos. La idea es que el viewmodel no sepa de donde
vienen los datos.

En una app pequeña es innecesario usarlo y el viewmodel puede hablar directamente con el DAO. Como
dije, se usa porque deja la app preparada para crecer. Por ejemplo, combinar varias fuentes de datos:

````kotlin
class TareaRepository(
    private val dao: TareaDao,
    private val api: TareaApi
) {
    val tareas = dao.obtenerTodas()

    suspend fun agregar(titulo: String) {
        val tarea = Tarea(titulo = titulo)
        dao.insertar(tarea)       // guarda local
        api.subirTarea(tarea)     // y en internet
    }
}
````

Ayuda a separar responsabilidades:
- ViewModel: maneja el estado de la pantalla
- Repository: decide como obtener y guardar datos
- DAO: ejecuta las consultas SQL

Así cada uno tiene una responsabilidad clara y no se mezclan. También permite centralizar la lógica
de datos en un solo lugar, si quiero validar que un string no esté vacío, lo hago en el repository, y
así si tengo 3 view models que usan el repository, no tengo que repetir lógica. Y por último facilita
hacer pruebas.

## ViewModel

````kotlin
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
````

### viewModelScope

Es un espacio para correr corrutinas que se cancela solo cuando el view model se destruye de verdad 
(al salir de la pantalla, no al rotar)

### agregarTarea(...)

Estamos lanzando una corrutina y dentro insertamos en la base de datos. Es para no bloquear la pantalla.

### stateIn

El DAO devuelve un Flow. Es un flujo que emite una lista de valores nueva cada vez que la tabla cambia.
Un Flow tiene dos problemas para la UI:
- No tiene valor actual. No puedes preguntarle cuál es el valor de la lista ahora, sin esperar.
- Es frío. Cada vez que alguien lo escucha, empieza una consulta nueva desde cero

**stateIn** lo convierte en un StateFlow, que siempre tiene valor actual y se comparte entre todos
los que lo escuchen.

**scope = viewModelScope**: la recolección vive mientras viva el viewmodel
**initialValue = emptyList()**: valor que tiene antes de que Room responda la primera vez, para que
la pantalla tenga algo que mostrar de inmediato
**started = WhileSubscribed(5000)**: solo escucha a Room mientras alguien está mirando la pantalla.
Si nadie mira, espera 5 segundos y deja de escuchar.

### Sin stateIn

Se puede hacer lo mismo sin stateIn:

````kotlin
class TareaViewModel(private val repository: TareaRepository) : ViewModel() {

    private val _tareas = MutableStateFlow<List<Tarea>>(emptyList())
    val tareas: StateFlow<List<Tarea>> = _tareas.asStateFlow()

    init {
        viewModelScope.launch {
            repository.tareas.collect { lista ->
                _tareas.value = lista
            }
        }
    }
}
````

En el "init" empezamos a escuchar los valores que nos trae el repository y actualizamos el MutableStateFlow.
Si te fijas, con stateIn es mucho más limpio, no necesitas dos variables, tampoco init ni launch,
solo escucha cuando alguien lo mira (por eficiencia), y es de solo lectura.

No quiero decir que dejemos de usar MutableStateFlow y empecemos a usar stateIn como locos, el
stateIn es mejor usarlo cuando los datos vienen de otra fuente (Room, un Flow de internet, etc.).
Un estado normal, que el viewmodel cambia por si mismo (texto de un campo, un dialogo, filtro, etc.
y no usa un repository) es mejor usar MutableStateFlow. Ejemplo correcto de uso de MutableStateFlow:

````kotlin
private val _textoNuevo = MutableStateFlow("")
val textoNuevo = _textoNuevo.asStateFlow()

fun cambiarTexto(texto: String) {
    _textoNuevo.value = texto
}
````

### Factory

Lo que hace Android para crear y obtener los view model es: si no existe lo crea y te lo da, si ya
existe, simplemente te lo da. Para crear un view model, llama al constructor vacío del mismo. El
problema está en que si llama a un view model que recibe argumentos en su constructor, Android se queja.
Para eso están los Factory:

````kotlin
companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TareasApplication
                TareaViewModel(TareaRepository(app.tareaDao))
            }
        }
    }
````

Con `companion object` decimos que esta variable pertenece a la clase en sí, no al objeto instanciado.
El `viewModelFactory` es la función de Android que construye un factory. `initializer` es la receta,
lo que devuelva será el viewmodel. `this[APPLICATION_KEY]` dentro de `initializer`, Android te da
acceso a algunos datos útiles. `APPLICATION_KEY` te entrega el objeto Application de la app. 
`as TareasApplication`, queremos tratarlo como la Application que creamos. 
`TareaViewModel(TareaRepository(app.tareaDao))` crea el repositorio y el viewmodel con el repositorio
y se devuelve al ser la última línea.

**En TareasScreen hacemos esto para obtener el viewmodel con el repository:**

````kotlin
fun TareasScreen(viewModel: TareaViewModel = viewModel(factory = TareaViewModel.Factory))
````

