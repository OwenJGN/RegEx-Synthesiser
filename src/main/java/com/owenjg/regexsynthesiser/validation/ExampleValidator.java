package com.owenjg.regexsynthesiser.validation;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates regular expressions against sets of positive and negative examples.
 * Ensures that generated regex patterns correctly match all the required strings
 * while avoiding matches with forbidden strings.
 */
public class ExampleValidator {

    /**
     * Validates a regular expression against sets of positive and negative examples.
     * A valid regex must match all positive examples and not match any negative examples.
     *
     * @param regex The regular expression to validate
     * @param positiveExamples List of strings that should match the regex
     * @param negativeExamples List of strings that should not match the regex
     * @return true if the regex matches all positive examples and no negative examples, false otherwise
     */
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