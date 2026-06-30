package com.example.util

import kotlin.math.*

class MathParser(private val useRadians: Boolean = true) {

    fun evaluate(expression: String): Double {
        val cleanExpr = sanitize(expression)
        if (cleanExpr.isEmpty()) return 0.0
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < cleanExpr.length) cleanExpr[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < cleanExpr.length) throw IllegalArgumentException("Unexpected character: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else break
                }
                return x
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor
                    } else if (eat('%'.code)) {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Modulo by zero")
                        x %= divisor
                    } else break
                }
                return x
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    if (!eat(')'.code)) throw IllegalArgumentException("Missing closing parenthesis")
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    val numStr = cleanExpr.substring(startPos, pos)
                    x = numStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number: $numStr")
                } else if (ch >= 'a'.code && ch <= 'z'.code || ch == 'π'.code || ch == 'e'.code) {
                    while (ch >= 'a'.code && ch <= 'z'.code || ch == 'π'.code || ch == 'e'.code) nextChar()
                    val name = cleanExpr.substring(startPos, pos)
                    if (name == "π" || name == "pi") {
                        x = PI
                    } else if (name == "e") {
                        x = Math.E
                    } else {
                        if (!eat('('.code)) {
                            val arg = parseFactor()
                            x = evaluateFunction(name, arg)
                        } else {
                            val arg = parseExpression()
                            if (!eat(')'.code)) throw IllegalArgumentException("Missing closing parenthesis for $name")
                            x = evaluateFunction(name, arg)
                        }
                    }
                } else {
                    throw IllegalArgumentException("Unexpected character: " + ch.toChar())
                }

                if (eat('^'.code)) {
                    x = x.pow(parseFactor())
                }
                
                if (eat('!'.code)) {
                    x = factorial(x)
                }

                return x
            }

            private fun evaluateFunction(func: String, arg: Double): Double {
                return when (func) {
                    "sin" -> {
                        val angle = if (useRadians) arg else Math.toRadians(arg)
                        sin(angle)
                    }
                    "cos" -> {
                        val angle = if (useRadians) arg else Math.toRadians(arg)
                        cos(angle)
                    }
                    "tan" -> {
                        val angle = if (useRadians) arg else Math.toRadians(arg)
                        tan(angle)
                    }
                    "asin" -> {
                        val rad = asin(arg)
                        if (useRadians) rad else Math.toDegrees(rad)
                    }
                    "acos" -> {
                        val rad = acos(arg)
                        if (useRadians) rad else Math.toDegrees(rad)
                    }
                    "atan" -> {
                        val rad = atan(arg)
                        if (useRadians) rad else Math.toDegrees(rad)
                    }
                    "sqrt" -> {
                        if (arg < 0) throw IllegalArgumentException("Square root of negative number")
                        sqrt(arg)
                    }
                    "log" -> {
                        if (arg <= 0) throw IllegalArgumentException("Logarithm of non-positive number")
                        log10(arg)
                    }
                    "ln" -> {
                        if (arg <= 0) throw IllegalArgumentException("Natural logarithm of non-positive number")
                        ln(arg)
                    }
                    "abs" -> abs(arg)
                    else -> throw IllegalArgumentException("Unknown function: $func")
                }
            }

            private fun factorial(n: Double): Double {
                if (n < 0.0) throw IllegalArgumentException("Factorial of negative number")
                val intN = n.toInt()
                if (intN.toDouble() != n) throw IllegalArgumentException("Factorial of non-integer")
                if (intN > 170) throw IllegalArgumentException("Factorial overflow")
                var result = 1.0
                for (i in 1..intN) {
                    result *= i
                }
                return result
            }
        }.parse()
    }

    private fun sanitize(expr: String): String {
        return expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", "π")
            .replace("pow", "^")
    }
}
