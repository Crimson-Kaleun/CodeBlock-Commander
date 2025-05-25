import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput


//import com.example.codeblockcommander.ui.theme.AppState
//import com.example.codeblockcommander.ui.theme.rememberAppState



@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WorkPlace(navController: NavController) {
    val appState = rememberAppState()

    var showBlockMenu by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var blocks by remember { mutableStateOf(emptyList<CodeBlock>()) }
    var draggedBlock by remember { mutableStateOf<CodeBlock?>(null) }
    var editedBlock by remember { mutableStateOf<CodeBlock?>(null) }
    var nextBlock by remember { mutableStateOf<Int?>(null) }

    val blockTypes = listOf("Start", "End", "Print", "Declare", "If", "For", "Set", "Function")

    var isDarkTheme by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Рабочая область") },
                actions = {
                    Row {
                        // Кнопка запуска
                        FloatingActionButton(
                            onClick = { executeProgram(appState, blocks) },
                            modifier = Modifier.padding(end = 8.dp),
                            containerColor = Color.Green
                        ) {
                            Icon(Icons.Default.PlayArrow, "Запуск")
                        }

                        // Кнопка отладки
                        FloatingActionButton(
                            onClick = { debugProgram(appState, blocks) },
                            modifier = Modifier.padding(end = 8.dp),
                            containerColor = Color.Blue
                        ) {
                            Icon(Icons.Default.Build, "Отладка")
                        }

                        // Кнопка остановки
                        FloatingActionButton(
                            onClick = { stopProgram(appState) },
                            containerColor = Color.Red
                        ) {
                            Icon(Icons.Default.Lock, "Стоп")
                        }
                    }
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
                                    if (type == "Start" && blocks.any { it.type == "Start" }) {
                                        appState.appendToConsole("Ошибка: блок Start уже существует!")
                                        return@DropdownMenuItem
                                    }
                                    if (type == "End" && blocks.any { it.type == "End" }) {
                                        appState.appendToConsole("Ошибка: блок End уже существует!")
                                        return@DropdownMenuItem
                                    }
                                    blocks = blocks + CodeBlock(
                                        id = System.currentTimeMillis().toInt(),
                                        type = type,
                                        x = 500f,
                                        y = 200f,
                                        params = when (type) {
                                            "Start" -> mapOf("nextBlock" to "-1")
                                            "End" -> emptyMap()
                                            "Print" -> mapOf("text" to "Hello", "nextBlock" to "-1")
                                            "Declare" -> mapOf("varName" to "", "varValue" to "", "nextBlock" to "-1")
                                            "Set" -> mapOf("varName" to "", "varValue" to "", "nextBlock" to "-1")
                                            "If" -> mapOf(
                                                "leftExpr" to "",
                                                "condition" to "==",
                                                "rightExpr" to "",
                                                "trueBlock" to "-1",
                                                "falseBlock" to "-1",
                                                "nextBlock" to "-1"
                                            )
                                            else -> mapOf("nextBlock" to "-1")
                                        }
                                    )
                                    showBlockMenu = false
                                }
                            )
                        }
                    }




                    DropdownMenu(
                        expanded = showSettingsMenu,
                        onDismissRequest = { showSettingsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Главное меню") },
                            onClick = {
                                navController.popBackStack()
                                showSettingsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isDarkTheme) "Светлая тема" else "Тёмная тема") },
                            onClick = {
                                isDarkTheme = !isDarkTheme
                                showSettingsMenu = false
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(Color.LightGray)
            ) {
                Text(
                    text = appState.consoleText,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top=8.dp, bottom = 28.dp),
                    color = Color.Black
                )
            }
        }

    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                blocks.forEach { block ->
                    val isDragged = draggedBlock?.id == block.id

                    DraggableBlock(
                        block = block,
                        isDragged = isDragged,
                        blocks = blocks,
                        onDragStart = { draggedBlock = block },
                        onDragEnd = { draggedBlock = null },
                        onEdit = { editedBlock = block },
                        onPositionUpdate = { x, y ->
                            blocks = blocks.map {
                                if (it.id == block.id) it.copy(x = x, y = y) else it
                            }
                        },
                        onNextBlockSelected = { blockId, nextBlockId ->
                            blocks = blocks.map { b ->
                                if (b.id == blockId) {
                                    val newParams = b.params.toMutableMap()
                                    newParams["nextBlock"] = nextBlockId.toString()
                                    b.copy(params = newParams)
                                } else {
                                    b
                                }
                            }
                        }
                    )
                }
            }

            editedBlock?.let { block ->
                BlockEditDialog(
                    block = block,
                    blocks = blocks,
                    appState = appState,
                    onDismiss = { editedBlock = null },
                    onSave = { newParams ->
                        blocks = blocks.map {
                            if (it.id == block.id) it.copy(params = newParams) else it
                        }
                        editedBlock = null
                    },
                    onNextBlockSelected = { blockId, nextBlockId ->
                        blocks = blocks.map { b ->
                            if (b.id == blockId) {
                                val newParams = b.params.toMutableMap()
                                newParams["nextBlock"] = nextBlockId.toString()
                                b.copy(params = newParams)
                            } else {
                                b
                            }
                        }
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
    blocks: List<CodeBlock>,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onEdit: () -> Unit,
    onPositionUpdate: (Float, Float) -> Unit,
    onNextBlockSelected: (Int, Int) -> Unit
) {
    val d = LocalDensity.current
    var offsetX by remember { mutableStateOf(block.x) }
    var offsetY by remember { mutableStateOf(block.y) }
    var showNextBlockMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .offset(((offsetX)/d.density).dp, ((offsetY)/d.density).dp)
            .size(160.dp, 120.dp)
            .background(
                color = when (block.type) {
                    "Print" -> Color(0xFF4CAF50)
                    "Declare" -> Color(0xFF009688)
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
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        onPositionUpdate(block.x + offsetX, block.y + offsetY)
                    },
                    onDragEnd = {
                        onPositionUpdate(block.x + offsetX, block.y + offsetY)
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

            // Display block specific info

            when (block.type) {
                "Print" -> Text(block.params["text"] ?: "", color = Color.White, fontSize = 12.sp)
                "Declare" -> Text("${block.params["varName"]} = ${block.params["varValue"]}",
                    color = Color.White, fontSize = 12.sp)
                "Set" -> Text("${block.params["varName"]} = ${block.params["varValue"]}",
                    color = Color.White, fontSize = 12.sp)
                "If" -> Text("${block.params["leftExpr"]} ${block.params["condition"]} ${block.params["rightExpr"]}",
                    color = Color.White, fontSize = 12.sp)
            }

            // Next block selector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(Color(0x55000000))
                    .clickable { showNextBlockMenu = true }
            ) {
                val nextBlockId = block.params["nextBlock"]?.toIntOrNull() ?: -1
                val nextBlock = blocks.find { it.id == nextBlockId }

                Text(
                    text = if (nextBlock != null) {
                        "Next: ${getBlockDisplayName(nextBlock, blocks)}"
                    } else {
                        "Next: none"
                    },
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(4.dp)
                )

                DropdownMenu(
                    expanded = showNextBlockMenu,
                    onDismissRequest = { showNextBlockMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            onNextBlockSelected(block.id, -1)
                            showNextBlockMenu = false
                        }
                    )
                    blocks.filter { it.id != block.id }.forEach { nextBlock ->
                        DropdownMenuItem(
                            text = { Text(getBlockDisplayName(nextBlock, blocks)) },
                            onClick = {
                                onNextBlockSelected(block.id, nextBlock.id)
                                showNextBlockMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BlockEditDialog(
    appState: AppState,
    block: CodeBlock,
    blocks: List<CodeBlock>,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit,
    onNextBlockSelected: (Int, Int) -> Unit
) {
    var showNextBlockMenu by remember { mutableStateOf(false) }
    var showTrueBlockMenu by remember { mutableStateOf(false) }
    var showFalseBlockMenu by remember { mutableStateOf(false) }

    // Локальные состояния для редактируемых полей
    var textValue by remember { mutableStateOf(block.params["text"] ?: "") }
    var varName by remember { mutableStateOf(block.params["varName"] ?: "") }
    var varValue by remember { mutableStateOf(block.params["varValue"] ?: "") }
    var leftExpr by remember { mutableStateOf(block.params["leftExpr"] ?: "") }
    var condition by remember { mutableStateOf(block.params["condition"] ?: "==") }
    var rightExpr by remember { mutableStateOf(block.params["rightExpr"] ?: "") }
    var nextBlock by remember { mutableStateOf(block.params["nextBlock"] ?: "") }

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
                            onValueChange = { textValue = it }
                        )

                        // Next block selector
                        Text("Следующий блок:", modifier = Modifier.padding(top = 8.dp))
                        Button(
                            onClick = { showNextBlockMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(blocks.find { it.id == (block.params["nextBlock"]?.toIntOrNull() ?: -1)?.takeIf { it != -1 }}?.let { getBlockDisplayName(it, blocks) } ?: "Выберите блок")
                        }
                    }
                    "Declare" -> {
                        Text("Имя переменной:")
                        TextField(
                            value = varName,
                            onValueChange = { varName = it }
                        )
                        Text("Значение:", modifier = Modifier.padding(top = 8.dp))
                        TextField(
                            value = varValue,
                            onValueChange = { varValue = it }
                        )

                        Text("Следующий блок:", modifier = Modifier.padding(top = 8.dp))
                        Button(
                            onClick = { showNextBlockMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(blocks.find { it.id == (block.params["nextBlock"]?.toIntOrNull() ?: -1)?.takeIf { it != -1 }}?.let { getBlockDisplayName(it, blocks) } ?: "Выберите блок")
                        }
                    }
                    "Set" -> {
                        val declaredVars = blocks
                            .filter { it.type == "Declare" }
                            .mapNotNull { it.params["varName"] }
                            .filter { it.isNotBlank() }
                            .distinct()

                        var showVarMenu by remember { mutableStateOf(false) }

                        Text("Выберите переменную:")
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = varName.ifEmpty { "Выберите переменную" },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showVarMenu = true }
                                    .padding(8.dp)
                                    .background(Color.LightGray)
                            )

                            DropdownMenu(
                                expanded = showVarMenu,
                                onDismissRequest = { showVarMenu = false }
                            ) {
                                declaredVars.forEach { _varName ->
                                    DropdownMenuItem(
                                        text = { Text(_varName) },
                                        onClick = {
                                            varName = _varName
                                            showVarMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Text("Значение:", modifier = Modifier.padding(top = 8.dp))
                        TextField(
                            value = varValue,
                            onValueChange = { varValue = it }
                        )
                    }
                    "If" -> {
                        Text("Условие:", modifier = Modifier.padding(top = 8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextField(
                                value = leftExpr,
                                onValueChange = { leftExpr = it },
                                modifier = Modifier.weight(1f)
                            )
                            DropdownMenu(
                                expanded = showNextBlockMenu,
                                onDismissRequest = { showNextBlockMenu = false }
                            ) {
                                listOf("==", "!=", ">", "<", ">=", "<=").forEach { cond ->
                                    DropdownMenuItem(
                                        text = { Text(cond) },
                                        onClick = {
                                            condition = cond
                                            showNextBlockMenu = false
                                        }
                                    )
                                }
                            }
                            Text(condition, modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clickable { showNextBlockMenu = true })
                            TextField(
                                value = rightExpr,
                                onValueChange = { rightExpr = it },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text("Блок если true:", modifier = Modifier.padding(top = 8.dp))
                        Button(
                            onClick = { showTrueBlockMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(blocks.find { it.id == (block.params["trueBlock"]?.toIntOrNull() ?: -1)?.takeIf { it != -1 }}?.let { getBlockDisplayName(it, blocks) } ?: "Выберите блок")
                        }

                        Text("Блок если false:", modifier = Modifier.padding(top = 8.dp))
                        Button(
                            onClick = { showFalseBlockMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(blocks.find { it.id == (block.params["falseBlock"]?.toIntOrNull() ?: -1)?.takeIf { it != -1 }}?.let { getBlockDisplayName(it, blocks) } ?: "Выберите блок")
                        }

                        Text("Следующий блок:", modifier = Modifier.padding(top = 8.dp))
                        Button(
                            onClick = { showNextBlockMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(blocks.find { it.id == (block.params["nextBlock"]?.toIntOrNull() ?: -1)?.takeIf { it != -1 }}?.let { getBlockDisplayName(it, blocks) } ?: "Выберите блок")
                        }
                    }
                }

                // Dropdown menus for block selection
                DropdownMenu(
                    expanded = showNextBlockMenu && block.type != "If",
                    onDismissRequest = { showNextBlockMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            onNextBlockSelected(block.id, -1)
                            showNextBlockMenu = false
                        }
                    )
                    blocks.filter { it.id != block.id }.forEach { nextBlock ->
                        DropdownMenuItem(
                            text = { Text(getBlockDisplayName(nextBlock, blocks)) },
                            onClick = {
                                onNextBlockSelected(block.id, nextBlock.id)
                                showNextBlockMenu = false
                            }
                        )
                    }
                }

                DropdownMenu(
                    expanded = showTrueBlockMenu,
                    onDismissRequest = { showTrueBlockMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            val newParams = block.params.toMutableMap()
                            newParams["trueBlock"] = "-1"
                            onSave(newParams)
                            showTrueBlockMenu = false
                        }
                    )
                    blocks.filter { it.id != block.id }.forEach { nextBlock ->
                        DropdownMenuItem(
                            text = { Text(getBlockDisplayName(nextBlock, blocks)) },
                            onClick = {
                                val newParams = block.params.toMutableMap()
                                newParams["trueBlock"] = nextBlock.id.toString()
                                onSave(newParams)
                                showTrueBlockMenu = false
                            }
                        )
                    }
                }

                DropdownMenu(
                    expanded = showFalseBlockMenu,
                    onDismissRequest = { showFalseBlockMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            val newParams = block.params.toMutableMap()
                            newParams["falseBlock"] = "-1"
                            onSave(newParams)
                            showFalseBlockMenu = false
                        }
                    )
                    blocks.filter { it.id != block.id }.forEach { nextBlock ->
                        DropdownMenuItem(
                            text = { Text(getBlockDisplayName(nextBlock, blocks)) },
                            onClick = {
                                val newParams = block.params.toMutableMap()
                                newParams["falseBlock"] = nextBlock.id.toString()
                                onSave(newParams)
                                showFalseBlockMenu = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val newParams = when (block.type) {
                    "Print" -> {
                        appState.appendToConsole("Вывод: $textValue")
                        mapOf(
                            "text" to textValue,
                            "nextBlock" to (block.params["nextBlock"] ?: "-1")
                        )
                    }
                    "Declare" -> {
                        mapOf(
                            "varName" to varName,
                            "varValue" to varValue,
                            "nextBlock" to (block.params["nextBlock"] ?: "-1")
                        )
                    }
                    "Set" -> {
                        mapOf(
                            "varName" to varName,
                            "varValue" to varValue,
                            "nextBlock" to (block.params["nextBlock"] ?: "-1")
                        )
                    }
                    "If" -> {
                        mapOf(
                            "leftExpr" to leftExpr,
                            "condition" to condition,
                            "rightExpr" to rightExpr,
                            "trueBlock" to (block.params["trueBlock"] ?: "-1"),
                            "falseBlock" to (block.params["falseBlock"] ?: "-1"),
                            "nextBlock" to (block.params["nextBlock"] ?: "-1")
                        )
                    }
                    else -> block.params
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

fun getBlockDisplayName(block: CodeBlock, allBlocks: List<CodeBlock>): String {
    val sameTypeBlocks = allBlocks.filter { it.type == block.type }
    return if (sameTypeBlocks.size > 1) {
        "${block.type} #${sameTypeBlocks.indexOf(block) + 1}"
    } else {
        block.type
    }
}



fun executeProgram(appState: AppState, blocks: List<CodeBlock>) {
    val startBlock = blocks.find { it.type == "Start" } ?: run {
        appState.appendToConsole("Ошибка: нет блока Start!")
        return
    }

    appState.clearConsole()
    appState.appendToConsole("=== Запуск программы ===")

    var currentBlock: CodeBlock? = startBlock
    while (currentBlock != null) {
        appState.appendToConsole(currentBlock.generateCode())

        currentBlock = when (currentBlock.type) {
            "Start", "Print", "Declare", "Set" -> {
                blocks.find { it.id == currentBlock.params["nextBlock"]?.toIntOrNull() }
            }
            "If" -> {
                // Логика для условного перехода
                null // Временная заглушка
            }
            "End" -> null
            else -> null
        }
    }

    appState.appendToConsole("=== Программа завершена ===")
}

fun debugProgram(appState: AppState, blocks: List<CodeBlock>) {
    appState.appendToConsole("=== Режим отладки ===")
    executeProgram(appState, blocks) // Пока аналогично, можно расширить
}

fun stopProgram(appState: AppState) {
    appState.appendToConsole("=== Программа остановлена ===")
}