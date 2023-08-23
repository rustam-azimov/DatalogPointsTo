package datalogpt.src;

import org.jacodb.api.*;
import org.jacodb.api.ext.HierarchyExtension;
import org.jacodb.api.ext.JcClasses;
import org.jacodb.impl.JacoDB;
import org.jacodb.impl.JcSettings;
import org.jacodb.impl.features.JcHierarchies;
import org.jacodb.impl.features.Usages;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public class FactGenerator {
    public static void generateFacts(List<File> dirOrJars) throws ExecutionException, InterruptedException, IOException {
        System.out.println("Creating database");
        try (JcDatabase instance = JacoDB.async(new JcSettings().installFeatures(Usages.INSTANCE)).get()) {
            try (JcClasspath classpath = instance.asyncClasspath(dirOrJars).get()) {
                for(int i = 0; i < 1; ++i) {

                    instance.asyncAwaitBackgroundJobs().get();
                    HierarchyExtension hierarchyExt = JcHierarchies.asyncHierarchy(classpath).get();
                    FactGeneratorTask generator = new FactGeneratorTask(classpath);

                    //JcApplicationGraphImpl applicationGraph =
                    //        new JcApplicationGraphImpl(classpath, JcUsages.asyncUsages(classpath).get());

                    Stream<RegisteredLocation> targetClasses = classpath.getRegisteredLocations().stream()
                            .filter(it -> !it.isRuntime());

                    Iterable<RegisteredLocation> iterable = targetClasses::iterator;
                    long start = System.currentTimeMillis();
                    for (RegisteredLocation location : iterable) {
                        List<ClassSource> locationClasses = classpath.getDb().getPersistence()
                                .findClassSources(location);
                        locationClasses.parallelStream().forEach(classSource -> {
                            JcClassOrInterface jcClass = classpath.toJcClass(classSource);
                            // TODO: How we should get entry methods or classes?
                            JcMethod entryMethod = JcClasses.findMethodOrNull(jcClass,
                                    "main", "([Ljava/lang/String;)V");
                            if (entryMethod != null) {
                                generator.addMainClass(jcClass);
                            }
                            generator.process(jcClass);
                        });
                    }
                    generator.flush();
                    long finish = System.currentTimeMillis();
                    System.out.println("Facts generated in " + (finish - start) + " ms");
                }
            }
        }
    }
};
