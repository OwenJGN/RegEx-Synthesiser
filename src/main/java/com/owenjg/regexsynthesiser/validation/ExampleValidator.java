package com.owenjg.regexsynthesiser.validation;

import java.util.List;
import java.util.regex.Pattern;

public class ExampleValidator {
    public boolean validateExamples(String regex, List<String> positiveExamples, List<String> negativeExamples) {
        try {
            // First translate any special characters
            String translatedRegex = translateGeneralizedRegex(regex);
            System.out.println("OUTPUT" +translatedRegex);
            // Compile the pattern
            Pattern pattern = Pattern.compile(translatedRegex);

            // Check positive examples
            for (String example : positiveExamples) {
                if (!pattern.matcher(example).matches()) {
                    System.out.println("Failed to match positive example: " + example);
                    return false;
                }
            }

            // Check negative examples
            for (String example : negativeExamples) {
                if (pattern.matcher(example).matches()) {
                    System.out.println("Incorrectly matched negative example: " + example);
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            System.err.println("Validation error: " + e.getMessage());
            return false;
        }
    }
    private String translateGeneralizedRegex(String regex) {
        StringBuilder result = new StringBuilder();
        boolean inGroup = false;
        int i = 0;

        while (i < regex.length()) {
            char c = regex.charAt(i);

            switch (c) {
                case '\\':
                    result.append("\\d");
                    break;
                case 'l':
                    result.append("[a-z]");
                    break;
                case 'u':
                    result.append("[A-Z]");
                    break;
                case '(':
                    inGroup = true;
                    result.append(c);
                    break;
                case ')':
                    inGroup = false;
                    result.append(c);
                    break;
                default:
                    // Handle special characters
                    if ("[]{}().*+?^$|".indexOf(c) != -1) {
                        result.append('\\').append(c);
                    } else {
                        result.append(c);
                    }
            }
            i++;
        }

        return result.toString();
    }
}
