package com.graficadora.model;

import java.util.*;
import java.util.function.DoubleUnaryOperator;

/**
 * Parser y evaluador de expresiones matemáticas científicas.
 * Soporta variables (x), constantes (pi, e, tau, phi), operadores (+, -, *, /, ^, %),
 * funciones trigonométricas, hiperbólicas, logarítmicas y especiales,
 * así como multiplicación implícita (ej. 2x, 3sin(x), (x+1)(x-2)).
 */
public class MathParser {

    public interface ExpressionNode {
        double evaluate(double x);
    }

    public static class NumberNode implements ExpressionNode {
        private final double value;
        public NumberNode(double value) { this.value = value; }
        @Override public double evaluate(double x) { return value; }
    }

    public static class VariableNode implements ExpressionNode {
        @Override public double evaluate(double x) { return x; }
    }

    public static class UnaryOpNode implements ExpressionNode {
        private final char op;
        private final ExpressionNode child;
        public UnaryOpNode(char op, ExpressionNode child) {
            this.op = op;
            this.child = child;
        }
        @Override
        public double evaluate(double x) {
            double v = child.evaluate(x);
            return (op == '-') ? -v : v;
        }
    }

    public static class BinaryOpNode implements ExpressionNode {
        private final char op;
        private final ExpressionNode left;
        private final ExpressionNode right;

        public BinaryOpNode(char op, ExpressionNode left, ExpressionNode right) {
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @Override
        public double evaluate(double x) {
            double a = left.evaluate(x);
            double b = right.evaluate(x);
            return switch (op) {
                case '+' -> a + b;
                case '-' -> a - b;
                case '*' -> a * b;
                case '/' -> a / b;
                case '%' -> a % b;
                case '^' -> Math.pow(a, b);
                default -> Double.NaN;
            };
        }
    }

    public static class FunctionNode implements ExpressionNode {
        private final String functionName;
        private final ExpressionNode argument;

        public FunctionNode(String functionName, ExpressionNode argument) {
            this.functionName = functionName.toLowerCase(Locale.ROOT);
            this.argument = argument;
        }

        @Override
        public double evaluate(double x) {
            double v = argument.evaluate(x);
            return switch (functionName) {
                case "sin", "sen" -> Math.sin(v);
                case "cos" -> Math.cos(v);
                case "tan", "tg" -> Math.tan(v);
                case "asin", "arcsin", "arcsen" -> Math.asin(v);
                case "acos", "arccos" -> Math.acos(v);
                case "atan", "arctan", "arctg" -> Math.atan(v);
                case "sinh" -> Math.sinh(v);
                case "cosh" -> Math.cosh(v);
                case "tanh" -> Math.tanh(v);
                case "sec" -> 1.0 / Math.cos(v);
                case "csc", "cosec" -> 1.0 / Math.sin(v);
                case "cot", "cotan", "ctg" -> 1.0 / Math.tan(v);
                case "sqrt", "raiz" -> Math.sqrt(v);
                case "cbrt" -> Math.cbrt(v);
                case "log", "log10" -> Math.log10(v);
                case "ln" -> Math.log(v);
                case "log2" -> Math.log(v) / Math.log(2.0);
                case "exp" -> Math.exp(v);
                case "abs" -> Math.abs(v);
                case "floor" -> Math.floor(v);
                case "ceil" -> Math.ceil(v);
                case "round" -> Math.round(v);
                case "sign", "sgn" -> Math.signum(v);
                case "sinc" -> (v == 0.0) ? 1.0 : Math.sin(v) / v;
                default -> Double.NaN;
            };
        }
    }

    // Tokenizer & Parser
    private static class Token {
        enum Type { NUMBER, IDENTIFIER, OPERATOR, LPAREN, RPAREN, EOF }
        final Type type;
        final String text;
        final double numberValue;

        Token(Type type, String text) {
            this.type = type;
            this.text = text;
            this.numberValue = 0;
        }
        Token(double value) {
            this.type = Type.NUMBER;
            this.text = String.valueOf(value);
            this.numberValue = value;
        }
    }

    private static String preprocess(String expr) {
        if (expr == null) return "";
        // Reemplazo de símbolos matemáticos estándar y unicode
        String s = expr.trim()
                .replace("²", "^2")
                .replace("³", "^3")
                .replace("π", "pi")
                .replace("√", "sqrt")
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace(",", ".");
        return s;
    }

