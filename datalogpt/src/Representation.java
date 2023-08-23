package datalogpt.src;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.jacodb.api.*;
import org.jacodb.api.cfg.*;

class Representation {

    private final Map<JcMethod, String> _methodSigRepr = new ConcurrentHashMap<>();
    private final Map<JcField, String> _fieldSigRepr = new ConcurrentHashMap<>();
    // TODO: Handle Traps
    //private final Map<Trap, String> _trapRepr = new ConcurrentHashMap<>();
    private final Map<JcMethod, String> methodNames = new ConcurrentHashMap<>();
    private static final Pattern qPat = Pattern.compile("'");

    public static String classConstant(String className) {
        return "<class " + className + ">";
    }

    public static String methodHandleConstant(String handleName) {
        return "<handle " + handleName + ">";
    }

    public static String nativeReturnVarOfMethod(String m) {
        return m + "/@native-return";
    }

    public static String instructionId(String m, String kind, int index) {
        return m + "/" + kind + "/" + index;
    }

    public static String numberedInstructionId(String pre, String mid, SessionCounter c) {
        return instructionId(pre, mid, c.nextNumber(mid));
    }

    protected static String unsupportedId(String m, String kind, String ins, int index) {
        return m + "/unsupported " + kind + "/" + ins + "/" + index;
    }

    protected static String localId(String m, String l) {
        return m + "/" + l;
    }

    protected static String newLocalIntermediateId(String s, SessionCounter c) {
        return numberedInstructionId(s, "intermediate", c);
    }

    public static String heapAllocId(String m, String s, SessionCounter c) {
        return m + "/new " + s + "/" + c.nextNumber(s);
    }

    public static String handlerMid(String excType) {
        return "catch " + excType;
    }

    protected static String throwLocalId(String name) {
        return "throw " + name;
    }

    public static String fieldId(String declClass, String type, String name) {
        return "<" + declClass + ": " + type + " " + name + ">";
    }

    public static String stripQuotes(CharSequence s) {
        return qPat.matcher(s).replaceAll("");
    }

    static String classConstant(JcClassOrInterface c) {
        return classConstant(c.getName());
    }

    static String classConstant(JcType t) {
        return classConstant(t.toString());
    }

    String signature(JcMethod m) {
        String result = _methodSigRepr.get(m);
        if (result == null) {
            result = stripQuotes(m.getDeclaration().getRelativePath());
            _methodSigRepr.put(m, result);
        }

        return result;
    }

    String signature(JcField f) {
        String result = _fieldSigRepr.get(f);
        if (result == null) {
            result = stripQuotes(f.getDeclaration().getRelativePath());
            _fieldSigRepr.put(f, result);
        }
        return result;
    }

    static String signature(JcMethodRef mRef) {
        return stripQuotes(mRef.toString());
    }

    String simpleName(JcMethod m) {
        String result = methodNames.get(m);
        if (result == null) {
            result = stripQuotes(m.getName());
            methodNames.put(m, result);
        }
        return result;
    }

    public static String unescapeSimpleName(String n) {
        boolean escaped = n.startsWith("'") && n.endsWith("'");
        return escaped ? n.substring(1, n.length()-1) : n;
    }

    // TODO: Find method interface in JCDB
    //static String simpleName(SootMethodInterface m) {
    //    return m.getName();
    //}

    static String simpleName(JcField m) {
        return m.getName();
    }

    static String params(JcMethod m) {
        StringBuilder builder = new StringBuilder("(");
        int count = m.getParameters().size();
        for(int i = 0; i < count; i++) {
            builder.append(m.getParameters().get(i).getType());
            if (i != count - 1)
                builder.append(",");
        }
        builder.append(")");
        return builder.toString();
    }

    String thisVar(String methodId) {
        return methodId + "/@this";
    }

    String nativeReturnVar(String methodId) {
        return nativeReturnVarOfMethod(methodId);
    }

    String param(String methodId, int i) {
        return methodId + "/@parameter" + i;
    }

    String local(String m, JcLocal l) {
        if (l == null)
            return "null";
        else
            return stripQuotes(localId(m, l.getName()));
    }

    String newLocalIntermediate(String m, JcLocal l, SessionCounter counter) {
        return newLocalIntermediateId(local(m, l), counter);
    }

    // TODO: Handle traps
    /*String handler(String methodId, Trap trap, SessionCounter counter) {
        String result = _trapRepr.get(trap);

        if(result == null)
        {
            String name = handlerMid(trap.getException().getName());
            result = numberedInstructionId(methodId, name, counter);
            _trapRepr.put(trap, result);
        }

        return result;
    }*/

    String throwLocal(String methodId, JcLocal l, SessionCounter counter) {
        String name = throwLocalId(l.getName());
        return numberedInstructionId(methodId, name, counter);
    }

