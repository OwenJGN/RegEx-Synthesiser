package com.owenjg.regexsynthesiser;

import com.owenjg.regexsynthesiser.synthesis.PatternAnalyser;
import com.owenjg.regexsynthesiser.synthesis.RegexSynthesiser;
import com.owenjg.regexsynthesiser.validation.ExampleValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class SynthesisTest {

    private PatternAnalyser patternAnalyser;
    private ExampleValidator validator;

    @BeforeEach
    void setUp() {
        patternAnalyser = new PatternAnalyser();
        validator = new ExampleValidator();
    }

    @Test
    void testPatternAnalyserWithSimpleExamples() {
        List<String> positiveExamples = Arrays.asList("abc", "abd", "abe");
        List<String> negativeExamples = Arrays.asList("acb", "adc", "xyz");

        String regex = patternAnalyser.generalisePattern(positiveExamples, negativeExamples);

        // Verify pattern is correct
        assertTrue(validator.validateExamples(regex, positiveExamples, negativeExamples));
    }

    @Test
    void testPatternAnalyserWithDigitsAndLetters() {
        List<String> positiveExamples = Arrays.asList("a1", "b2", "c3", "d4");
        List<String> negativeExamples = Arrays.asList("1a", "2b", "aa", "11");

        String regex = patternAnalyser.generalisePattern(positiveExamples, negativeExamples);

        // Verify pattern is correct
        assertTrue(validator.validateExamples(regex, positiveExamples, negativeExamples));
    }

    @Test
    void testPatternAnalyserWithEmptyString() {
        List<String> positiveExamples = Arrays.asList("");
        List<String> negativeExamples = Arrays.asList("a", "b", "ab");

        String regex = patternAnalyser.generalisePattern(positiveExamples, negativeExamples);

        // Verify pattern is correct
        assertTrue(validator.validateExamples(regex, positiveExamples, negativeExamples));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testSynthesiserAsync() throws InterruptedException {
        // Create synthesiser without UI components
        RegexSynthesiser synthesiser = new RegexSynthesiser(null);

        List<String> positiveExamples = Arrays.asList("abc", "abd", "abe");
        List<String> negativeExamples = Arrays.asList("acb", "adc", "xyz");

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> resultRegex = new AtomicReference<>();
        final AtomicBoolean gotError = new AtomicBoolean(false);

        // Set up callback
        synthesiser.setProgressCallback(new RegexSynthesiser.ProgressCallback() {
            @Override
            public void onProgress(long elapsedTime, String status) {
                // Nothing to do here
            }

            @Override
            public void onComplete(String generatedRegex) {
                resultRegex.set(generatedRegex);
                latch.countDown();
            }

            @Override
            public void onCancel() {
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                gotError.set(true);
                latch.countDown();
            }
        });

        // Start synthesis in a separate thread
        Thread synthesisThread = new Thread(() -> {
            synthesiser.synthesise(positiveExamples, negativeExamples);
        });
        synthesisThread.start();

        // Wait for completion
        boolean completed = latch.await(5, TimeUnit.SECONDS);

        assertTrue(completed, "Synthesis should complete within timeout");
        assertFalse(gotError.get(), "Synthesis should not produce an error");
        assertNotNull(resultRegex.get(), "Result regex should not be null");

        // Verify that the returned regex contains both pattern types
        String result = resultRegex.get();
        assertTrue(result.contains("Pattern Analyser:") && result.contains("DFA-based:"),
                "Result should contain both regex types");
    }

    @Test
    void testSynthesiserWithEmptyInput() {
        RegexSynthesiser synthesiser = new RegexSynthesiser(null);

        final AtomicReference<String> errorMessage = new AtomicReference<>();

        synthesiser.setProgressCallback(new RegexSynthesiser.ProgressCallback() {
            @Override
            public void onProgress(long elapsedTime, String status) {}

            @Override
            public void onComplete(String generatedRegex) {}

            @Override
            public void onCancel() {}

            @Override
            public void onError(String message) {
                errorMessage.set(message);
            }
        });

        // This should trigger an error because we need at least one positive example
        synthesiser.synthesise(Collections.emptyList(), Collections.emptyList());

        assertNotNull(errorMessage.get(), "Should have received an error message");
        assertTrue(errorMessage.get().contains("positive example"),
                "Error should mention positive examples");
    }

    @Test
    void testPatternAnalyserWithCommonPrefixSuffix() {
        List<String> positiveExamples = Arrays.asList("test123end", "test456end", "test789end");
        List<String> negativeExamples = Collections.emptyList();

        String regex = patternAnalyser.generalisePattern(positiveExamples, negativeExamples);

        // Should identify the common prefix and suffix
        assertTrue(regex.startsWith("test"));
        assertTrue(regex.endsWith("end"));
    }

    @Test
    void testPatternAnalyserWithPrefixSuffix() {
            List<String> positiveExamples = Arrays.asList("prefix123suffix", "prefix456suffix", "prefix789suffix");
        List<String> negativeExamples = Arrays.asList("notprefix123suffix", "prefix123", "123suffix");

        String regex = patternAnalyser.generalisePattern(positiveExamples, negativeExamples);

        // Verify pattern is correct
        assertTrue(validator.validateExamples(regex, positiveExamples, negativeExamples));
        }

}