    public static ExpressionNode parse(String rawExpression) throws IllegalArgumentException {
        String expr = preprocess(rawExpression);
        if (expr.isEmpty()) {
            throw new IllegalArgumentException("La expresión está vacía.");
        }

        List<Token> rawTokens = tokenize(expr);
        List<Token> tokens = insertImplicitMultiplication(rawTokens);

        ParserState state = new ParserState(tokens);
        ExpressionNode node = parseExpression(state);
        if (state.peek().type != Token.Type.EOF) {
            throw new IllegalArgumentException("Símbolo inesperado al final de la expresión: " + state.peek().text);
        }
        return node;
    }

    public static DoubleUnaryOperator compile(String rawExpression) throws IllegalArgumentException {
        ExpressionNode node = parse(rawExpression);
        return node::evaluate;
    }

    private static List<Token> tokenize(String expr) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int n = expr.length();

        while (i < n) {
            char c = expr.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            if (Character.isDigit(c) || (c == '.' && i + 1 < n && Character.isDigit(expr.charAt(i + 1)))) {
                int start = i;
                boolean hasDot = false;
                while (i < n && (Character.isDigit(expr.charAt(i)) || (!hasDot && expr.charAt(i) == '.'))) {
                    if (expr.charAt(i) == '.') hasDot = true;
                    i++;
                }
                // Revisar notación científica (ej. 1e-5 o 2.5E3)
                if (i < n && (expr.charAt(i) == 'e' || expr.charAt(i) == 'E')) {
                    int ePos = i;
                    i++;
                    if (i < n && (expr.charAt(i) == '+' || expr.charAt(i) == '-')) {
                        i++;
                    }
                    if (i < n && Character.isDigit(expr.charAt(i))) {
                        while (i < n && Character.isDigit(expr.charAt(i))) {
                            i++;
                        }
                    } else {
                        i = ePos; // No era notación científica
                    }
                }
                String numStr = expr.substring(start, i);
                try {
                    tokens.add(new Token(Double.parseDouble(numStr)));
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Número no válido: " + numStr);
                }
                continue;
            }

            if (Character.isLetter(c)) {
                int start = i;
                while (i < n && (Character.isLetterOrDigit(expr.charAt(i)) || expr.charAt(i) == '_')) {
                    i++;
                }
                tokens.add(new Token(Token.Type.IDENTIFIER, expr.substring(start, i)));
                continue;
            }

            if (c == '(') {
                tokens.add(new Token(Token.Type.LPAREN, "("));
                i++;
                continue;
            }
            if (c == ')') {
                tokens.add(new Token(Token.Type.RPAREN, ")"));
                i++;
                continue;
            }

            if ("+-*/^%".indexOf(c) != -1) {
                tokens.add(new Token(Token.Type.OPERATOR, String.valueOf(c)));
                i++;
                continue;
            }

            throw new IllegalArgumentException("Carácter no reconocido: '" + c + "'");
        }

        tokens.add(new Token(Token.Type.EOF, ""));
        return tokens;
    }

