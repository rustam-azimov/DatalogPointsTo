package julietTest;

import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import datalogpt.src.FactGenerator;


public class JulietTest {
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        List<File> juliet_dirOrJars = Lists.<File>newArrayList(
                new File("../juliet-test-suite/target/classes"),
                new File("../juliet-test-suite/lib/commons-codec-1.5.jar"),
                new File("../juliet-test-suite/lib/commons-lang-2.5.jar"),
                new File("../juliet-test-suite/lib/javamail-1.4.4.jar"),
                new File("../juliet-test-suite/lib/servlet-api.jar"),
                new File("../../.m2/repository/javax/activation/javax.activation-api/1.2.0/javax.activation-api-1.2.0.jar")
        );
        FactGenerator.generateFacts(juliet_dirOrJars);
    }
}
