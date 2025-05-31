package com.example.codeblockcommander.ui.theme
import AppState
import java.util.ArrayDeque
import kotlin.collections.mutableMapOf

fun Boolean.toInt() = if (this) 1 else 0
fun Boolean.toDouble() = if (this) 1.0 else 0.0;
fun Int.toBool() = if (this==0) false else true;
fun Double.toBool() = if (this==0.0) false else true;

fun Parser(expr: String, appState: AppState): String{
    var variables = appState.variables.mapValues { it.value.second }
    var expression = expr
    expression = expression.replace("\\.\\.+".toRegex(), ".")
    //val variables = mapOf("x" to 5.0, "y" to 4.0, "z" to 10.0)
    try {
        val postfix = infixToPostfix(expression)
        println("Постфикс (Обратная польская): $postfix")

        val result = evaluatePostfix(postfix, variables)
        println("Результат: $result")

        return result.toString()
    } catch (e: Exception) {
        println("Ошибка: ${e.message}")
    }
    return ""
}

//Обратная польская нотация (инфиксную запись в постфиксную)
fun infixToPostfix(infix: String): String {
    val output = StringBuilder()
    val stack = ArrayDeque<Char>()
    val precedence = mapOf('<' to 0, '>' to 0,'+' to 1, '-' to 1, '*' to 2, '/' to 2, '^' to 3, '~' to 4)
    var i = 0

    while (i < infix.length) {
        val token = infix[i]

        when {
            //Числа
            token.isDigit() || token == '.' -> {
                val number = StringBuilder()
                while (i < infix.length && (infix[i].isDigit() || infix[i] == '.')) {
                    number.append(infix[i++])
                }
                output.append(number).append(' ')
                i--
            }

            //Переменные
            token.isLetter() || token == '_' -> {
                val variable = StringBuilder()
                while (i < infix.length && (infix[i].isLetterOrDigit() || infix[i] == '_')) {
                    variable.append(infix[i++])
                }
                output.append(variable).append(' ')
                i--
            }

            //Унарный минус
            token == '-' && (i == 0 || infix[i - 1] in "<>+-*/^(") -> {
                //output.append("0 ")  // Для унарного минуса: "-x" → "0 x -"
                stack.push('~')
            }

            //Скобки
            token == '(' -> stack.push(token)
            token == ')' -> {
                while (stack.isNotEmpty() && stack.peek() != '(') {
                    output.append(stack.pop()).append(' ')
                }
                if (stack.isEmpty()) throw IllegalArgumentException("Несбалансированные скобки")
                stack.pop()
            }
            token in "<>+-*/^~" -> {
                while (stack.isNotEmpty() && stack.peek() != '(' &&
                    (precedence[stack.peek()] ?: 0) >= (precedence[token] ?: 0)) {
                    output.append(stack.pop()).append(' ')
                }
                stack.push(token)
            }
            token == ' ' -> {}
            else -> throw IllegalArgumentException("Недопустимый символ: '$token'")
        }
        i++
    }

    while (stack.isNotEmpty()) {
        if (stack.peek() == '(') throw IllegalArgumentException("Несбалансированные скобки")
        output.append(stack.pop()).append(' ')
    }

    return output.toString().trim()
}

//Из польской нотации в ответ
fun evaluatePostfix(postfix: String, variables: Map<String, Any?> = emptyMap()): Double {
    val stack = ArrayDeque<Double>()
    val tokens = postfix.split(" ").filter { it.isNotBlank() }

    for (token in tokens) {
        when {
            //Число
            token.toDoubleOrNull() != null -> stack.push(token.toDouble())

            //Переменная
            token in variables -> stack.push(variables[token].toString().toDouble())

            //Унарный минус
            token == "~" -> {
                if (stack.isEmpty()) throw IllegalArgumentException("Недостаточно операндов для унарного минуса")
                stack.push(-stack.pop())
            }

            //Операторы
            token in "<>+-*/^" -> {
                if (stack.size < 2) throw IllegalArgumentException("Недостаточно операндов для оператора '$token'")
                val b = stack.pop()
                val a = stack.pop()
                val result = when (token) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> if (b == 0.0) throw ArithmeticException("Деление на ноль") else a / b
                    "^" -> Math.pow(a, b)
                    "<" -> ((a < b).toDouble())
                    ">" -> ((a > b).toDouble())
                    else -> throw IllegalArgumentException("Неизвестный оператор: '$token'")
                }
                stack.push(result)
            }

            else -> throw IllegalArgumentException("Неизвестный токен: '$token'")
        }
    }

    if (stack.size != 1) throw IllegalArgumentException("Некорректное выражение")
    return stack.pop()
}

fun isCorrectName(NAME: String): Boolean
{
    if (NAME.length==0) return false;
    if (NAME[0].isDigit()) return false;
    return NAME.matches(Regex("[a-zA-Z0-9_$]+"));
}


