import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.codeblockcommander.ui.theme.Parser
import rememberAppState
import kotlin.collections.set

class AppState {
    val debuggerState = DebuggerState()
    val variables = mutableMapOf<String, Pair<String, Any?>>()
    //val variables = mutableMapOf<String, Pair<String, Any?>>()
    var currentBlockId by mutableStateOf<Int?>(null)

    var consoleText by mutableStateOf("Консоль выполнения:\n")
        private set

    fun appendToConsole(text: String) {
        consoleText += "$text\n"
    }

    fun clearConsole() {
        consoleText = "Консоль выполнения:\n"
    }


    var isDebugging by mutableStateOf(false)
    var currentDebugBlock by mutableStateOf<CodeBlock?>(null)
    var debugPaused by mutableStateOf(true) // Начинаем в paused режиме

    fun stopDebugging() {
        isDebugging = false
        currentDebugBlock = null
        debugPaused = true
    }

    // Добавление переменной с автоматической инициализацией
    fun addVariable(name: String, type: String, value: String? = null) {
        val actualValue = value ?: when (type) {
            "Int" -> 0
            "Double" -> 0.0
            "Boolean" -> true
            "String" -> ""
            else -> null
        }
        variables[name] = type to Parser(actualValue.toString())
    }

    // Обновление значения переменной
    fun updateVariable(name: String, value: String) {
        variables[name]?.let { (type, _) ->
            val new_value = Parser(value.toString())
            variables[name] = type to new_value
        }
    }

    // Получение типа переменной
    fun getVariableType(name: String): String? {
        return variables[name]?.first
    }


    val arrays = mutableMapOf<String, Pair<String, Array<Any?>>>()

    // Добавление массива
    fun addArray(name: String, type: String, size: Int) {
        val initialValue = when (type) {
            "Int" -> 0
            "Double" -> 0.0
            "Boolean" -> false
            "String" -> ""
            else -> null
        }
        arrays[name] = type to Array(size) { 0 }
    }

    // Установка элемента массива
    fun setArrayElement(name: String, index: Int, value: Any) {
        arrays[name]?.let { (type, array) ->
            when (type) {
                "Int" -> array[index] = value.toString().toInt()
                "Double" -> array[index] = value.toString().toDouble()
                "Boolean" -> array[index] = value.toString().toBoolean()
                else -> array[index] = value
            }
        }
    }

    // Получение элемента массива
    fun getArrayElement(name: String, index: Int): Any? {
        return arrays[name]?.second?.getOrNull(index)
    }


}

class ExecutionContext(
    val parent: ExecutionContext? = null,
    val variables: MutableMap<String, Pair<String, Any?>> = mutableMapOf()
) {
    fun getVariable(name: String): Any? {
        return variables[name]?.second ?: parent?.getVariable(name)
    }

    fun setVariable(name: String, type: String, value: Any?) {
        if (variables.containsKey(name) || parent == null) {
            variables[name] = type to value
        } else {
            parent.setVariable(name, type, value)
        }
    }
}


data class BlockConnection(
    val fromBlockId: Int,
    val fromSide: ConnectionSide,
    val toBlockId: Int,
    val toSide: ConnectionSide
)

enum class ConnectionSide { TOP, RIGHT, BOTTOM, LEFT }

data class CodeBlock(
    val id: Int,
    val type: String,
    val x: Float,
    val y: Float,
    val params: Map<String, String> = emptyMap(),
    val connections: List<BlockConnection> = emptyList(),
    val childBlocks: MutableMap<String, List<CodeBlock>> = mutableMapOf(),
    val appState: AppState
) {
    fun generateCode(): String {
        return when (type) {
            "Start" -> "// Начало программы"
            "End" -> "// Конец программы"
            "Print Var" -> "println(\"${params["text"]}\")"
            "Declare Var" -> "var ${params["varName"]} = ${params["varValue"]}"
            "Set Var" -> "${params["varName"]} = ${params["varValue"]}"

            "Declare Array" -> "val ${params["arrayName"]} = Array<${params["varType"]}>(${params["arraySize"]})"
            "Set Array" -> "${params["arrayName"]}[${params["arrayId"]}] = ${params["arrayValue"]}"
            "Print Array" -> "println(${params["text"]}.contentToString())"

            "If" -> "${params["leftExpr"]} ${params["condition"]} ${params["rightExpr"]}"
            else -> "// $type блок"
        }
    }


    fun execute() {
        //return "${this.x} ${this.y} ${this.type}\n"
        when(type) {
            "Start" -> {

            }

            "Declare Var" -> {
                appState.addVariable(params["varName"].toString(), type, params["varValue"])
            }

            "Set Var" -> {
                if (appState.variables.containsKey(params["varName"].toString())) {
                    appState.updateVariable(
                        params["varName"].toString(),
                        params["varValue"].toString()
                    )
                } else {
                    appState.appendToConsole("Переменной ${params["varName"]} не существует")
                }
            }
            "Print Var" -> {
                if (params["isVariable"]?.toBooleanStrict() == true) {
                    //appState.appendToConsole(params["text"].toString())
                    if (appState.variables.containsKey(params["text"].toString())) {
                        appState.appendToConsole(appState.variables[params["text"].toString()]?.second.toString())
                    }
                }
                else {
                    appState.appendToConsole("${params["text"]}")
                }
            }

            "Declare Array" -> {
                val name = params["arrayName"] ?: ""
                val type = params["varType"] ?: "Int"
                val size = params["arraySize"]?.toIntOrNull() ?: 0
                appState.addArray(name, type, size)
            }

            "Set Array" -> {
                val name = params["arrayName"] ?: ""
                val index = params["arrayId"]?.toIntOrNull() ?: 0
                val value = params["arrayValue"] ?: ""
                appState.setArrayElement(name, index, value)
            }

            "Print Array" -> {
                val name = params["arrayName"] ?: ""
                val array = appState.arrays[name]?.second
                //appState.appendToConsole("$name: ${array?.contentToString()}")
                appState.appendToConsole("${array.toString()}")
            }

        }

    }

}

@Composable
fun rememberAppState(): AppState {
    return remember { AppState() }
}



class DebuggerState {
    var breakpoints = mutableSetOf<Int>() // ID блоков с точками останова
    var executionSpeed by mutableStateOf(1f) // Скорость выполнения
    var callStack = mutableListOf<Int>() // Стек вызовов
}
