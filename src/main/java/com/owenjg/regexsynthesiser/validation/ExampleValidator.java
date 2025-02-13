package com.owenjg.regexsynthesiser.validation;

import java.util.List;
import java.util.regex.Pattern;

public class ExampleValidator {
    public boolean validateExamples(String regex, List<String> positiveExamples, List<String> negativeExamples) {
        try {
            Pattern pattern = Pattern.compile(regex);

            // Check all positive examples match
            boolean allPositiveMatch = positiveExamples.stream()
                    .allMatch(ex -> pattern.matcher(ex).matches());

            // Check no negative examples match
            boolean noNegativeMatch = negativeExamples == null || negativeExamples.isEmpty() ||
                    negativeExamples.stream()
                            .noneMatch(ex -> pattern.matcher(ex).matches());

            return allPositiveMatch && noNegativeMatch;
        } catch (Exception e) {
            return false;
        }
    }
}
