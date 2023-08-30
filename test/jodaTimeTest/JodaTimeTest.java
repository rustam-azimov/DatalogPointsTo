package jodaTimeTest;

import com.google.common.collect.Lists;
import datalogpt.src.FactGenerator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class JodaTimeTest {
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        List<File> dirOrJars = Lists.<File>newArrayList(
                new File("../joda-time/target/classes"),
                new File("../joda-time/target/test-classes"),
                new File("../joda-time/lib/joda-convert-1.9.2.jar"),
                new File("../joda-time/lib/junit-3.8.2.jar"));
        FactGenerator.generateFacts(dirOrJars);
    }
}
