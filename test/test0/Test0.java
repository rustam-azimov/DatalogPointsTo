package test0;

import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import datalogpt.src.FactGenerator;


public class Test0 {
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        List<File> test_dirOrJars = Lists.<File>newArrayList(new File("test/test0/resources/Test.jar"));
        FactGenerator.generateFacts(test_dirOrJars);
    }
}