package com.owenjg.regexsynthesiser.validation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Examples {

    List<String> positiveExamples;
    List<String> negativeExamples;

    public List<String> getPositiveExamples(){
        return positiveExamples;
    }

    public List<String> getNegativeExamples(){
        return negativeExamples;
    }

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

    public List<List<String>> splitPositiveAndNegativeFile(String filePath) throws IOException {
        String content = Files.readString(Path.of(filePath));

        String[] splitExamples = content.split("::");
        List<String> pos = splitExamples(splitExamples[0]);
        List<String> neg = splitExamples(splitExamples[1]);

        List<List<String>> list = new ArrayList<>();
        list.add(pos);
        list.add(neg);

        this.positiveExamples = pos;
        this.negativeExamples = neg;
        return list;
    }

    private List<String> splitExamples(String examples) {
        return Optional.ofNullable(examples)
                .map(ex -> Arrays.stream(ex.split("\\|"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

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
