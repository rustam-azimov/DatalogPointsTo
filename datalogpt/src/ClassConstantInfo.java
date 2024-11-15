package datalogpt.src;

import org.jacodb.api.JcClasspath;
import org.jacodb.api.JcType;
import org.jacodb.api.cfg.JcClassConstant;

/**
 * Auxiliary class to help when method type constants appear as class heaps.
 */
class ClassConstantInfo {
    final String heap;
    boolean isMethodType = false;
    // null for method types
    String actualType = null;

    public ClassConstantInfo(JcClassConstant constant, JcClasspath cp) {
        String s = TypeUtils.replaceSlashesWithDots(constant.toString());
        char first = s.charAt(0);

        /* There is some weirdness in class constants: normal Java class
           types seem to have been translated to a syntax with the initial
           L, but arrays are still represented as [, for example [C for
           char[] */
        if (TypeUtils.isLowLevelType(first, s)) {
            // array type
            JcType t = ClassHeapFinder.raiseTypeWithJc(s, cp);
            this.actualType = t.toString();
            this.heap = Representation.classConstant(t);
        } else if (first == '(') {
            // method type constant (viewed by Soot as a class constant)
            this.heap = s;
            this.isMethodType = true;
        } else {
            //            SootClass c = soot.Scene.v().getSootClass(s);
            //            if (c == null) {
            //                throw new RuntimeException("Unexpected class constant: " + constant);
            //            }
            //
            //            heap =  _rep.classConstant(c);
            //            actualType = c.getName();
            ////              if (!actualType.equals(s))
            ////                  System.out.println("hallelujah!\n\n\n\n");
            // The code above should be functionally equivalent with the simple code below,
            // but the above causes a concurrent modification exception due to a Soot
            // bug that adds a phantom class to the Scene's hierarchy, although
            // (based on their own comments) it shouldn't.
            this.heap = classConstant(s);
            this.actualType = s;
        }
    }

    public static String classConstant(String className) {
        return "<class " + className + ">";
    }
}

