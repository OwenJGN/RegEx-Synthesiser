package com.owenjg.regexsynthesiser;

import com.owenjg.regexsynthesiser.synthesis.Examples;
import com.owenjg.regexsynthesiser.synthesis.RegexSynthesiser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Testing {
    public static void main(String[] args) {
        String[] filePaths = {
                "D:/owenj/Documents/simple.txt",
                "D:/owenj/Documents/email.txt",
                "D:/owenj/Documents/simple2.txt",
                "D:/owenj/Documents/simple3.txt",
                "D:/owenj/Documents/simple4.txt"
        };
        ArrayList<String> regexs = new ArrayList<>();
        RegexSynthesiser synthesiser = new RegexSynthesiser(null);
        Examples examples = new Examples();

        for (String filePath : filePaths) {
            System.out.println("Processing file: " + filePath);

            try {
                List<List<String>> exampleLists = examples.splitPositiveAndNegativeFile(filePath);
                List<String> positiveExamples = exampleLists.get(0);
                List<String> negativeExamples = exampleLists.get(1);

                synthesiser.setProgressCallback(new RegexSynthesiser.ProgressCallback() {
                    @Override
                    public void onProgress(long elapsedTime, String status) {
                        System.out.println("Progress: " + status);
                    }

                    @Override
                    public void onComplete(String generatedRegex) {

                        regexs.add("Positives:" + positiveExamples +"\nNegatives:" + negativeExamples +"\nCurrent File: " +filePath + "\nRegex: " + generatedRegex);
                        System.out.println("---");
                    }

                    @Override
                    public void onCancel() {
                        System.out.println("Generation canceled.");
                    }

                    @Override
                    public void onError(String errorMessage) {
                        System.out.println("Error: " + errorMessage);
                    }
                });

                synthesiser.synthesise(positiveExamples, negativeExamples);
            } catch (IOException e) {
                System.out.println("Error processing file: " + filePath);
                e.printStackTrace();
            }

            for (String s:
                 regexs) {
                System.out.println(s);
                System.out.println();
            }
        }
    }
}