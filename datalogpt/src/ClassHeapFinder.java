package datalogpt.src;

import org.jacodb.api.JcClassOrInterface;
import org.jacodb.api.JcClasspath;
import org.jacodb.api.JcMethod;
import org.jacodb.api.JcType;
import org.jacodb.api.cfg.*;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

class ClassHeapFinder {
    private final Collection<String> recordedTypes = new LinkedList<>();
    private final Collection<String> classHeapTypes = new LinkedList<>();
    private JcClasspath _cp;

    public ClassHeapFinder(JcClasspath cp) {
        _cp = cp;
    }

    /**
     * Returns the heap types that appear in class constants.
     *
     * @return the heap types
     */
    public Collection<String> getUnrecordedTypes(Iterable<JcClassOrInterface> classes) {
        scan(classes);
        Collection<String> ret = ConcurrentHashMap.<String>newKeySet();
        ret.addAll(classHeapTypes);
        ret.removeAll(recordedTypes);
        return ret;
    }

    private void scan(Iterable<JcClassOrInterface> classes) {
        for (JcClassOrInterface c : classes) {
            recordedTypes.add(c.getName());
            for (JcMethod m : c.getDeclaredMethods())
                if (!(/*m.isPhantom() || */m.isAbstract() || m.isNative()))
                    scan(m);
        }
    }

    private void scan(JcMethod m) {
        /*if (!m.hasActiveBody()) {
            m.retrieveActiveBody();
            System.err.println("Preprocessing: found method without active body: " + m.getSignature());
        }*/

        for (JcInst inst : m.getInstList())
            if (inst instanceof JcAssignInst) {
                JcExpr right = ((JcAssignInst)inst).getRhv();
                if (right instanceof JcClassConstant)
                    processClassConstant((JcClassConstant)right);
                else if (right instanceof JcCallExpr)
                    processInvokeExpr((JcCallExpr)right);
            } else if (inst instanceof JcCallExpr)
                processInvokeExpr((JcCallExpr)inst);
            /*else if (u instanceof JcCall)
                processInvokeExpr(((JcCallExpr)inst).getInvokeExpr());*/
    }

    private void processInvokeExpr(JcCallExpr invoke) {
        for (JcValue arg : invoke.getArgs())
            if (arg instanceof JcClassConstant)
                processClassConstant((JcClassConstant)arg);
    }

    private void processClassConstant(JcClassConstant constant) {
        String s = TypeUtils.replaceSlashesWithDots(constant.toString());
        char first = s.charAt(0);
        if (TypeUtils.isLowLevelType(first, s)) {
            // array type
            JcType t = raiseTypeWithJc(s, _cp);
            String actualType = t.toString();
            if (actualType.endsWith("[]")) {
                String elemType = actualType.substring(0, actualType.length() - 2);
                if (!TypeUtils.isPrimitiveType(elemType))
                    classHeapTypes.add(elemType);
            } else
                classHeapTypes.add(actualType);
        } else if (first != '(')   // Ignore method type constants
            classHeapTypes.add(s);
    }

    /**
     * Use JCDB machinery to translate a low-level type id to a Type.
     *
     * @param s  the low-level JVM id of the type
     * @return   a JCDB Type
     */
    public static JcType raiseTypeWithJc(String s, JcClasspath cp) {
        return cp.findTypeOrNull(s);
    }
}

