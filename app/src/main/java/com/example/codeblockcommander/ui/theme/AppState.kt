import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.codeblockcommander.ui.theme.Parser
import com.example.codeblockcommander.ui.theme.isCorrectName
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

    fun addVariable(name: String, type: String, value: String? = null) {
        val actualValue = value ?: when (type) {
            "Int" -> 0
            "Double" -> 0.0
            "Boolean" -> true
            "String" -> ""
            else -> null
        }
        variables[name] = type to Parser(actualValue.toString(), this)
    }

    fun updateVariable(name: String, value: String) {
        variables[name]?.let { (type, _) ->
            val new_value = Parser(value.toString(), this)
            variables[name] = type to new_value
        }
    }

    fun getVariableType(name: String): String? {
        return variables[name]?.first
    }


    val arrays = mutableMapOf<String, Pair<String, Array<Any?>>>()

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

    fun setArrayElement(name: String, indexStr: String, valueStr: String) {
        arrays[name]?.let { (type, array) ->
            val index = try {
                indexStr.toDouble().toInt()
            } catch (e: NumberFormatException) {
                variables[indexStr]?.second?.toString()?.toDouble()?.toInt() ?: run {
                    appendToConsole("Ошибка: неверный индекс '$indexStr'")
                    return
                }
            }

            if (index !in array.indices) {
                appendToConsole("Ошибка: индекс $index выходит за границы массива")
                return
            }

            val value = try {
                when (type) {
                    "IntArray" ->
                        valueStr.toIntOrNull() ?: variables[valueStr]?.second?.toString()?.toInt()
                    "DoubleArray" ->
                        valueStr.toDoubleOrNull() ?: variables[valueStr]?.second?.toString()?.toDouble()
                    "BooleanArray" ->
                        valueStr.toBooleanStrictOrNull() ?: variables[valueStr]?.second?.toString()?.toBoolean()
                    else ->
                        variables[valueStr]?.second ?: valueStr
                }
            } catch (e: Exception) {
                appendToConsole("Ошибка преобразования значения '$valueStr': ${e.message}")
                return
            }

            if (value == null) {
                appendToConsole("Ошибка: неверное значение '$valueStr'")
                return
            }

            try {
                when (type) {
                    "IntArray" -> array[index] = value as Int
                    "DoubleArray" -> array[index] = value as Double
                    "BooleanArray" -> array[index] = value as Boolean
                    else -> array[index] = value
                }
                appendToConsole("Установлено: $name[$index] = $value")
            } catch (e: ClassCastException) {
                appendToConsole("Ошибка: несоответствие типа значения для массива $name")
            }
        } ?: appendToConsole("Ошибка: массив '$name' не найден")
    }


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
                val index = params["arrayId"] ?: ""
                val value = params["arrayValue"] ?: ""
                appState.setArrayElement(name, index, value)
            }

            "Print Array" -> {
                val name = params["arrayName"] ?: ""
                val array = appState.arrays[name]?.second
                //appState.appendToConsole("$name: ${array?.contentToString()}")
                appState.appendToConsole("${array.toString()}")
            }
            "Get Array Element" -> {
                val targetVar = params["varName"] ?: ""
                val arrayName = params["arrayName"] ?: ""
                val indexStr = params["arrayId"] ?: "0"

                if (targetVar !in appState.variables) {
                    appState.appendToConsole("Ошибка: переменная '$targetVar' не объявлена")
                    return@execute
                }

                val index = try {
                    indexStr.toIntOrNull() ?: appState.variables[indexStr]?.second.toString().toInt()
                } catch (e: Exception) {
                    appState.appendToConsole("Ошибка: неверный индекс '$indexStr'")
                    return@execute
                }

                val value = appState.arrays[arrayName]?.second?.getOrNull(index) ?: run {
                    appState.appendToConsole("Ошибка: массив '$arrayName' или индекс $index не существует")
                    return@execute
                }

                appState.variables[targetVar] = appState.variables[targetVar]!!.first to value
                appState.appendToConsole("Успешно: $targetVar = $arrayName[$index] = $value")
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
