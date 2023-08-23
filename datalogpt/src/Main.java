package datalogpt.src;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        List<File> test_dirOrJars = Lists.<File>newArrayList(new File("test/Test.jar"));
        FactGenerator.generateFacts(test_dirOrJars);
    }
}