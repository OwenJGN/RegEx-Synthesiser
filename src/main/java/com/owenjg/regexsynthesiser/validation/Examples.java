package com.owenjg.regexsynthesiser.validation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages and processes example strings used for regular expression synthesis.
 * Handles both positive examples (strings that should match) and negative examples
 * (strings that should not match), either from user input or from files.
 */
public class Examples {

    /** List of strings that should match the generated regex */
    List<String> positiveExamples;

    /** List of strings that should not match the generated regex */
    List<String> negativeExamples;

    /**
     * Retrieves the current set of positive examples.
     *
     * @return List of strings that should match the regex
     */
    public List<String> getPositiveExamples(){
        return positiveExamples;
    }

    /**
     * Retrieves the current set of negative examples.
     *
     * @return List of strings that should not match the regex
     */
    public List<String> getNegativeExamples(){
        return negativeExamples;
    }

    /**
     * Processes raw input strings containing pipe-separated examples.
     * Splits and cleans the input strings to extract individual examples.
     *
     * @param positiveExamples Raw string containing pipe-separated positive examples
     * @param negativeExamples Raw string containing pipe-separated negative examples
     * @return A list containing two lists: positive examples and negative examples
     */
    public List<List<String>> splitPositiveAndNegativeInput(String positiveExamples, String negativeExamples){
        List<String> pos = splitExamples(positiveExamples);
        List<String> neg = splitExamples(negativeExamples);

        List<List<String>> list = new ArrayList<>();
        list.add(pos);
        list.add(neg);

        this.positiveExamples = pos;
        this.negativeExamples = neg;

        return list;
    }

    /**
     * Reads and processes examples from a file containing both positive and negative examples.
     * The file should have positive examples, followed by "::", followed by negative examples.
     *
     * @param filePath Path to the file containing examples
     * @return A list containing two lists: positive examples and negative examples
     * @throws IOException If the file cannot be read
     */
    public List<List<String>> splitPositiveAndNegativeFile(String filePath) throws IOException {
        String content = Files.readString(Path.of(filePath));

        String[] splitExamples = content.split("::");
        List<String> pos = splitExamples(splitExamples[0]);
        List<String> neg = splitExamples.length > 1 ? splitExamples(splitExamples[1]) : Collections.emptyList();

        List<List<String>> list = new ArrayList<>();
        list.add(pos);
        list.add(neg);

        this.positiveExamples = pos;
        this.negativeExamples = neg;
        return list;
    }

    /**
     * Splits a raw string of pipe-separated examples into a list of individual examples.
     * Trims whitespace and filters out empty strings.
     *
     * @param examples Raw string containing pipe-separated examples
     * @return List of individual, trimmed example strings
     */
    private List<String> splitExamples(String examples) {
        return Optional.ofNullable(examples)
                .map(ex -> Arrays.stream(ex.split("\\|"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    /**
     * Validates that a file containing examples has the correct format.
     * The file must contain the separator "::" between positive and negative examples.
     *
     * @param filePath Path to the file to validate
     * @return true if the file has valid format, false otherwise
     * @throws IOException If the file cannot be read
     */
    public boolean validateFileContent(String filePath) throws IOException {
        String content = Files.readString(Path.of(filePath));

        if(!content.contains("::")){
            return false;
        } else {
            String[] splitExamples = content.split("::");
            if (splitExamples.length == 0){
                return false;
            }
        }
        return true;
    }
}