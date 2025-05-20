import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WorkPlace(navController: NavController) {

    var showBlockMenu by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var consoleText by remember { mutableStateOf("Консоль выполнения:\n") }
    var blocks by remember { mutableStateOf(emptyList<CodeBlock>()) }
    var draggedBlock by remember { mutableStateOf<CodeBlock?>(null) }
    var editedBlock by remember { mutableStateOf<CodeBlock?>(null) }

    val blockTypes = listOf("Print", "If", "For", "Variable", "Function")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Рабочая область") },
                actions = {
                    IconButton(onClick = { showBlockMenu = true }) {
                        Icon(Icons.Default.Add, "Добавить блок")
                    }
                    IconButton(onClick = { showSettingsMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Настройки")
                    }

                    DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                        blockTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    blocks = blocks + CodeBlock(
                                        id = System.currentTimeMillis().toInt(),
                                        type = type,
                                        x = 500f,
                                        y = 200f,
                                        params = when (type) {
                                            "Print" -> mapOf("text" to "Hello")
                                            "If" -> mapOf("condition" to "true")
                                            else -> emptyMap()
                                        }
                                    )
                                    showBlockMenu = false
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            //Console
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.LightGray)
                    .padding(48.dp)
            ) {
                Text(
                    text = consoleText,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Black
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            // Рабочая область
            Box(modifier = Modifier.fillMaxSize()) {
                blocks.forEach { block ->
                    val isDragged = draggedBlock?.id == block.id

                    DraggableBlock(
                        block = block,
                        isDragged = isDragged,
                        onDragStart = { draggedBlock = block },
                        onDragEnd = { draggedBlock = null },
                        onEdit = { editedBlock = block },
                        onPositionUpdate = { x, y ->
                            blocks = blocks.map {
                                if (it.id == block.id) it.copy(x = x, y = y) else it
                            }
                        }
                    )
                }
            }

            //Меню блока
            editedBlock?.let { block ->
                BlockEditDialog(
                    block = block,
                    onDismiss = { editedBlock = null },
                    onSave = { newParams ->
                        blocks = blocks.map {
                            if (it.id == block.id) it.copy(params = newParams) else it
                        }
                        editedBlock = null
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableBlock(
    block: CodeBlock,
    isDragged: Boolean,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onEdit: () -> Unit,
    onPositionUpdate: (Float, Float) -> Unit
) {
    val d = LocalDensity.current
    var offsetX by remember { mutableStateOf(block.x) }
    var offsetY by remember { mutableStateOf(block.y) }

    Box(
        modifier = Modifier
            .offset(((offsetX)/d.density).dp, ((offsetY)/d.density).dp)
            .size(160.dp, 120.dp)
            .background(
                color = when (block.type) {
                    "Print" -> Color(0xFF4CAF50)
                    "If" -> Color(0xFF2196F3)
                    "For" -> Color(0xFFFFC107)
                    else -> Color(0xFF9C27B0)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .combinedClickable(
                onClick = { /* */ },
                onLongClick = onEdit
            )
            .pointerInput(block.id) {
                detectDragGestures(
                    onDragStart = {
                        //offsetX = 0f
                        //offsetY = 0f
                        onDragStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        onPositionUpdate(block.x + offsetX, block.y + offsetY)
                    },
                    onDragEnd = {
                        onPositionUpdate(block.x + offsetX, block.y + offsetY)
                        //offsetX = 0f
                        //offsetY = 0f
                        onDragEnd()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(block.type, color = Color.White)
            Text("🟢", modifier = Modifier.padding(top = 4.dp))
            if (block.params.isNotEmpty()) {
                Text(
                    block.params.values.first().toString(),
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun BlockEditDialog(
    block: CodeBlock,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    var textValue by remember { mutableStateOf(block.params.values.firstOrNull() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать ${block.type}") },
        text = {
            Column {
                when (block.type) {
                    "Print" -> {
                        Text("Текст для вывода:")
                        TextField(
                            value = textValue,
                            onValueChange = {
                                textValue = it
                                //consoleText += "${textValue}\n"
                            }
                        )
                    }
                    "If" -> {
                        Text("Условие:")
                        TextField(
                            value = textValue,
                            onValueChange = { textValue = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val newParams = when (block.type) {
                    "Print" -> mapOf("text" to textValue)
                    "If" -> mapOf("condition" to textValue)
                    else -> emptyMap()
                }
                onSave(newParams)
            }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

data class CodeBlock(
    val id: Int,
    val type: String,
    val x: Float,
    val y: Float,
    val params: Map<String, String> = emptyMap()
)