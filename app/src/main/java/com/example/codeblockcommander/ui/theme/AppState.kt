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
            "Print" -> "println(\"${params["text"]}\")"
            "Declare" -> "var ${params["varName"]} = ${params["varValue"]}"
            "Set" -> "${params["varName"]} = ${params["varValue"]}"
            "If" -> "${params["leftExpr"]} ${params["condition"]} ${params["rightExpr"]}"
            else -> "// $type блок"
        }
    }


    fun execute() {
        //return "${this.x} ${this.y} ${this.type}\n"
        when(type) {
            "Start" -> {

            }

            "Declare" -> {
                appState.addVariable(params["varName"].toString(), type, params["varValue"])
            }

            "Set" -> {
                if (appState.variables.containsKey(params["varName"].toString())) {
                    appState.updateVariable(
                        params["varName"].toString(),
                        params["varValue"].toString()
                    )
                } else {
                    appState.appendToConsole("Переменной ${params["varName"]} не существует")
                }
            }
            "Print" -> {
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
