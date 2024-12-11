package com.owenjg.regexsynthesiser.synthesis;

import java.io.File;
import java.util.ArrayList;
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
        List<String> pos = new ArrayList<>();
        List<String> neg = new ArrayList<>();

        pos.add("test");
        neg.add("tedst");

        List<List<String>> list = new ArrayList<>();
        list.add(pos);
        list.add(neg);

        return list;
    }

    public List<List<String>> splitPositiveAndNegativeFile(String filePath){
        return null;
    }

}
