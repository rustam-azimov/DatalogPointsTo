package test1;

import com.google.common.collect.Lists;
import datalogpt.src.FactGenerator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class Test1 {
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        List<File> test_dirOrJars = Lists.<File>newArrayList(new File("test/test1/resources/Test.jar"));
        FactGenerator.generateFacts(test_dirOrJars);
    }
}