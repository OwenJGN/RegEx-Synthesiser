package com.owenjg.regexsynthesiser.synthesis;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PatternGeneralizer {
    private static final Set<Character> SPECIAL_CHARS = new HashSet<>(Arrays.asList(
            '#', '@', '.', '-', '_', '!', '$', '%', '&', '*', '+', '=', '|', '\\',
            ':', ';', '/', '~'
    ));

    public String generalizePattern(List<String> examples) {
        // Parse examples into structure
        List<List<Token>> tokenizedExamples = examples.stream()
                .map(this::tokenize)
                .collect(Collectors.toList());

        // Convert to pattern
        return buildPattern(tokenizedExamples);
    }

    private List<Token> tokenize(String example) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        TokenType currentType = null;

        for (char c : example.toCharArray()) {
            TokenType type = getCharacterType(c);

            if (type != currentType && current.length() > 0) {
                tokens.add(new Token(currentType, current.toString()));
                current = new StringBuilder();
            }

            current.append(c);
            currentType = type;
        }

        if (current.length() > 0) {
            tokens.add(new Token(currentType, current.toString()));
        }

        return tokens;
    }

    private TokenType getCharacterType(char c) {
        if (SPECIAL_CHARS.contains(c)) {
            return TokenType.SPECIAL;
        } else if (Character.isUpperCase(c)) {
            return TokenType.UPPERCASE;
        } else if (Character.isLowerCase(c)) {
            return TokenType.LOWERCASE;
        } else if (Character.isDigit(c)) {
            return TokenType.DIGIT;
        }
        return TokenType.OTHER;
    }

    private String buildPattern(List<List<Token>> tokenizedExamples) {
        if (tokenizedExamples.isEmpty()) return "";

        List<Token> firstExample = tokenizedExamples.get(0);
        StringBuilder pattern = new StringBuilder("^");

        for (int i = 0; i < firstExample.size(); i++) {
            int finalI1 = i;
            Set<String> allValues = tokenizedExamples.stream()
                    .map(tokens -> tokens.get(finalI1).value)
                    .collect(Collectors.toSet());

            TokenType type = firstExample.get(i).type;

            // Handle each token type
            switch (type) {
                case SPECIAL:
                    pattern.append("\\").append(firstExample.get(i).value);
                    break;

                case UPPERCASE:
                    int finalI = i;
                    if (i == 0 && firstExample.size() > 1 &&
                            tokenizedExamples.stream().allMatch(tokens ->
                                    tokens.get(finalI +1).type == TokenType.LOWERCASE)) {
                        // Word pattern detected
                        pattern.append("[A-Z][a-z]+");
                        i++; // Skip next token as we've handled it
                    } else {
                        pattern.append(getUppercasePattern(allValues));
                    }
                    break;

                case LOWERCASE:
                    pattern.append(getLowercasePattern(allValues));
                    break;

                case DIGIT:
                    pattern.append(getDigitPattern(allValues));
                    break;

                case OTHER:
                    pattern.append(Pattern.quote(firstExample.get(i).value));
                    break;
            }
        }

        pattern.append("$");
        return pattern.toString();
    }

    private String getUppercasePattern(Set<String> values) {
        if (values.size() == 1) {
            return Pattern.quote(values.iterator().next());
        }
        return "[A-Z]+";
    }

    private String getLowercasePattern(Set<String> values) {
        // If all values are the same length and very short (like "com")
        boolean allSameLength = values.stream()
                .mapToInt(String::length)
                .distinct()
                .count() == 1;
        int firstLength = values.iterator().next().length();

        if (allSameLength && firstLength <= 3) {
            return "[a-z]{" + firstLength + "}";
        }
        return "[a-z]+";
    }

    private String getDigitPattern(Set<String> values) {
        // If all values are the same length
        boolean allSameLength = values.stream()
                .mapToInt(String::length)
                .distinct()
                .count() == 1;

        if (allSameLength) {
            return "\\d{" + values.iterator().next().length() + "}";
        }
        return "\\d+";
    }

    private enum TokenType {
        SPECIAL,
        UPPERCASE,
        LOWERCASE,
        DIGIT,
        OTHER
    }

    private static class Token {
        final TokenType type;
        final String value;

        Token(TokenType type, String value) {
            this.type = type;
            this.value = value;
        }
    }
}