    public static String getKind(JcInst inst) {
        if (inst instanceof JcAssignInst) {
            JcAssignInst assignStmt = (JcAssignInst) inst;
            JcExpr rightOp = assignStmt.getRhv();
            JcValue leftOp = assignStmt.getLhv();
            if (rightOp instanceof JcCastExpr)
                return "assign-cast";
            else if (rightOp instanceof JcFieldRef)
                return "read-field-" + ((JcFieldRef) rightOp).getField().getName();
            else if (leftOp instanceof JcFieldRef)
                return "write-field-" + ((JcFieldRef) leftOp).getField().getName();
            else if (rightOp instanceof JcArrayAccess)
                return "read-array-idx";
            else if (leftOp instanceof JcArrayAccess)
                return "write-array-idx";
            else
                return "assign";
        }
        // TODO: Find identityStmt
        /*else if (inst instanceof IdentityStmt)
            return "assign";*/
        else if (inst instanceof JcDeclaration)
            return "definition";
        else if (inst instanceof JcEnterMonitorInst)
            return "enter-monitor";
        else if (inst instanceof JcExitMonitorInst)
            return "exit-monitor";
        else if (inst instanceof JcGotoInst)
            return "goto";
        else if (inst instanceof JcIfInst)
            return "if";
        else if (inst instanceof JcCallInst)
            return "invoke";
        else if (inst instanceof JcReturnInst)
            return "ret";
        // TODO: Find void and return stmt
        /*else if (inst instanceof ReturnVoidStmt)
            return "return-void";
        else if (inst instanceof ReturnStmt)
            return "return";*/
        else if (inst instanceof JcSwitchInst) {
            /*if (inst instanceof TableSwitchStmt)
                return "table-switch";
            else if (inst instanceof LookupSwitchStmt)
                return "lookup-switch";
            else*/
                return "switch";
        } else if (inst instanceof JcThrowInst)
            return "throw";
        return "unknown";
    }

    /*String invoke(JcMethod inMethod, InvokeExpr expr, SessionCounter counter) {
        SootMethodRef exprMethodRef = expr.getMethodRef();
        String name = simpleName(exprMethodRef);
        String midPart;
        if (expr instanceof DynamicInvokeExpr) {
            SootMethodRef bootRef = ((DynamicInvokeExpr)expr).getBootstrapMethodRef();
            String bootName = simpleName(bootRef);
            midPart = JvmDynamicMethodInvocationGenericId(bootName, name);
        } else
            midPart = exprMethodRef.getDeclaringClass() + "." + name;
        return numberedInstructionId(signature(inMethod), midPart, counter);
    }*/

    /*String heapAlloc(String inMethod, Value expr, SessionCounter counter) {
        if(expr instanceof NewExpr || expr instanceof NewArrayExpr)
            return heapAlloc(inMethod, expr.getType(), counter);
        else if(expr instanceof NewMultiArrayExpr)
            return heapAlloc(inMethod, expr.getType(), counter);
            //      return signature(inMethod) + "/" + type + "/" +  session.nextNumber(type);
        else
            throw new RuntimeException("Cannot handle new expression: " + expr);
    }*/

    //String heapMultiArrayAlloc(String inMethod, /* NewMultiArrayExpr expr, */ ArrayType type, SessionCounter counter) {
    //    return heapAlloc(inMethod, type, counter);
    //}

    /*private String heapAlloc(String inMethod, Type type, SessionCounter counter) {
        return heapAllocId(inMethod, type.toString(), counter);
    }*/

    public static String JvmDynamicMethodInvocationGenericId(String bootName, String dynamicName) {
        return "invokedynamic_" + bootName + "::" + dynamicName;
    }

    String invoke(JcMethod inMethod, JcCallExpr expr, SessionCounter counter) {
        JcTypedMethod exprMethodRef = expr.getMethod();
        String name = exprMethodRef.getName();
        String midPart;
        /*if (expr instanceof JcDynamicCallExpr) {
            SootMethodRef bootRef = ((JcDynamicCallExpr)expr).getBootstrapMethodRef();
            String bootName = simpleName(bootRef);
            midPart = JvmDynamicMethodInvocation.genericId(bootName, name);
        } else
            midPart = exprMethodRef.getDeclaringClass() + "." + name;*/
        midPart = exprMethodRef.getMethod().getEnclosingClass() + "." + name;
        return numberedInstructionId(signature(inMethod), midPart, counter);
    }

    String heapAlloc(String inMethod, JcExpr expr, SessionCounter counter) {
        if(expr instanceof JcNewExpr || expr instanceof JcNewArrayExpr)
            return heapAlloc(inMethod, expr.getType(), counter);
        /*else if(expr instanceof NewMultiArrayExpr)
            return heapAlloc(inMethod, expr.getType(), counter);
            //      return signature(inMethod) + "/" + type + "/" +  session.nextNumber(type);*/
        else
            throw new RuntimeException("Cannot handle new expression: " + expr);
    }

    private String heapAlloc(String inMethod, JcType type, SessionCounter counter) {
        return heapAllocId(inMethod, type.toString(), counter);
    }
}
