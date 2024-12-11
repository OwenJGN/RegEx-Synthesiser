package com.owenjg.regexsynthesiser.synthesis;

import java.io.File;
import java.util.List;

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
        return null;
    }

    public List<List<String>> splitPositiveAndNegativeFile(File file){
        return null;
    }

}
