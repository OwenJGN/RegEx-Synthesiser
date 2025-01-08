package com.owenjg.regexsynthesiser.validation;

import com.owenjg.regexsynthesiser.exceptions.RegexSynthesisException;

import java.util.List;
import java.util.regex.Pattern;

// Class for validating examples
public class ExampleValidator {
    public void validateExamples(String regex, List<String> positiveExamples,
                                 List<String> negativeExamples) throws RegexSynthesisException {
        Pattern pattern = Pattern.compile(regex);

        // Validate positive examples
        for (String example : positiveExamples) {
            if (!pattern.matcher(example).matches()) {
                throw new RegexSynthesisException(
                        "Regex does not match positive example: " + example);
            }
        }

        // Validate negative examples
        if (negativeExamples != null) {
            for (String example : negativeExamples) {
                if (pattern.matcher(example).matches()) {
                    throw new RegexSynthesisException(
                            "Regex incorrectly matches negative example: " + example);
                }
            }
        }
    }
}
