package my.reqqpe.rseller.utils;

import org.bukkit.Bukkit;

import java.util.regex.Pattern;

public class SyntaxParser {

    public enum Type {
        AUTO, STRING, INTEGER
    }

    public static boolean parse(String syntax, Type type) {
        if (syntax == null || syntax.isEmpty()) return false;

        syntax = syntax.trim();
        String[] operators = {">=", "<=", "==", "!=", ">", "<"};

        for (String op : operators) {
            if (syntax.contains(op)) {
                String[] parts = syntax.split(Pattern.quote(op), 2);
                if (parts.length != 2) return false;

                String leftRaw = parts[0].trim();
                String rightRaw = parts[1].trim();

                String leftStr = removeQuotes(leftRaw);
                String rightStr = removeQuotes(rightRaw);

                Double leftNum = tryParseDouble(leftStr);
                Double rightNum = tryParseDouble(rightStr);

                if (type == Type.INTEGER) {
                    if (leftNum == null || rightNum == null) {
                        printTypeError(leftStr, rightStr, "Expected both to be numbers");
                        return false;
                    }

                    return compareNumbers(leftNum, rightNum, op);
                }

                if (type == Type.STRING) {
                    return compareStrings(leftStr, rightStr, op);
                }

                if (leftNum != null && rightNum != null) {
                    return compareNumbers(leftNum, rightNum, op);
                } else if (leftNum == null && rightNum == null) {
                    return compareStrings(leftStr, rightStr, op);
                } else {
                    printTypeError(leftStr, rightStr, "Cannot compare number with string");
                    return false;
                }
            }
        }

        Bukkit.getLogger().warning("Error: No valid operator found in expression: " + syntax);
        return false;
    }

    private static boolean compareNumbers(double left, double right, String op) {
        return switch (op) {
            case ">=" -> left >= right;
            case "<=" -> left <= right;
            case "==" -> left == right;
            case "!=" -> left != right;
            case ">" -> left > right;
            case "<" -> left < right;
            default -> false;
        };
    }

    private static boolean compareStrings(String left, String right, String op) {
        int cmp = left.compareTo(right);
        return switch (op) {
            case "==" -> left.equals(right);
            case "!=" -> !left.equals(right);
            case ">" -> cmp > 0;
            case "<" -> cmp < 0;
            case ">=" -> cmp >= 0;
            case "<=" -> cmp <= 0;
            default -> false;
        };
    }

    private static String removeQuotes(String input) {
        input = input.trim();
        if ((input.startsWith("\"") && input.endsWith("\"")) ||
                (input.startsWith("'") && input.endsWith("'"))) {
            return input.substring(1, input.length() - 1);
        }
        return input;
    }

    private static Double tryParseDouble(String input) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void printTypeError(String left, String right, String message) {
        Bukkit.getLogger().warning("Type mismatch: \"" + left + "\" vs \"" + right + "\" — " + message);
    }
}
