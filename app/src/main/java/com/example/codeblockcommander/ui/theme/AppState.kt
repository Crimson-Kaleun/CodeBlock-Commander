import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class AppState {
    var consoleText by mutableStateOf("Консоль выполнения:\n")
        private set

    fun appendToConsole(text: String) {
        consoleText += "$text\n"
    }

    fun clearConsole() {
        consoleText = "Консоль выполнения:\n"
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
    val connections: List<BlockConnection> = emptyList()
) {
    fun generateCode(): String {
        return when (type) {
            "Start" -> "// Начало программы"
            "End" -> "// Конец программы"
            "Print" -> "println(\"${params["text"]}\")"
            "Declare" -> "var ${params["varName"]} = ${params["varValue"]}"
            "Set" -> "${params["varName"]} = ${params["varValue"]}"
            "If" -> "if (${params["leftExpr"]} ${params["condition"]} ${params["rightExpr"]}) { ... }"
            else -> "// $type блок"
        }
    }
}

@Composable
fun rememberAppState(): AppState {
    return remember { AppState() }
}