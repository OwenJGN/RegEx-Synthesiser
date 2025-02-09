package com.owenjg.regexsynthesiser.validation;

import java.util.List;
import java.util.regex.Pattern;

public class ExampleValidator {
    public boolean validateExamples(String regex, List<String> positiveExamples, List<String> negativeExamples) {
        Pattern pattern = Pattern.compile(regex);

        for (String example : positiveExamples) {
            if (!pattern.matcher(example).matches()) {
                return false;
            }
        }

        for (String example : negativeExamples) {
            if (pattern.matcher(example).matches()) {
                return false;
            }
        }

        return true;
    }
}