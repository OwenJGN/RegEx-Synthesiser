package com.owenjg.regexsynthesiser;

import com.owenjg.regexsynthesiser.validation.Examples;
import com.owenjg.regexsynthesiser.validation.ExampleValidator;
import com.owenjg.regexsynthesiser.validation.RegexComparator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationTest {

    private ExampleValidator validator;
    private Examples examples;

    @BeforeEach
    void setUp() {
        validator = new ExampleValidator();
        examples = new Examples();
    }

    @Test
    void testExampleValidator() {
        // Test regex with positive examples
        String regex = "a[bc]d";
        List<String> positiveExamples = Arrays.asList("abd", "acd");
        List<String> negativeExamples = Arrays.asList("abc", "adc", "abcd");

        assertTrue(validator.validateExamples(regex, positiveExamples, Collections.emptyList()));
        assertTrue(validator.validateExamples(regex, positiveExamples, negativeExamples));

        // Add a failing positive example
        List<String> failingPositive = Arrays.asList("abd", "acd", "axd");
        assertFalse(validator.validateExamples(regex, failingPositive, negativeExamples));

        // Test with an invalid regex
        assertFalse(validator.validateExamples("[invalid", positiveExamples, negativeExamples));
    }

    @Test
    void testRegexComparator() {
        String regex1 = "a[bc]d";
        String regex2 = "(abd|acd)";

        // Test comparison text formatting
        String comparison = RegexComparator.compareRegexes(regex1, regex2);
        assertTrue(comparison.contains("Pattern Analyser: " + regex1));
        assertTrue(comparison.contains("DFA-based: " + regex2));

        // Test null handling
        comparison = RegexComparator.compareRegexes(null, regex2);
        assertTrue(comparison.contains("Pattern Analyser: N/A"));
        assertTrue(comparison.contains("DFA-based: " + regex2));

        comparison = RegexComparator.compareRegexes(regex1, null);
        assertTrue(comparison.contains("Pattern Analyser: " + regex1));
        assertTrue(comparison.contains("DFA-based: N/A"));
    }

    @Test
    void testLengthRatio() {
        // Test length ratio calculation
        double ratio = RegexComparator.getLengthRatio("abc", "abcdef");
        assertEquals(0.5, ratio, 0.001);

        // Test with null or empty values
        assertEquals(0.0, RegexComparator.getLengthRatio(null, "abc"));
        assertEquals(0.0, RegexComparator.getLengthRatio("abc", null));
        assertEquals(0.0, RegexComparator.getLengthRatio("abc", ""));
    }

    @Test
    void testComplexityRatio() {
        // Test complexity based on special characters
        double ratio = RegexComparator.getComplexityRatio("a[bc]*", "a(b|c)*");
        // a[bc]* has 3 special chars ([, ], *)
        // a(b|c)* has 5 special chars ((, ), |, ), *)
        assertEquals(0.75, ratio, 0.001);

        // Test with null values
        assertEquals(0.0, RegexComparator.getComplexityRatio(null, "abc"));
        assertEquals(0.0, RegexComparator.getComplexityRatio("abc", null));

        // Test with no special chars in denominators
        assertEquals(Double.POSITIVE_INFINITY, RegexComparator.getComplexityRatio("a*b+c", "abc"));
        assertEquals(1.0, RegexComparator.getComplexityRatio("abc", "def"));
    }

    @Test
    void testSplitExamples() {
        // Test splitting input strings
        List<List<String>> result = examples.splitPositiveAndNegativeInput(
                "abc|def|ghi", "jkl|mno");

        assertEquals(Arrays.asList("abc", "def", "ghi"), result.get(0));
        assertEquals(Arrays.asList("jkl", "mno"), result.get(1));

        // Test with empty strings and null
        result = examples.splitPositiveAndNegativeInput("", null);
        assertTrue(result.get(0).isEmpty());
        assertTrue(result.get(1).isEmpty());

        // Test with whitespace and empty segments
        result = examples.splitPositiveAndNegativeInput("abc| |def", "jkl||mno");
        assertEquals(Arrays.asList("abc", "def"), result.get(0));
        assertEquals(Arrays.asList("jkl", "mno"), result.get(1));
    }

    @Test
    void testGetExamples() {
        examples.splitPositiveAndNegativeInput("abc|def", "ghi|jkl");

        List<String> positives = examples.getPositiveExamples();
        List<String> negatives = examples.getNegativeExamples();

        assertEquals(Arrays.asList("abc", "def"), positives);
        assertEquals(Arrays.asList("ghi", "jkl"), negatives);
    }

    @Test
    void testFileProcessing(@TempDir Path tempDir) throws IOException {
        // Create a test file
        Path testFile = tempDir.resolve("test_examples.txt");
        Files.writeString(testFile, "abc|def|ghi\n::\njkl|mno");

        // Test file validation
        assertTrue(examples.validateFileContent(testFile.toString()));

        // Test splitting from file
        List<List<String>> result = examples.splitPositiveAndNegativeFile(testFile.toString());
        assertEquals(Arrays.asList("abc", "def", "ghi"), result.get(0));
        assertEquals(Arrays.asList("jkl", "mno"), result.get(1));

        // Test invalid file format
        Path invalidFile = tempDir.resolve("invalid.txt");
        Files.writeString(invalidFile, "abc|def|ghi\njkl|mno");
        assertFalse(examples.validateFileContent(invalidFile.toString()));
    }
}