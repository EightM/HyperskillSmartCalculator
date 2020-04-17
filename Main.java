package calculator;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Main {

    private static final HashMap<String, BigInteger> vars = new HashMap<>();
    private static final Pattern varNamePattern = Pattern.compile("[a-zA-Z]+");
    private static final HashMap<String, Integer> operatorWeights = new HashMap<>();
    private static final Pattern operandPattern = Pattern.compile("[\\da-zA-Z]");
    private static final Pattern operatorPattern = Pattern.compile("[+\\-*/]");
    private static final Pattern spacePattern = Pattern.compile("\\s+");
    static {
        operatorWeights.put("*", 100);
        operatorWeights.put("/", 100);
        operatorWeights.put("+", 10);
        operatorWeights.put("-", 10);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();

        while (!"/exit".equals(input)) {

            String userAction = "";

            if (input.startsWith("/")) {
                userAction = "command";
            } else if (varNamePattern.matcher(input).matches()) {
                userAction = "printVar";
            } else if (input.contains("=")) {
                userAction = "assignVar";
            } else if (input.isEmpty()) {
                userAction = "skip";
            } else {
                userAction = "calculate";
            }

            switch (userAction) {
                case "command":
                    handleCommands(input);
                    break;
                case "printVar":
                    printVar(input);
                    break;
                case "assignVar":
                    try {
                        assignVar(input);
                    } catch (Exception e) {
                        System.out.println("Invalid assignment");
                    }
                    break;
                case "calculate":
                    try {
                        System.out.println(calculate(input));
                    } catch (Exception e) {
                        System.out.println("Invalid expression");
                    }
                    break;
            }

            input = scanner.nextLine();

        }
        System.out.println("Bye!");
    }

    private static void printVar(String input) {

        if (!vars.containsKey(input)) {
            System.out.println("Unknown variable");
            return;
        }

        System.out.println(vars.get(input));
    }

    private static void assignVar(String input) {
        String[] var = input.split("\\s*=\\s*");
        if (var.length != 2) {
            throw new NumberFormatException();
        }

        Pattern varValuePattern = Pattern.compile("[\\da-zA-Z]+");
        if (!varNamePattern.matcher(var[0]).matches()) {
            System.out.println("Invalid identifier");
            return;
        }

        BigInteger value = BigInteger.ZERO;
        if (varNamePattern.matcher(var[1]).matches() && vars.containsKey(var[1])) {
            value = vars.get(var[1]);
        } else if (!varValuePattern.matcher(var[1]).matches()) {
            System.out.println("Invalid assignment");
            return;
        } else {
            value = new BigInteger(var[1]);
        }

        vars.put(var[0], value);

    }

    private static void handleCommands(String input) {

        if ("/help".equals(input)) {
            System.out.println("The program calculates the sum of numbers");
        } else {
            System.out.println("Unknown command");
        }
    }

    private static BigInteger calculate(String input) {
        String[] postfixInput = transformToPostfix(input);
        ArrayDeque<BigInteger> operandsSlack = new ArrayDeque<>();

        StringBuilder currentOperand = new StringBuilder();
        for (String symbol : postfixInput) {

            if (operandPattern.matcher(symbol).matches()) {
                currentOperand.append(symbol);
            } else if (spacePattern.matcher(symbol).matches()) {
                operandsSlack.offerLast(getBigIntOperand(currentOperand.toString()));
                currentOperand = new StringBuilder();
            } else if (varNamePattern.matcher(symbol).matches()) {
                currentOperand.append(symbol);
            } else if (operatorPattern.matcher(symbol).matches()) {
                if (currentOperand.length() > 0) {
                    operandsSlack.offerLast(getBigIntOperand(currentOperand.toString()));
                    currentOperand = new StringBuilder();
                }
                BigInteger secondOperand = Objects.requireNonNullElse(operandsSlack.pollLast(), BigInteger.ZERO);
                BigInteger firstOperand = Objects.requireNonNullElse(operandsSlack.pollLast(), BigInteger.ZERO);
                BigInteger result = calculateValue(firstOperand, secondOperand, symbol);
                operandsSlack.offerLast(result);
            }
        }

        return Objects.requireNonNullElse(operandsSlack.pollLast(), BigInteger.ZERO);
    }

    private static BigInteger calculateValue(BigInteger firstOperand, BigInteger secondOperand, String symbol) {

        BigInteger result;
        switch (symbol) {
            case "+":
                result = firstOperand.add(secondOperand);
                break;
            case "-":
                result = firstOperand.subtract(secondOperand);
                break;
            case "*":
                result = firstOperand.multiply(secondOperand);
                break;
            case "/":
                if (!secondOperand.equals(BigInteger.ZERO)) {
                    result = firstOperand.divide(secondOperand);
                } else {
                    result = BigInteger.ZERO;
                }
                break;
            default:
                throw new UnsupportedOperationException();

        }

        return result;
    }

    private static BigInteger getBigIntOperand(String operand) {
        if (varNamePattern.matcher(operand).matches() && vars.containsKey(operand)) {
            return vars.get(operand);
        }

        return new BigInteger(operand);
    }

    private static String[] transformToPostfix(String input) {
        input = collapseExtraCharacters(input);

        ArrayDeque<String> operatorStack = new ArrayDeque<>();
        StringBuilder postfixResult = new StringBuilder();

        StringBuilder currentOperand = new StringBuilder();
        String[] symbols = input.split("");
        for (String symbol : symbols) {

            if (operandPattern.matcher(symbol).matches()) {
                currentOperand.append(symbol);
            } else {
                if (currentOperand.length() > 0) {
                    currentOperand.append(" ");
                    postfixResult.append(currentOperand);
                    currentOperand = new StringBuilder();
                }
            }

            if (operatorPattern.matcher(symbol).matches()) {
                processOperator(operatorStack, postfixResult, symbol);
            } else if (symbol.equals(")")) {
                processBracketStatement(operatorStack, postfixResult);
            } else if (symbol.equals("(")) {
                operatorStack.offerLast(symbol);
            }
        }

        postfixResult.append(currentOperand);
        while (!operatorStack.isEmpty()) {
            String operator = operatorStack.pollLast();
            if (operator.equals("(")) {
                throw new UnsupportedOperationException();
            }
            postfixResult.append(operator);
        }

        return postfixResult.toString().trim().split("");
    }

    private static void processBracketStatement(ArrayDeque<String> operatorStack, StringBuilder postfixResult) {
        String currentOperator = operatorStack.peekLast();
        if (currentOperator == null) {
            throw new UnsupportedOperationException();
        }

        while (!currentOperator.equals("(")) {
            postfixResult.append(operatorStack.pollLast());
            currentOperator = operatorStack.peekLast();
            if (currentOperator == null) {
                throw new UnsupportedOperationException();
            }
        }
        operatorStack.pollLast();
    }

    private static void processOperator(ArrayDeque<String> operatorStack, StringBuilder postfixResult, String symbol) {

        if ((operatorStack.isEmpty() || operatorStack.peekLast().equals("("))
                || (operatorWeights.get(symbol) > operatorWeights.get(operatorStack.peekLast()))) {
            operatorStack.offerLast(symbol);

        } else if (operatorWeights.get(symbol) <= operatorWeights.get(operatorStack.peekLast())) {

            while (!operatorStack.isEmpty()
                    && !operatorStack.peekLast().equals("(")
                    && operatorWeights.get(operatorStack.peekLast()) >= operatorWeights.get(symbol)) {
                postfixResult.append(operatorStack.pollLast());
            }
            operatorStack.offerLast(symbol);
        }
    }

    private static String collapseExtraCharacters(String input) {
        input = input.replaceAll("\\s+", "");
        input = input.replaceAll("-{2}", "+");
        input = input.replaceAll("\\+{2,}", "+");
        input = input.replaceAll("\\+-", "-");
        return input;
    }

    private static String deleteNonNumeric(String operand) {
        return operand.replaceAll("[+-]", "");
    }
}
