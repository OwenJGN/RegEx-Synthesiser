package com.owenjg.regexsynthesiser.synthesis;

import java.util.List;

public class RegexSynthesiser {
    private volatile boolean cancelRequested = false;
    private ProgressCallback progressCallback;

    // Interface for progress callback
    public interface ProgressCallback {
        void onProgress(long elapsedTime, String status);
        void onComplete(String generatedRegex);
        void onCancel();
    }

    public void synthesise(List<String> positiveExamples, List<String> negativeExamples) {
        // Reset cancellation flag
        cancelRequested = false;

        // Start time tracking
        long startTime = System.currentTimeMillis();

        try {
            // Your actual regex generation logic would replace this loop
            for (int i = 0; i < 10; i++) {
                // Check for cancellation
                if (cancelRequested) {
                    if (progressCallback != null) {
                        progressCallback.onCancel();
                    }
                    break;

                }

                // Simulate regex generation work
                Thread.sleep(500);  // Simulated delay

                // Calculate elapsed time
                long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;

                // Update progress if callback is set
                if (progressCallback != null) {
                    progressCallback.onProgress(elapsedTime, "Generating... Step " + (i + 1));
                }
            }

            // If not cancelled, complete the process
            if (!cancelRequested) {
                String generatedRegex = "GENERATED_REGEX_PLACEHOLDER";

                if (progressCallback != null) {
                    progressCallback.onComplete(generatedRegex);
                }
            }
        } catch (InterruptedException e) {
            // Handle interruption
            if (progressCallback != null) {
                progressCallback.onCancel();
            }
        }
    }

    // Method to set progress callback
    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }

    // Method to cancel the generation
    public void cancelGeneration() {
        cancelRequested = true;
    }
}