    private static List<Token> insertImplicitMultiplication(List<Token> tokens) {
        List<Token> result = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            Token current = tokens.get(i);
            result.add(current);

            if (i + 1 < tokens.size()) {
                Token next = tokens.get(i + 1);
                boolean shouldInsertMult = false;

                // Caso: número seguido de identificador o '(' (ej. 2x, 2sin(x), 2(x))
                if (current.type == Token.Type.NUMBER) {
                    if (next.type == Token.Type.IDENTIFIER || next.type == Token.Type.LPAREN) {
                        shouldInsertMult = true;
                    }
                }
                // Caso: ')' seguido de número, identificador o '(' (ej. (x+1)(x-1), (x)x, (x)2)
                else if (current.type == Token.Type.RPAREN) {
                    if (next.type == Token.Type.NUMBER || next.type == Token.Type.IDENTIFIER || next.type == Token.Type.LPAREN) {
                        shouldInsertMult = true;
                    }
                }
                // Caso: variable 'x' o constante seguida de '(' o identificador (ej. x sin(x))
                else if (current.type == Token.Type.IDENTIFIER) {
                    String id = current.text.toLowerCase(Locale.ROOT);
                    boolean isVariableOrConst = id.equals("x") || id.equals("pi") || id.equals("e") || id.equals("tau") || id.equals("phi");
                    if (isVariableOrConst) {
                        if (next.type == Token.Type.IDENTIFIER || next.type == Token.Type.LPAREN || next.type == Token.Type.NUMBER) {
                            shouldInsertMult = true;
                        }
                    }
                }

                if (shouldInsertMult) {
                    result.add(new Token(Token.Type.OPERATOR, "*"));
                }
            }
        }
        return result;
    }

    private static class ParserState {
        private final List<Token> tokens;
        private int pos = 0;

        ParserState(List<Token> tokens) {
            this.tokens = tokens;
        }

        Token peek() {
            return tokens.get(pos);
        }

        Token next() {
            return tokens.get(pos++);
        }

        boolean match(Token.Type type, String text) {
            Token t = peek();
            if (t.type == type && (text == null || t.text.equals(text))) {
                pos++;
                return true;
            }
            return false;
        }
    }

    private static ExpressionNode parseExpression(ParserState state) {
        return parseAddSub(state);
    }

    // Nivel 1: Suma y Resta (+, -)
    private static ExpressionNode parseAddSub(ParserState state) {
        ExpressionNode left = parseMulDivMod(state);
        while (true) {
            Token t = state.peek();
            if (t.type == Token.Type.OPERATOR && (t.text.equals("+") || t.text.equals("-"))) {
                state.next();
                ExpressionNode right = parseMulDivMod(state);
                left = new BinaryOpNode(t.text.charAt(0), left, right);
            } else {
                break;
            }
        }
        return left;
    }

    // Nivel 2: Multiplicación, División, Módulo (*, /, %)
    private static ExpressionNode parseMulDivMod(ParserState state) {
        ExpressionNode left = parseUnary(state);
        while (true) {
            Token t = state.peek();
            if (t.type == Token.Type.OPERATOR && (t.text.equals("*") || t.text.equals("/") || t.text.equals("%"))) {
                state.next();
                ExpressionNode right = parseUnary(state);
                left = new BinaryOpNode(t.text.charAt(0), left, right);
            } else {
                break;
            }
        }
        return left;
    }

    // Nivel 3: Operadores unarios (+, -)
    private static ExpressionNode parseUnary(ParserState state) {
        Token t = state.peek();
        if (t.type == Token.Type.OPERATOR && (t.text.equals("+") || t.text.equals("-"))) {
            state.next();
            ExpressionNode child = parseUnary(state);
            return new UnaryOpNode(t.text.charAt(0), child);
        }
        return parseExponent(state);
    }

    // Nivel 4: Exponente (^, asociatividad a la derecha)
    private static ExpressionNode parseExponent(ParserState state) {
        ExpressionNode base = parsePrimary(state);
        Token t = state.peek();
        if (t.type == Token.Type.OPERATOR && t.text.equals("^")) {
            state.next();
            ExpressionNode exp = parseUnary(state); // Permite -x^2 o 2^-x
            return new BinaryOpNode('^', base, exp);
        }
        return base;
    }

    // Nivel 5: Primarios (números, variables, constantes, funciones, paréntesis)
    private static ExpressionNode parsePrimary(ParserState state) {
        Token t = state.peek();

        if (t.type == Token.Type.NUMBER) {
            state.next();
            return new NumberNode(t.numberValue);
        }

        if (t.type == Token.Type.LPAREN) {
            state.next();
            ExpressionNode expr = parseExpression(state);
            if (!state.match(Token.Type.RPAREN, ")")) {
                throw new IllegalArgumentException("Falta paréntesis de cierre ')'");
            }
            return expr;
        }

        if (t.type == Token.Type.IDENTIFIER) {
            state.next();
            String name = t.text.toLowerCase(Locale.ROOT);

            // Variable x
            if (name.equals("x")) {
                return new VariableNode();
            }

            // Constantes
            switch (name) {
                case "pi" -> { return new NumberNode(Math.PI); }
                case "e" -> { return new NumberNode(Math.E); }
                case "tau" -> { return new NumberNode(Math.PI * 2.0); }
                case "phi" -> { return new NumberNode((1.0 + Math.sqrt(5.0)) / 2.0); }
            }

            // Llamada a función (ej. sin(x))
            if (state.peek().type == Token.Type.LPAREN) {
                state.next(); // Consume '('
                ExpressionNode arg = parseExpression(state);
                if (!state.match(Token.Type.RPAREN, ")")) {
                    throw new IllegalArgumentException("Falta paréntesis de cierre ')' en función " + name);
                }
                return new FunctionNode(name, arg);
            } else {
                throw new IllegalArgumentException("Función '" + name + "' debe ser seguida de argumentos entre paréntesis, ej. " + name + "(x)");
            }
        }

        throw new IllegalArgumentException("Expresión inesperada cerca de: '" + t.text + "'");
    }
}
