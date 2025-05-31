import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.example.codeblockcommander.ui.theme.Parser
import com.example.codeblockcommander.ui.theme.isCorrectName
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin


//import com.example.codeblockcommander.ui.theme.AppState
//import com.example.codeblockcommander.ui.theme.rememberAppState

@Composable
fun Float.toDp(): Dp = with(LocalDensity.current) { this@toDp.toDp() }
@Composable
fun Dp.toPx(): Float = with(LocalDensity.current) { this@toPx.toPx() }


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WorkPlace(navController: NavController) {
    val appState = rememberAppState()

    var showBlockMenu by remember { mutableStateOf(false) }
    var showSubMenu by remember { mutableStateOf<BlockCategory?>(null) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var blocks by remember { mutableStateOf(emptyList<CodeBlock>()) }
    var draggedBlock by remember { mutableStateOf<CodeBlock?>(null) }
    var editedBlock by remember { mutableStateOf<CodeBlock?>(null) }

    //val blockTypes = listOf("Start", "End", "Print", "Declare", "If", "For", "Set", "Function")

    var isDarkTheme by remember { mutableStateOf(false) }

    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(1f) }


    val panGesture = rememberTransformableState { zoomChange, panChange, _ ->
        scale *= zoomChange
        offsetX += panChange.x
        offsetY += panChange.y
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Рабочая область") },
                actions = {
                    Row {
                        FloatingActionButton(
                            onClick = { executeProgram(appState, blocks) },
                            modifier = Modifier.padding(end = 8.dp),
                            containerColor = Color.Green
                        ) {
                            Icon(Icons.Default.PlayArrow, "Запуск")
                        }

                        if (appState.isDebugging) {
                            FloatingActionButton(
                                onClick = {
                                    if (appState.debugPaused) {
                                        appState.debugPaused = false
                                        executeDebugStep(appState, blocks)
                                    } else {
                                        appState.debugPaused = true
                                    }
                                },
                                modifier = Modifier.padding(end = 8.dp),
                                containerColor = Color.Blue
                            ) {
                                Icon(
                                    if (appState.debugPaused) Icons.Default.PlayArrow else Icons.Default.Star,
                                    "Шаг отладки"
                                )
                            }
                        } else {
                            FloatingActionButton(
                                onClick = { debugProgram(appState, blocks) },
                                modifier = Modifier.padding(end = 8.dp),
                                containerColor = Color.Blue
                            ) {
                                Icon(Icons.Default.Build, "Отладка")
                            }
                        }

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

                    DropdownMenu(
                        expanded = showBlockMenu,
                        onDismissRequest = { showBlockMenu = false }
                    ) {
                        blockCategories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (category) {
                                            is BlockCategory.Core -> "Основные блоки"
                                            is BlockCategory.Variables -> "Переменные"
                                            is BlockCategory.Arrays -> "Массивы"
                                        }
                                    )
                                },
                                onClick = {
                                    showSubMenu = category
                                    showBlockMenu = false
                                }
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showSubMenu != null,
                        onDismissRequest = { showSubMenu = null }
                    ) {
                        showSubMenu?.let { category ->
                            blockSubTypes[category]?.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        if ((type == "Start" || type == "End") &&
                                            blocks.any { it.type == type }) {
                                            appState.appendToConsole("Ошибка: блок $type уже существует!")
                                        } else {
                                            val newBlock = when (type) {
                                                "Declare Var" -> CodeBlock(
                                                    id = System.currentTimeMillis().toInt(),
                                                    type = type,
                                                    x = 500f,
                                                    y = 200f,
                                                    appState = appState,
                                                    params = mapOf(
                                                        "varName" to "",
                                                        "varType" to "Int",
                                                        "varValue" to "",
                                                        "nextBlock" to "-1",
                                                    )
                                                )
                                                "Set Var" -> CodeBlock(
                                                    id = System.currentTimeMillis().toInt(),
                                                    type = type,
                                                    x = 500f,
                                                    y = 200f,
                                                    appState = appState,
                                                    params = mapOf(
                                                        "varName" to "",
                                                        "varValue" to "",
                                                        "nextBlock" to "-1",
                                                    )
                                                )
                                                "Declare Array" -> CodeBlock(
                                                    id = System.currentTimeMillis().toInt(),
                                                    type = type,
                                                    x = 500f,
                                                    y = 200f,
                                                    appState = appState,
                                                    params = mapOf(
                                                        "arrayName" to "",
                                                        "varType" to "Int",
                                                        "arrayValue" to "",
                                                        "arrayId" to "",
                                                        "arraySize" to "",
                                                        "nextBlock" to "-1",
                                                    )
                                                )
                                                "Set Array" -> CodeBlock(
                                                    id = System.currentTimeMillis().toInt(),
                                                    type = type,
                                                    x = 500f,
                                                    y = 200f,
                                                    appState = appState,
                                                    params = mapOf(
                                                        "arrayName" to "",
                                                        "arrayValue" to "",
                                                        "arrayId" to "",
                                                        "nextBlock" to "-1",
                                                    )
                                                )

                                                "Get Array Element" -> CodeBlock(
                                                    id = System.currentTimeMillis().toInt(),
                                                    type = type,
                                                    x = 500f,
                                                    y = 200f,
                                                    appState = appState,
                                                    params = mapOf(
                                                        "varName" to "",
                                                        "arrayName" to "",
                                                        "index" to "0",
                                                        "nextBlock" to "-1",
                                                    )
                                                )
                                                "If" -> CodeBlock(
                                                    id = System.currentTimeMillis().toInt(),
                                                    type = type,
                                                    x = 500f,
                                                    y = 200f,
                                                    appState = appState,
                                                    params = mapOf(
                                                        "leftExpr" to "",
                                                        "condition" to "==",
                                                        "rightExpr" to "",
                                                        "trueBlock" to "-1",
                                                        "falseBlock" to "-1",
                                                    )
                                                )



                                                else -> CodeBlock(
                                                    id = System.currentTimeMillis().toInt(),
                                                    type = type,
                                                    x = 500f,
                                                    y = 200f,
                                                    appState = appState,
                                                    params = mapOf("nextBlock" to "-1")
                                                )
                                            }
                                            blocks = blocks + newBlock
                                        }
                                        showSubMenu = null
                                    }
                                )
                            }
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
                DebugControls(appState, blocks)
                Text(
                    text = appState.consoleText,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 28.dp),
                    color = Color.Black
                )
            }

        }

    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, _, _ ->
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
            ) {
                DrawConnections(blocks, offsetX, offsetY)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                ) {
                    blocks.forEach { block ->
                        val isDragged = draggedBlock?.id == block.id

                        DraggableBlock(
                            block = block,
                            isDragged = isDragged,
                            blocks = blocks,
                            onDragStart = { draggedBlock = block },
                            onDragEnd = { draggedBlock = null },
                            onEdit = { editedBlock = block },
                            appState = appState,
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
    onNextBlockSelected: (Int, Int) -> Unit,
    appState: AppState
) {
    val d = LocalDensity.current
    var offsetX by remember { mutableStateOf(block.x) }
    var offsetY by remember { mutableStateOf(block.y) }
    var showNextBlockMenu by remember { mutableStateOf(false) }

    val borderColor = when {
        appState.currentDebugBlock?.id == block.id -> Color.Yellow
        isDragged -> Color.White.copy(alpha = 0.0f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            //.offset(((offsetX) / d.density).dp, ((offsetY) / d.density).dp)
            .offset(offsetX.toDp(), offsetY.toDp())
            .size(160.dp, 120.dp)
            .background(
                color = when (block.type) {
                    "Print Var" -> Color(0xFF4CAF50)
                    "Declare Var" -> Color(0xFF009688)
                    "Set Var" -> Color(0xFF112A28)
                    "If" -> Color(0xFF2196F3)
                    "For" -> Color(0xFFFFC107)
                    else -> Color(0xFF9C27B0)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .border(5.dp, borderColor, RoundedCornerShape(8.dp))
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
                        onPositionUpdate(offsetX, offsetY)
                    },
                    onDragEnd = {
                        //onPositionUpdate(block.x, block.y)
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

            // Display

            when (block.type) {
                "Print Var" -> Text(block.params["text"] ?: "", color = Color.White, fontSize = 12.sp)
                "Declare Var" -> Text("${block.params["varName"]} = ${block.params["varValue"]}",
                    color = Color.White, fontSize = 12.sp)
                "Set Var" -> Text("${block.params["varName"]} = ${block.params["varValue"]}",
                    color = Color.White, fontSize = 12.sp)
                "Print Array" -> Text(block.params["text"] ?: "", color = Color.White, fontSize = 12.sp)
                "Declare Array" -> Text("${block.params["arrayName"]}<${block.params["varType"]}>(${block.params["arraySize"]})",
                    color = Color.White, fontSize = 12.sp)
                "Set Array" -> Text("${block.params["arrayName"]}[${block.params["arrayId"]}]= ${block.params["arrayValue"]}",
                    color = Color.White, fontSize = 12.sp)
                "If" -> Text("${block.params["leftExpr"]} ${block.params["condition"]} ${block.params["rightExpr"]}",
                    color = Color.White, fontSize = 12.sp)

                "Get Array Element" -> Text("${block.params["arrayName"]} ${block.params["arrayID"]} ${block.params["varName"]}",
                    color = Color.White, fontSize = 12.sp)
            }

            // Next block
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

    var textValue by remember { mutableStateOf(block.params["text"] ?: "") }
    var varName by remember { mutableStateOf(block.params["varName"] ?: "") }
    var arrayName by remember { mutableStateOf(block.params["arrayName"] ?: "") }
    var arrayId by remember { mutableStateOf(block.params["arrayId"] ?: "") }
    var arrayValue by remember { mutableStateOf(block.params["arrayValue"] ?: "") }
    var arraySize by remember { mutableStateOf(block.params["arraySize"] ?: "5") }

    var varValue by remember { mutableStateOf(block.params["varValue"] ?: "") }
    var leftExpr by remember { mutableStateOf(block.params["leftExpr"] ?: "") }
    var condition by remember { mutableStateOf(block.params["condition"] ?: "==") }
    var rightExpr by remember { mutableStateOf(block.params["rightExpr"] ?: "") }

    var selectedType by remember { mutableStateOf(block.params["varType"] ?: "Int") }
    var varType by remember { mutableStateOf("") }
    var isVariableMode by remember { mutableStateOf(block.params["isVariable"]?.toBooleanStrict() ?: false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать ${block.type}") },
        text = {
            Column {
                when (block.type) {
                    "Print Var" -> {
                        val declaredVars = blocks
                            .filter { it.type == "Declare Var" }
                            .mapNotNull { it.params["varName"] }
                            .filter { it.isNotBlank() }
                            .distinct()

                        var showVarMenu by remember { mutableStateOf(false) }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !isVariableMode,
                                onClick = { isVariableMode = false }
                            )
                            Text("Текст")

                            RadioButton(
                                selected = isVariableMode,
                                onClick = { isVariableMode = true }
                            )
                            Text("Переменная")
                        }

                        if (isVariableMode) {
                            Text("Выберите переменную:")
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = textValue.ifEmpty { "Выберите переменную" },
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
                                    declaredVars.forEach { varName ->
                                        DropdownMenuItem(
                                            text = { Text(varName) },
                                            onClick = {
                                                textValue = varName
                                                showVarMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            Text("Текст для вывода:")
                            TextField(
                                value = textValue,
                                onValueChange = { textValue = it }
                            )
                        }
                    }
                    "Declare Var" -> {
                        val availableTypes = listOf("Int", "Double", "Boolean", "String")
                        var showTypeMenu by remember { mutableStateOf(false) }

                        Text("Имя переменной:")
                        TextField(
                            value = varName,
                            onValueChange = { varName = it }
                        )

                        Text("Тип переменной:", modifier = Modifier.padding(top = 8.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = selectedType,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTypeMenu = true }
                                    .padding(8.dp)
                                    .background(Color.LightGray)
                            )
                            DropdownMenu(
                                expanded = showTypeMenu,
                                onDismissRequest = { showTypeMenu = false }
                            ) {
                                availableTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            selectedType = type
                                            showTypeMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Text("Значение (опционально):", modifier = Modifier.padding(top = 8.dp))
                        TextField(
                            value = varValue,
                            onValueChange = { varValue = it }
                        )

                    }
                    "Set Var" -> {
                        val declaredVars = blocks
                            .filter { it.type == "Declare Var" }
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

                    "Declare Array" -> {

                        val availableTypes = listOf("Int", "Double", "Boolean", "String")
                        var showTypeMenu by remember { mutableStateOf(false) }

                        Text("Имя переменной:")
                        TextField(
                            value = arrayName,
                            onValueChange = { arrayName = it }
                        )

                        Text("Тип Массива:", modifier = Modifier.padding(top = 8.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = selectedType,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTypeMenu = true }
                                    .padding(8.dp)
                                    .background(Color.LightGray)
                            )
                            DropdownMenu(
                                expanded = showTypeMenu,
                                onDismissRequest = { showTypeMenu = false }
                            ) {
                                availableTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            selectedType = type
                                            showTypeMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Text("Размер массива:", modifier = Modifier.padding(top = 8.dp))
                        TextField(
                            value = arraySize,
                            onValueChange = { if (it.all { c -> c.isDigit() }) arraySize = it }
                        )

                        Text("Значение элементов(опционально):", modifier = Modifier.padding(top = 8.dp))
                        TextField(
                            value = arrayValue,
                            onValueChange = { arrayValue = it }
                        )

                    }


                    "Set Array" -> {
                        val declaredVars = blocks
                            .filter { it.type == "Declare Array" }
                            .mapNotNull { it.params["arrayName"] }
                            .filter { it.isNotBlank() }
                            .distinct()

                        var showVarMenu by remember { mutableStateOf(false) }

                        Text("Выберите массив:")
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = arrayName.ifEmpty { "Выберите массив" },
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
                                declaredVars.forEach { _arrayValue ->
                                    DropdownMenuItem(
                                        text = { Text(_arrayValue) },
                                        onClick = {
                                            arrayName = _arrayValue
                                            showVarMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Text("Номер элемента:", modifier = Modifier.padding(top = 8.dp))
                        TextField(
                            value = arrayId,
                            onValueChange = { arrayId = it }
                        )

                        Text("Значение:", modifier = Modifier.padding(top = 8.dp))
                        TextField(
                            value = arrayValue,
                            onValueChange = { arrayValue = it }
                        )
                    }


                    "Print Array" -> {
                        val declaredVars = blocks
                            .filter { it.type == "Declare Array" }
                            .mapNotNull { it.params["arrayName"] }
                            .filter { it.isNotBlank() }
                            .distinct()

                        var showVarMenu by remember { mutableStateOf(false) }

                        Text("Выберите массив:")
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = textValue.ifEmpty { "Выберите массив" },
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
                                declaredVars.forEach { _textValue ->
                                    DropdownMenuItem(
                                        text = { Text(_textValue) },
                                        onClick = {
                                            textValue = _textValue
                                            showVarMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    "Get Array Element" -> {
                        val declaredVars = blocks
                            .filter { it.type == "Declare Array" }
                            .mapNotNull { it.params["arrayName"] }
                            .filter { it.isNotBlank() }
                            .distinct()

                        var showVarMenu by remember { mutableStateOf(false) }

                        Text("Выберите массив:")
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = arrayName.ifEmpty { "Выберите массив" },
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
                                declaredVars.forEach { _arrayValue ->
                                    DropdownMenuItem(
                                        text = { Text(_arrayValue) },
                                        onClick = {
                                            arrayName = _arrayValue
                                            showVarMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Text("Номер элемента:", modifier = Modifier.padding(top = 8.dp))
                        TextField(
                            value = arrayId,
                            onValueChange = { arrayId = it }
                        )

                        val declaredVars2 = blocks
                            .filter { it.type == "Declare Var" }
                            .mapNotNull { it.params["varName"] }
                            .filter { it.isNotBlank() }
                            .distinct()

                        var showVarMenu2 by remember { mutableStateOf(false) }

                        Text("Выберите переменную:")
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = varName.ifEmpty { "Выберите переменную" },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showVarMenu2 = true }
                                    .padding(8.dp)
                                    .background(Color.LightGray)
                            )

                            DropdownMenu(
                                expanded = showVarMenu2,
                                onDismissRequest = { showVarMenu2 = false }
                            ) {
                                declaredVars2.forEach { _varName ->
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


                    }
                }


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
                errorMessage?.let {
                    Text(it, color = Color.Red)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val newParams = when (block.type) {
                    "Print Var" -> {
                        appState.appendToConsole("Вывод: $textValue")
                        mapOf(
                            "text" to textValue,
                            "nextBlock" to (block.params["nextBlock"] ?: "-1"),
                            "isVariable" to isVariableMode.toString()
                        )
                    }
                    "Declare Var" -> {
                        if(!isCorrectName(varName)){
                            errorMessage = "Некорректное имя переменной"
                            appState.appendToConsole("Ошибка: Некорректное название переменной!")
                            return@Button
                        }
                        else {
                            //appState.addVariable(varName, selectedType, varValue.ifEmpty { null })
                            mapOf(
                                //mutableStateOf(block.params["varType"]
                                "varType" to selectedType,
                                "varName" to varName,
                                "varValue" to varValue,
                                "nextBlock" to (block.params["nextBlock"] ?: "-1")
                            )
                        }
                    }
                    "Set Var" -> {

                        try {
                            val parsedValue = when (varType) {
                                "Int" -> varValue.toInt()
                                "Double" -> varValue.toDouble()
                                "Boolean" -> varValue.toBoolean()
                                else -> varValue
                            }
                            //appState.updateVariable(varName, parsedValue)
                            onSave(mapOf(
                                "varName" to varName,
                                "varValue" to varValue,
                                "nextBlock" to (block.params["nextBlock"] ?: "-1")
                            )
                            )
                        } catch (e: Exception) {
                            appState.appendToConsole("Ошибка: неверный тип значения для $varName")
                        }

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



                    "Print Array" -> {
                        appState.appendToConsole("Вывод: $textValue")
                        mapOf(
                            "text" to textValue,
                            "nextBlock" to (block.params["nextBlock"] ?: "-1")
                        )
                    }
                    "Declare Array" -> {
                        if(!isCorrectName(arrayName)){
                            errorMessage = "Некорректное имя переменной"
                            appState.appendToConsole("Ошибка: Некорректное название переменной!")
                            return@Button
                        }
                        else {
                            //appState.addVariable(varName, selectedType, varValue.ifEmpty { null })
                            mapOf(
                                //mutableStateOf(block.params["varType"]
                                "varType" to selectedType,
                                "arrayName" to arrayName,
                                "arrayValue" to arrayValue,
                                "arraySize" to arraySize,
                                "nextBlock" to (block.params["nextBlock"] ?: "-1")
                            )
                        }
                    }

                    "Set Array" -> {
                        mapOf(
                            "arrayName" to arrayName,
                            "arrayId" to arrayId,
                            "arrayValue" to arrayValue,
                            "nextBlock" to (block.params["nextBlock"] ?: "-1")
                        )
                    }

                    "Get Array Element" -> {

                        mapOf(
                            "arrayName" to arrayName,
                            "arrayId" to arrayId,
                            "varName" to varName,
                            "nextBlock" to (block.params["nextBlock"] ?: "-1")
                        )
                    }

                    else -> block.params
                }
                errorMessage = null
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
    appState.variables.clear()
    val startBlock = blocks.find { it.type == "Start" } ?: run {
        appState.appendToConsole("Ошибка: нет блока Start!")
        return
    }

    appState.clearConsole()
    appState.appendToConsole("=== Запуск программы ===")

    var currentBlock: CodeBlock? = startBlock
    while (currentBlock != null) {
        //appState.appendToConsole(currentBlock.generateCode())
        //appState.appendToConsole(currentBlock.execute())
        currentBlock.execute()
        currentBlock = when (currentBlock.type) {
            "Start", "Print Var", "Declare Var", "Set Var", "Print Array", "Declare Array", "Set Array" -> {
                blocks.find { it.id == currentBlock.params["nextBlock"]?.toIntOrNull() }
            }
            "If" -> {
                val expr = Parser(currentBlock.generateCode(), appState)
                appState.appendToConsole(currentBlock.generateCode())
                appState.appendToConsole(expr)

                if (expr.toDouble() == 1.0){
                    blocks.find { it.id == currentBlock.params["trueBlock"]?.toIntOrNull() }
                }
                else {
                    blocks.find { it.id == currentBlock.params["falseBlock"]?.toIntOrNull() }
                }
            }
            "End" -> null
            else -> null
        }
    }
    appState.appendToConsole("=== Переменные ===")
    for (item in appState.variables) {
        appState.appendToConsole("${item.key.toString()} ${item.value.second.toString()}")
    }
    appState.appendToConsole("=== Массивы ===")
    for (item in appState.arrays) {
        appState.appendToConsole("${item.key.toString()} ${item.value.second.toString()}")

        val name = item.key.toString()
        val array = appState.arrays[name]?.second
        appState.appendToConsole("$name: ${array?.contentToString()}")

    }
    appState.appendToConsole("=== ===")
    for (item in blocks) {
        appState.appendToConsole(item.generateCode())
    }
    appState.appendToConsole("=== ===")

    appState.appendToConsole("=== Программа завершена ===")
}

fun debugProgram(appState: AppState, blocks: List<CodeBlock>) {
    appState.variables.clear()
    val startBlock = blocks.find { it.type == "Start" } ?: run {
        appState.appendToConsole("Ошибка: нет блока Start!")
        return
    }

    appState.clearConsole()
    appState.appendToConsole("=== Режим отладки ===")
    appState.isDebugging = true
    appState.debugPaused = true
    appState.currentDebugBlock = blocks.find { it.id == startBlock.id }
}

fun executeDebugStep(appState: AppState, blocks: List<CodeBlock>) {
    val currentId = appState.currentDebugBlock?.id ?: return
    val currentBlock = blocks.find { it.id == currentId } ?: return

    currentBlock.execute()
    appState.appendToConsole(currentBlock.generateCode())


    val nextBlock = when (currentBlock.type) {
        "Start", "Print Var", "Declare Var", "Set Var", "Print Array", "Declare Array", "Set Array" -> {
            blocks.find { it.id == currentBlock.params["nextBlock"]?.toIntOrNull() }
        }
        "If" -> {
            val expr = Parser(currentBlock.generateCode(), appState)
            if (expr.toDouble() == 1.0) {
                blocks.find { it.id == currentBlock.params["trueBlock"]?.toIntOrNull() }
            } else {
                blocks.find { it.id == currentBlock.params["falseBlock"]?.toIntOrNull() }
            }
        }
        "End" -> null
        else -> null
    }

    appState.currentDebugBlock = nextBlock

    if (nextBlock == null) {
        appState.appendToConsole("=== Отладка завершена ===")
        appState.stopDebugging()
    }
}


fun stopProgram(appState: AppState) {
    appState.appendToConsole("=== Отладка прервана ===")
    appState.stopDebugging()
}


@Composable
fun DebugControls(appState: AppState, blocks: List<CodeBlock>) {
    if (appState.isDebugging) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.LightGray)
                .padding(8.dp)
        ) {
            Text("Режим отладки", fontWeight = FontWeight.Bold)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { executeDebugStep(appState, blocks) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Шаг")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { stopProgram(appState) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Стоп")
                }
            }

            var text = ""
            for (item in appState.variables) {
                text += "${item.key.toString()} ${item.value.second.toString()}\n"
            }
            for (item in appState.arrays) {
                //appState.appendToConsole("${item.key.toString()} ${item.value.second.toString()}")

                val name = item.key.toString()
                val array = appState.arrays[name]?.second
                text += "$name: ${array?.contentToString()}\n"

            }

            appState.currentDebugBlock?.let { block ->
                Text("Текущий блок: ${block.type}", modifier = Modifier.padding(top = 8.dp))
                Text(
                    //block.generateCode(),
                    text = text,
                    fontStyle = FontStyle.Italic)
            }
        }
    }
}





@Composable
fun ControlsPanel(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetView: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        IconButton(onClick = onZoomIn) {
            Icon(Icons.Default.Add, "Увеличить")
        }
        IconButton(onClick = onZoomOut) {
            //Icon(Icons.Default.Remove, "Уменьшить")
        }
        IconButton(onClick = onResetView) {
            Icon(Icons.Default.Refresh, "Сбросить вид")
        }
    }
}


@Composable
fun DrawConnections(blocks: List<CodeBlock>, offsetX: Float, offsetY: Float) {
    val density = LocalDensity.current
    val path = remember { Path() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        blocks.forEach { block ->
            if (block.type != "If") {
                val nextBlockId = block.params["nextBlock"]?.toIntOrNull() ?: -1
                if (nextBlockId != -1) {
                    drawConnection(
                        block = block,
                        nextBlockId = nextBlockId,
                        blocks = blocks,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        startYOffset = 60.dp,
                        color = Color(0xFF448AFF)
                    )
                }
            } else {
                //If block
                val trueBlockId = block.params["trueBlock"]?.toIntOrNull() ?: -1
                val falseBlockId = block.params["falseBlock"]?.toIntOrNull() ?: -1

                //True
                if (trueBlockId != -1) {
                    drawConnection(
                        block = block,
                        nextBlockId = trueBlockId,
                        blocks = blocks,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        startYOffset = 40.dp,
                        color = Color(0xFF4CAF50)
                    )
                }

                //False
                if (falseBlockId != -1) {
                    drawConnection(
                        block = block,
                        nextBlockId = falseBlockId,
                        blocks = blocks,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        startYOffset = 80.dp,
                        color = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawConnection(
    block: CodeBlock,
    nextBlockId: Int,
    blocks: List<CodeBlock>,
    offsetX: Float,
    offsetY: Float,
    startYOffset: Dp,
    color: Color
) {
    val nextBlock = blocks.find { it.id == nextBlockId } ?: return
    val path = Path()

    val startX = block.x + 160.dp.toPx()
    val startY = block.y + startYOffset.toPx()

    val endX = nextBlock.x
    val endY = nextBlock.y + 60.dp.toPx()

    val controlX1 = startX + 100f
    val controlY1 = startY
    val controlX2 = endX - 100f
    val controlY2 = endY

    path.reset()
    path.moveTo(startX + offsetX, startY + offsetY)
    path.cubicTo(
        controlX1 + offsetX, controlY1 + offsetY,
        controlX2 + offsetX, controlY2 + offsetY,
        endX + offsetX, endY + offsetY
    )

    val angle = atan2(endY - startY, endX - startX)
    val arrowSize = 10.dp.toPx()
    val arrowX1 = endX + offsetX - arrowSize * cos(angle - PI / 6)
    val arrowY1 = endY + offsetY - arrowSize * sin(angle - PI / 6)
    val arrowX2 = endX + offsetX - arrowSize * cos(angle + PI / 6)
    val arrowY2 = endY + offsetY - arrowSize * sin(angle + PI / 6)

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )

    drawLine(
        color = color,
        start = Offset(endX + offsetX, endY + offsetY),
        end = Offset(arrowX1.toFloat(), arrowY1.toFloat()),
        strokeWidth = 2.dp.toPx()
    )
    drawLine(
        color = color,
        start = Offset(endX + offsetX, endY + offsetY),
        end = Offset(arrowX2.toFloat(), arrowY2.toFloat()),
        strokeWidth = 2.dp.toPx()
    )
}


sealed class BlockCategory {
    object Core : BlockCategory()
    object Variables : BlockCategory()
    object Arrays : BlockCategory()
}

val blockCategories = listOf(
    BlockCategory.Core,
    BlockCategory.Variables,
    BlockCategory.Arrays
)

val blockSubTypes = mapOf(
    BlockCategory.Core to listOf("Start", "End", "If"),
    BlockCategory.Variables to listOf("Declare Var", "Set Var", "Print Var"),
    BlockCategory.Arrays to listOf("Declare Array", "Set Array", "Print Array", "Get Array Element")
)

@Composable
fun BlockIcon(type: String) {
    when {
        type == "Start" -> Icon(Icons.Default.PlayArrow, null)
        type == "End" -> Icon(Icons.Default.Lock, null)
        type.contains("Var") -> Icon(Icons.Default.Star, null)
        type.contains("Array") -> Icon(Icons.Default.DateRange, null)
        type == "If" -> Icon(Icons.Default.Share, null)
        else -> Icon(Icons.Default.Home, null)
    }
}