package datalogpt.src;

import org.jacodb.api.*;
import org.jacodb.api.cfg.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static datalogpt.src.PredicateFile.*;

public class FactWriter {
    private FactDatabase _db;

    final Representation _rep;

    private static final String L_OP = "1";
    private static final String R_OP = "2";
    private final boolean _extractMoreStrings = false;
    private final boolean _writeArtifactsMap = false;
    private final boolean _regMethods = false;
    private final Map<String, JcType> _varTypeMap = new ConcurrentHashMap<>();

    private final Map<JcInst, Collection<InstrInfo>> expandedPhiNodes = new ConcurrentHashMap<>();
    private final Set<String> methodStrings;

    protected FactWriter() {
        try {
            this._db = new FactDatabase("out/facts");
        } catch (java.io.IOException e){
            System.out.println(e.getMessage());
        }
        this._rep = new Representation();
        this.methodStrings = this._regMethods ? ConcurrentHashMap.newKeySet() : null;
    }

    public Set<String> getMethodStrings() {
        return this.methodStrings;
    }

    public static String str(int i) {
        return String.valueOf(i);
    }

    public static String encodeStringConstant(String constant) {
        String raw;
        if(constant.trim().equals(constant) && constant.length() > 0)
            raw = constant;
        else
            raw = "<<\"" + constant + "\">>";
        return raw;
    }

    protected String writeStringConstant(String constant) {
        String raw = encodeStringConstant(constant);
        String result;
        if (raw.length() <= 256) {
            result = raw;
        } else {
            result = "<<HASH:" + raw.hashCode() + ">>";
        }

        this._db.add(PredicateFile.STRING_RAW, result, new String[]{raw});
        this._db.add(PredicateFile.STRING_CONST, result, new String[0]);
        return result;
    }

    protected String hashMethodNameIfLong(String methodRaw) {
        return methodRaw.length() <= 1024 ? methodRaw : "<<METHOD HASH:" + methodRaw.hashCode() + ">>";
    }

    private void writeProperty(String path, String key, String value) {
        String pathId = this.writeStringConstant(path);
        String keyId = this.writeStringConstant(key);
        String valueId = this.writeStringConstant(value);
        this._db.add(PredicateFile.PROPERTIES, pathId, new String[]{keyId, valueId});
    }

    void writeMethodHandleConstant(String heap, String method, String retType, String paramTypes, int arity) {
        this._db.add(PredicateFile.METHOD_HANDLE_CONSTANT, heap, new String[]{method, retType, paramTypes, str(arity)});
    }

    void writeFormalParam(String methodId, String var, String type, int i) {
        this._db.add(PredicateFile.FORMAL_PARAM, str(i), new String[]{methodId, var});
        this.writeLocal(var, type, methodId);
    }

    void writeFormalParam(String methodId, TypeName type, int i, JcClasspath cp) {
        String var = _rep.param(methodId, i);
        JcType jcType = getTypeByName(type, cp);
        writeFormalParam(methodId, var, writeType(jcType), i);
    }

    void writeThisVar(String methodId, String thisVar, String type) {
        this._db.add(PredicateFile.THIS_VAR, methodId, new String[]{thisVar});
        this.writeLocal(thisVar, type, methodId);
    }

    void writeThisVar(String methodId, JcClassOrInterface declaringClass) {
        String thisVar = _rep.thisVar(methodId);
        String type = writeType(declaringClass);
        writeThisVar(methodId, thisVar, type);
    }

    public void writeApplicationClass(JcClassOrInterface application) {
        _db.add(APP_CLASS, writeType(application));
    }

    public void flush() {
        try {
            _db.flush();
        } catch (IOException e) {
            System.out.println("Error while flush facts database.");
        }
    }

    public void writeMainClass(JcClassOrInterface clazz) {
        this._db.add(MAIN_CLASS, clazz.getName());
    }

    public void writeNumConstantRaw(String val, String valType) {
        this._db.add(PredicateFile.NUM_CONSTANT_RAW, val, new String[]{valType});
    }

    /*public void writePreliminaryFacts(BasicJavaSupport java, boolean debug) {
        PropertyProvider propertyProvider = java.getPropertyProvider();
        Iterator var4 = propertyProvider.getProperties().entrySet().iterator();

        while(var4.hasNext()) {
            Map.Entry<String, Properties> entry = (Map.Entry)var4.next();
            String path = (String)entry.getKey();
            Properties properties = (Properties)entry.getValue();
            Iterator var8 = properties.stringPropertyNames().iterator();

            while(var8.hasNext()) {
                String propertyName = (String)var8.next();
                String propertyValue = properties.getProperty(propertyName);
                this.writeProperty(path, propertyName, propertyValue);
            }
        }

        this.generateFactsForXML(this._db, java.xmlRoots, debug);
    }*/

    /*public void writeLastFacts(BasicJavaSupport java) {
        ArtifactScanner artScanner = java.getArtifactScanner();
        Map<String, Set<ArtifactEntry>> artifactToClassMap = artScanner.getArtifactToClassMap();
        Set<GenericFieldInfo> genericFields = artScanner.getGenericFields();
        if (this._writeArtifactsMap) {
            System.out.println("Generated artifact-to-class map for " + artifactToClassMap.size() + " artifacts.");
            Iterator var5 = artifactToClassMap.keySet().iterator();

            while(var5.hasNext()) {
                String artifact = (String)var5.next();
                Iterator var7 = ((Set)artifactToClassMap.get(artifact)).iterator();

                while(var7.hasNext()) {
                    ArtifactEntry ae = (ArtifactEntry)var7.next();
                    this.writeClassArtifact(artifact, ae.className, ae.subArtifact, ae.size);
                }
            }
        }

        this.writeGenericFields(genericFields);
        artifactToClassMap.clear();
    }*/

    void writeMethodDeclaresException(String methodId, String exceptionType) {
        this._db.add(PredicateFile.METHOD_DECL_EXCEPTION, exceptionType, new String[]{methodId});
    }

    /*protected void writeGenericFields(Iterable<GenericFieldInfo> genericFields) {
        GenericFieldInfo fi;
        for(Iterator var2 = genericFields.iterator(); var2.hasNext(); this._db.add(PredicateFile.GENERIC_FIELD, "<" + fi.definingClass + ": " + fi.type + " " + fi.name + ">", new String[]{fi.definingClass, fi.name, fi.type})) {
            fi = (GenericFieldInfo)var2.next();
            if (!fi.type.contains("extends") && !fi.type.contains("super")) {
                try {
                    GenericTypeLexer lexer = new GenericTypeLexer(CharStreams.fromFileName(fi.type));
                    GenericTypeParser parser = new GenericTypeParser(new CommonTokenStream(lexer));
                    ParseTree parseTree = parser.type();
                    PrintVisitor printVisitor = new PrintVisitor(this._db);
                    printVisitor.visit(parseTree);
                } catch (IOException var8) {
                    var8.printStackTrace();
                }
            }
        }

    }*/

    protected void writeLocal(String local, String type, String method) {
        this._db.add(PredicateFile.VAR_TYPE, local, new String[]{type});
        this.writeVarDeclaringMethod(local, method);
    }

    protected void writeVarDeclaringMethod(String local, String method) {
        this._db.add(PredicateFile.VAR_DECLARING_METHOD, local, new String[]{method});
    }

    protected void writeArrayTypes(String arrayType, String componentType) {
        this._db.add(PredicateFile.ARRAY_TYPE, arrayType, new String[0]);
        this._db.add(PredicateFile.COMPONENT_TYPE, arrayType, new String[]{componentType});
    }

    protected void writeOperatorAt(String insn, String op) {
        this._db.add(PredicateFile.OPERATOR_AT, insn, new String[]{op});
    }

    protected void writeIf(String insn, int index, int indexTo, String methodId) {
        this._db.add(PredicateFile.IF, insn, new String[]{str(index), str(indexTo), methodId});
    }

    protected void writeIfConstant(String insn, String branch, String cons) {
        this._db.add(PredicateFile.IF_CONSTANT, insn, new String[]{branch, cons});
    }

    protected void writeIfVar(String insn, String branch, String local) {
        this._db.add(PredicateFile.IF_VAR, insn, new String[]{branch, local});
    }

    protected void writeDummyIfVar(String insn, String local) {
        this._db.add(PredicateFile.DUMMY_IF_VAR, insn, new String[]{local});
    }

    protected void writeAssignBinop(String insn, int index, String local, String methodId) {
        this._db.add(PredicateFile.ASSIGN_BINOP, insn, new String[]{str(index), local, methodId});
    }

    protected void writeAssignOperFrom(String insn, String branch, String local) {
        this._db.add(PredicateFile.ASSIGN_OPER_FROM, insn, new String[]{branch, local});
    }

    protected void writeAssignOperFromConstant(String insn, String branch, String value) {
        this._db.add(PredicateFile.ASSIGN_OPER_FROM_CONSTANT, insn, new String[]{branch, value});
    }

    protected void writeInvokedynamic(String insn, int index, String bootSig, String dynName, String dynRetType, int dynArity, String dynParamTypes, int tag, String methodId) {
        this._db.add(PredicateFile.DYNAMIC_METHOD_INV, insn, new String[]{str(index), bootSig, dynName, dynRetType, str(dynArity), dynParamTypes, str(tag), methodId});
        this.writeStringConstant(dynName);
        this.writeStringConstant(dynRetType + dynParamTypes);
    }

    protected void writeInvokedynamicParameterType(String insn, int paramIndex, String type) {
        this._db.add(PredicateFile.DYNAMIC_METHOD_INV_PARAM_TYPE, insn, new String[]{str(paramIndex), type});
    }

    protected void writeAssignLocal(String insn, int index, String from, String to, String methodId) {
        this._db.add(PredicateFile.ASSIGN_LOCAL, insn, new String[]{str(index), from, to, methodId});
    }

    void writeAssignLocal(InstrInfo ii, JcLocal to, JcLocal from) {
        String methodId = ii.methodId;
        writeAssignLocal(ii.insn, ii.index, _rep.local(methodId, from), _rep.local(ii.methodId, to), methodId);
    }

    void writeAssignLocal(InstrInfo ii, JcLocal to, JcArgument ref) {
        String methodId = ii.methodId;
        writeAssignLocal(ii.insn, ii.index, _rep.param(methodId, ref.getIndex()), _rep.local(methodId, to), methodId);
    }

    void writeLoadArrayIndex(JcInst stmt, InstrInfo ii, JcLocal base, JcLocal to, JcValue arrIndex) {
        writeLoadOrStoreArrayIndex(stmt, ii, base, to, arrIndex, LOAD_ARRAY_INDEX);
    }

    void writeAssignInvoke(JcMethod inMethod, JcInst stmt, JcCallExpr callExpr, InstrInfo ii, JcLocal to, SessionCounter session, JcClasspath cp) {
        String invokeInstructionId = writeInvoke(inMethod, stmt, callExpr, ii, session, cp);
        _db.add(ASSIGN_RETURN_VALUE, invokeInstructionId, _rep.local(ii.methodId, to));
    }

    String writeInvoke(JcMethod inMethod, JcInst stmt, JcCallExpr callExpr, InstrInfo ii, SessionCounter session, JcClasspath cp) {
        if (callExpr != null) {
            String insn = _rep.invoke(inMethod, callExpr, session);
            return writeInvokeHelper(insn, stmt, ii, callExpr, session, cp);
        } else if (stmt instanceof JcCallInst) {
            String insn = _rep.invoke(inMethod, ((JcCallInst) stmt).getCallExpr(), session);
            return writeInvokeHelper(insn, stmt, ii, ((JcCallInst) stmt).getCallExpr(), session, cp);
        }
        return "unknown_call";
    }

    private String writeInvokeHelper(String insn, JcInst stmt, InstrInfo ii,
                                     JcCallExpr expr, SessionCounter session, JcClasspath cp) {
        writeActualParams(stmt, ii, expr, insn, session, cp);

        JcMethod exprMethodRef = expr.getMethod().getMethod();
        String simpleName = exprMethodRef.getName();
        String declClass = exprMethodRef.getEnclosingClass().getName();

        _db.add(METHOD_INV_LINE, insn, str(stmt.getLineNumber()));

        String methodId = ii.methodId;
        if (expr instanceof JcDynamicCallExpr)
            System.out.println("Cannot handle JcDynamicCallExpr: " + expr);
            //writeDynamicInvoke((DynamicInvokeExpr) expr, ii.index, insn, methodId);
        else {
            String methodSig = invokeMethodSig(insn, declClass, simpleName, exprMethodRef, expr);
            if (expr instanceof JcStaticCallExpr)
                _db.add(STATIC_METHOD_INV, insn, str(ii.index), methodSig, methodId);
            else if (expr instanceof JcVirtualCallExpr/* || expr instanceof InterfaceInvokeExpr*/) {
                _db.add(VIRTUAL_METHOD_INV, insn, str(ii.index), methodSig, _rep.local(methodId, (JcLocal) ((JcInstanceCallExpr) expr).getInstance()), methodId);
            }
            else if (expr instanceof JcSpecialCallExpr)
                _db.add(SPECIAL_METHOD_INV, insn, str(ii.index), methodSig, _rep.local(methodId, (JcLocal) ((JcInstanceCallExpr) expr).getInstance()), methodId);
            else
                System.out.println("Cannot handle invoke expr: " + expr);
        }

        return insn;
    }

    private void writeActualParams(JcInst stmt, InstrInfo ii, JcCallExpr expr,
                                   String invokeExprRepr, SessionCounter session, JcClasspath cp) {
        String methodId = ii.methodId;
        boolean isInvokedynamic = (expr instanceof JcDynamicCallExpr);
        int count = expr.getArgs().size();
        for (int i = 0; i < count; i++) {
            JcValue arg = expr.getArgs().get(i);
            JcValue v = writeActualParam(stmt, ii, expr, session, arg, i, cp);
            if (v instanceof JcLocal)
                writeActualParam(i, invokeExprRepr, _rep.local(methodId, (JcLocal) v));
            else
                System.out.println("Actual parameter is not a local: " + v + " " + v.getClass());
        }
        /*if (isInvokedynamic) {
            DynamicInvokeExpr di = (DynamicInvokeExpr)expr;
            for (int j = 0; j < di.getBootstrapArgCount(); j++) {
                Value v = di.getBootstrapArg(j);
                if (v instanceof Constant) {
                    Value vConst = writeActualParam(stmt, ii, expr, session, v, j);
                    if (vConst instanceof Local) {
                        Local l = (Local) vConst;
                        _db.add(BOOTSTRAP_PARAMETER, str(j), invokeExprRepr, _rep.local(methodId, l));
                    } else
                        System.out.println("Unknown actual parameter: " + v + " of type " + v.getClass().getName());
                } else
                    System.out.println("Found non-constant argument to bootstrap method: " + di);
            }
        }*/
    }

    private JcValue writeActualParam(JcInst stmt, InstrInfo ii, JcCallExpr expr, SessionCounter session, JcValue v, int idx, JcClasspath cp) {
        if (v instanceof JcStringConstant)
            return writeStringConstantExpression(stmt, ii.methodId, (JcStringConstant) v, session, cp);
        /*else if (v instanceof JcClassConstant)
            return writeClassConstantExpression(ii.methodId, (JcClassConstant) v, session);
         */
        else if (v instanceof JcNumericConstant)
            return writeNumConstantExpression(ii.methodId, v, null, session);
        /*else if (v instanceof MethodHandle)
            return writeMethodHandleConstantExpression(ii.methodId, (MethodHandle) v, session);*/
        else if (v instanceof JcNullConstant) {
            // Giving the type of the formal argument to be used in the creation of
            // temporary var for the actual argument (whose value is null).
            JcType argType = expr.getMethod().getParameters().get(idx).getType();
            return writeNullExpression(ii.methodId, argType, session);
        } /*else if (v instanceof JcConstant) {
            DoopAddons.MethodType mt = DoopAddons.methodType(v);
            if (mt != null)
                return writeMethodTypeConstantExpression(ii, mt, session);
            else
                System.out.println("Value has unknown constant type: " + v);
        } else if (!(v instanceof JimpleLocal))
            System.err.println("WARNING: value has unknown non-constant type: " + v.getClass().getName());*/
        return v;
    }

    static class FreshAssignLocal {
        final JcLocal local;
        final InstrInfo ii;

        FreshAssignLocal(JcLocal local, InstrInfo ii) {
            this.local = local;
            this.ii = ii;
        }
    }

    JcLocal writeStringConstantExpression(JcInst stmt, String methodId, JcStringConstant constant, SessionCounter session, JcClasspath cp) {
        JcType stringType = cp.findTypeOrNull("java.lang.String");
        // introduce a new temporary variable
        FreshAssignLocal fal = newAssignForFreshLocal(methodId, "$stringconstant", stringType, session);
        JcLocal l = fal.local;
        writeAssignStringConstant(stmt, fal.ii, l, constant);
        return l;
    }

    void writeAssignStringConstant(JcInst stmt, InstrInfo ii, JcLocal l, JcStringConstant s) {
        String constant = s.toString();
        String content = constant.substring(1, constant.length() - 1);
        String heapId = writeStringConstant(content);
        String methodId = ii.methodId;
        _db.add(ASSIGN_HEAP_ALLOC, ii.insn, str(ii.index), heapId, _rep.local(methodId, l), methodId, ""+ stmt.getLineNumber());
    }

    private FreshAssignLocal newAssignForFreshLocal(String inMethod, String basename, JcType type, SessionCounter session) {
        return newInstructionWithFreshLocal(inMethod, "fresh-null-assign", basename, type, session);
    }

    private FreshAssignLocal newInstructionWithFreshLocal(String inMethod, String kind, String basename, JcType type, SessionCounter session) {
        return new FreshAssignLocal(freshLocal(inMethod, basename, type, session),
                new InstrInfo(inMethod, kind, session));
    }

    private JcLocal freshLocal(String inMethod, String basename, JcType type, SessionCounter session) {
        String varname = basename + session.nextNumber(basename);
        JcLocal l = new JcLocal() {
            @NotNull
            @Override
            public String getName() {
                return varname;
            }

            @NotNull
            @Override
            public JcType getType() {
                return type;
            }

            @Override
            public <T> T accept(@NotNull JcExprVisitor<T> jcExprVisitor) {
                return null;
            }
        };
        writeLocal(inMethod, l);
        return l;
    }

    void writeLocal(String methodId, JcLocal l) {
        String local = _rep.local(methodId, l);
        JcType type;

        if (_varTypeMap.containsKey(local))
            type = _varTypeMap.get(local);
        else {
            type = l.getType();
            _varTypeMap.put(local, type);
        }

        writeLocal(local, writeType(type), methodId);
        _db.add(VAR_SIMPLENAME, local, l.getName());
    }

    protected void writeActualParam(int index, String invo, String var) {
        this._db.add(PredicateFile.ACTUAL_PARAMETER, str(index), new String[]{invo, var});
    }

    protected void writeMethodTypeConstant(String retType, String[] paramTypes, String params) {
        if (params == null) {
            params = this.concatenate(paramTypes);
        }

        String mt = "(" + params + ")" + retType;
        int arity = paramTypes.length;

        for(int idx = 0; idx < arity; ++idx) {
            this._db.add(PredicateFile.METHOD_TYPE_CONSTANT_PARAM, mt, new String[]{str(idx), paramTypes[idx]});
        }

        this._db.add(PredicateFile.METHOD_TYPE_CONSTANT, mt, new String[]{str(arity), retType, params});
    }

    protected String concatenate(String[] elems) {
        int num = elems.length;
        if (num == 0) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder(elems[0]);

            for(int idx = 1; idx < num; ++idx) {
                sb.append(',');
                sb.append(elems[idx]);
            }

            return sb.toString();
        }
    }

    protected void writeMethodTypeConstant(String mt) {
        int rParen = mt.indexOf(")");
        if (mt.startsWith("(") && rParen != -1) {
            String retType = mt.substring(rParen + 1);
            String params = mt.substring(1, rParen);
            String[] paramTypes = params.split(",");
            this.writeMethodTypeConstant(retType, paramTypes, params);
        } else {
            System.err.println("WARNING: cannot process method type " + mt);
        }

    }

    protected void writeMethodAnnotation(String method, String annotationType) {
        this._db.add(PredicateFile.METHOD_ANNOTATION, method, new String[]{annotationType});
    }

    protected void writeExceptionHandler(String insn, String method, int index, String type, int begin, int end) {
        this._db.add(PredicateFile.EXCEPTION_HANDLER, insn, new String[]{method, str(index), type, str(begin), str(end)});
    }

    protected void writeExceptionHandlerFormal(String insn, String var) {
        this._db.add(PredicateFile.EXCEPTION_HANDLER_FORMAL_PARAM, insn, new String[]{var});
    }

    protected void writeExceptionHandlerPrevious(String currInsn, String prevInsn) {
        this._db.add(PredicateFile.EXCEPT_HANDLER_PREV, currInsn, new String[]{prevInsn});
    }

    public static boolean polymorphicHandling(String declClass, String simpleName) {
        return declClass.equals("java.lang.invoke.MethodHandle") && (simpleName.equals("invoke") || simpleName.equals("invokeExact") || simpleName.equals("invokeBasic"));
    }

    public void writeNativeMethodId(String methodId, String type, String name) {
        String jniMethodId = "Java_" + type.replaceAll("\\.", "_") + "_" + name;
        this._db.add(PredicateFile.NATIVE_METHOD_ID, methodId, new String[]{jniMethodId});
    }

    JcType getTypeByName(TypeName typeName, JcClasspath cp) {
        return cp.findTypeOrNull(typeName.getTypeName());
    }

    void writeNativeReturnVar(String methodId, TypeName returnType, JcClasspath cp) {
        JcType jcType = getTypeByName(returnType, cp);
        /*if (!(jcType instanceof Type.JCVoidType)) {*/
            String var = _rep.nativeReturnVar(methodId);
            _db.add(NATIVE_RETURN_VAR, var, methodId);
            writeLocal(var, writeType(jcType), methodId);
        //}
    }

    private String writeType(JcType t) {
        String result = t.getTypeName();

        if (t instanceof JcArrayType) {
            JcType componentType = ((JcArrayType) t).getElementType();
            writeArrayTypes(result, writeType(componentType));
        }
        else if (t instanceof JcPrimitiveType || t instanceof JcRefType /*|| t instanceof VoidType*/) {
            // taken care of by the standard facts
        }
        else
            System.out.println("Don't know what to do with type " + t);

        return result;
    }

    protected void writeMethod(String methodId, String simpleName, String paramsSig, String declType, String retType, String jvmSig, String arity) {
        this._db.add(PredicateFile.METHOD, methodId, new String[]{simpleName, paramsSig, declType, retType, jvmSig, arity});
        if (this._regMethods) {
            this.methodStrings.add(simpleName);
            this.methodStrings.add(jvmSig);
        }

    }

    String writeMethod(JcMethod m, JcClasspath cp) {
        String methodRaw = _rep.signature(m);
        String methodId = methodSig(m, methodRaw);
        String arity = Integer.toString(m.getParameters().size());
        JcType returnType = cp.findTypeOrNull(m.getReturnType().getTypeName());

        _db.add(STRING_RAW, methodId, methodRaw);
        if (returnType == null) {
            writeMethod(methodId, _rep.simpleName(m), Representation.params(m), writeType(m.getEnclosingClass()), m.getReturnType().getTypeName(), m.getDescription(), arity);
            System.out.println("Warning: Null returnType is encountred");
        } else {
            // TODO: Check method description
            writeMethod(methodId, _rep.simpleName(m), Representation.params(m), writeType(m.getEnclosingClass()), writeType(returnType), m.getDescription(), arity);
        }

        // TODO: Handle method annotations
        for (JcAnnotation jcAnnotation : m.getAnnotations().stream().filter(it -> it.getVisible()).collect(Collectors.toList())) {
            writeMethodAnnotation(methodId, getAnnotationType(jcAnnotation));
            //writeAnnotationElements("method", methodId, null, jcAnnotation.getElems());
        }

        // TODO: Handle method's parameters annotations
        for (int i = 0; i < m.getParameters().size(); i++) {
            for (JcAnnotation jcAnnotation : m.getParameters().get(i).getAnnotations().stream().filter(it -> it.getVisible()).collect(Collectors.toList())) {
                String paramIdx = str(i);
                _db.add(PARAM_ANNOTATION, methodId, paramIdx, getAnnotationType(jcAnnotation));
                //String paramId = methodId + "::parameter#" + paramIdx;
                //writeAnnotationElements("param", paramId, null, jcAnnotation.getElems());
            }
        }
        return methodId;
    }

    public String methodSig(JcMethod m, String methodRaw) {
        if (methodRaw == null)
            methodRaw = _rep.signature(m);
        return methodRaw; // hashMethodNameIfLong(methodRaw);
    }

    void writeClassOrInterfaceType(JcClassOrInterface clazz) {
        String classStr = clazz.getName();
        // TODO: Handle phantom classes
        /*if (clazz.isPhantom()) {
            phantoms.reportPhantom("Interface", classStr);
            writePhantomType(c);
        }*/
        _db.add(clazz.isInterface() ? INTERFACE_TYPE : CLASS_TYPE, classStr);
        writeClassHeap(Representation.classConstant(clazz), classStr);
        // TODO: Handle annotations. Only for flow-sensitivity? getAnnotations и filter по visible
        for (JcAnnotation jcAnnotation : clazz.getAnnotations().stream().filter(it -> it.getVisible()).collect(Collectors.toList())) {
            _db.add(TYPE_ANNOTATION, classStr, getAnnotationType(jcAnnotation));
            //writeAnnotationElements("type", classStr, null, jcAnnotation.getValues());
        }
        // TODO: Do we need type simple name?
        //_db.add(TYPE_SIMPLENAME, classStr, clazz.getShortName());
    }

    void writeClassModifier(JcClassOrInterface c, String modifier) {
        writeClassModifier(c.getName(), modifier);
    }

    void writeClassModifier(String c, String modifier) {
        this._db.add(PredicateFile.CLASS_MODIFIER, modifier, new String[]{c});
    }


    private static String getAnnotationType(JcAnnotation jcAnnotation) {
        return TypeUtils.raiseTypeId(jcAnnotation.getName());
    }

    void writeClassHeap(String heap, String className) {
        this._db.add(PredicateFile.CLASS_HEAP, heap, new String[]{className});
        if (this._extractMoreStrings) {
            this.writeStringConstant(className);
        }

    }

    void writeDirectSuperclass(JcClassOrInterface sub, JcClassOrInterface sup) {
        _db.add(DIRECT_SUPER_CLASS, writeType(sub), writeType(sup));
    }

    void writeDirectSuperinterface(JcClassOrInterface clazz, JcClassOrInterface iface) {
        _db.add(DIRECT_SUPER_IFACE, writeType(clazz), writeType(iface));
    }

    private String writeType(JcClassOrInterface c) {
        return c.getName();
    }

    String writeField(JcField f, JcClasspath cp) {
        if (f == null) {
            System.err.println("WARNING: null field encountered.");
            return null;
        }
        String fieldId = _rep.signature(f);
        JcType fieldType = getTypeByName(f.getType(), cp);
        if (fieldType == null) {
            _db.add(FIELD_SIGNATURE, fieldId, writeType(f.getEnclosingClass()), Representation.simpleName(f), f.getType().getTypeName());
            System.out.println("WARNING: null fieldType encountered.");
        } else
            _db.add(FIELD_SIGNATURE, fieldId, writeType(f.getEnclosingClass()), Representation.simpleName(f), writeType(fieldType));
        // TODO: Handle field annotations
        for (JcAnnotation jcAnnotation : f.getAnnotations().stream().filter(it -> it.getVisible()).collect(Collectors.toList())) {
            _db.add(FIELD_ANNOTATION, fieldId, getAnnotationType(jcAnnotation));
            //writeAnnotationElements("field", fieldId, null, aTag.getElems());
        }
        return fieldId;
    }

    void writeFieldInitialValue(JcField f) {
        String fieldId = _rep.signature(f);
        // TODO: завести feature issue или разобраться как этот таг и константу soot записывает
        /*
        List<Tag> tagList = f.getTags();
        for (Tag tag : tagList)
            if (tag instanceof ConstantValueTag) {
                String val = ((ConstantValueTag)tag).getConstant().toString();
                _db.add(FIELD_INITIAL_VALUE, fieldId, val);
                // Put constant in appropriate "raw" input facts.
                String tagType = null;
                if (tag instanceof IntegerConstantValueTag)
                    tagType = "int";
                else if (tag instanceof DoubleConstantValueTag)
                    tagType = "double";
                else if (tag instanceof LongConstantValueTag)
                    tagType = "long";
                else if (tag instanceof FloatConstantValueTag)
                    tagType = "float";
                if (tagType != null) {
                    // Trim last non-digit qualifier (e.g. 'L' in long constants).
                    int len = val.length();
                    if (!Character.isDigit(val.charAt(len-1)))
                        val = val.substring(0, len-1);
                    writeNumConstantRaw(val, tagType);
                } else if (tag instanceof StringConstantValueTag) {
                    writeStringConstant(val);
                } else
                    System.err.println("Unsupported field tag " + tag.getClass());
            }
        */
    }

    void writeFieldModifier(JcField f, String modifier) {
        String fieldId = _rep.signature(f);
        _db.add(FIELD_MODIFIER, modifier, fieldId);
    }

    void writeMethodModifier(String methodId, String modifier) {
        _db.add(METHOD_MODIFIER, modifier, methodId);
    }

    void writeMethodDeclaresException(JcMethod m, JcClassOrInterface exception) {
        writeMethodDeclaresException(methodSig(m, null), writeType(exception));
    }

    JcLocal writeNumConstantExpression(String methodId, JcExpr constant,
                                     JcType explicitType, SessionCounter session) {
        JcType constantType = (explicitType == null) ? constant.getType() : explicitType;
        // introduce a new temporary variable
        FreshAssignLocal fal = newAssignForFreshLocal(methodId, "$numconstant", constantType, session);
        JcLocal l = fal.local;
        writeAssignNumConstant(fal.ii, l, constant);
        return l;
    }

    void writeAssignNumConstant(InstrInfo ii, JcLocal l, JcExpr constant) {
        String methodId = ii.methodId;
        _db.add(ASSIGN_NUM_CONST, ii.insn, str(ii.index), constant.toString(), _rep.local(methodId, l), methodId);
    }

    JcLocal writeNullExpression(String methodId, JcType type, SessionCounter session) {
        // introduce a new temporary variable
        FreshAssignLocal fal = newAssignForFreshLocal(methodId, "$null", type, session);
        JcLocal l = fal.local;
        writeAssignNull(fal.ii, l);
        return l;
    }

    void writeAssignNull(InstrInfo ii, JcLocal l) {
        String methodId = ii.methodId;
        _db.add(ASSIGN_NULL, ii.insn, str(ii.index), _rep.local(methodId, l), methodId);
    }

    private String invokeMethodSig(String insn, String declClass, String simpleName, JcMethod exprMethodRef, JcCallExpr expr) {
        /*if (!simpleName.equals("<init>") && DoopAddons.polymorphicHandling(declClass, simpleName)) {
            _db.add(POLYMORPHIC_INVOCATION, insn, simpleName);
            return exprMethodRef.toString();
        } else*/
            return _rep.signature(expr.getMethod().getMethod());
    }

    void writeAssignHeapAllocation(JcInst stmt, InstrInfo ii, JcLocal l, JcExpr expr, SessionCounter session) {
        String methodId = ii.methodId;
        String heap = _rep.heapAlloc(methodId, expr, session);

        _db.add(NORMAL_HEAP, heap, writeType(expr.getType()));
        _db.add(ASSIGN_HEAP_ALLOC, ii.insn, str(ii.index), heap, _rep.local(methodId, l), methodId, ""+ stmt.getLineNumber());

        if (expr instanceof JcNewArrayExpr) {
            JcNewArrayExpr newArray = (JcNewArrayExpr) expr;
            writeArraySize(ii, newArray.getDimensions().get(0), 0, heap);
        }
    }

    private void writeArraySize(InstrInfo ii, JcValue sizeVal, int pos, String heap) {
        if (sizeVal instanceof JcNumericConstant) {
            JcNumericConstant size = (JcNumericConstant) sizeVal;
            _db.add(ARRAY_ALLOC_CONST_SIZE, ii.insn, str(pos), str(size.getValue().intValue()));
            if(size.getValue().intValue() == 0) _db.add(EMPTY_ARRAY, heap);
        }
        else if (sizeVal instanceof JcLocal)
            _db.add(ARRAY_ALLOC, ii.insn, str(pos), _rep.local(ii.methodId, (JcLocal) sizeVal));
    }

    void writeLoadStaticField(InstrInfo ii, JcField f, JcLocal to, JcClasspath cp) {
        writeStaticField(ii, f, to, LOAD_STATIC_FIELD, cp);
    }

    private void writeStaticField(InstrInfo ii, JcField f, JcLocal var, PredicateFile staticFieldFacts, JcClasspath cp) {
        String methodId = ii.methodId;
        String fieldId = writeField(f, cp);
        if (fieldId != null)
            _db.add(staticFieldFacts, ii.insn, str(ii.index), _rep.local(methodId, var), fieldId, methodId);
    }

    void writeAssignCast(InstrInfo ii, JcLocal to, JcLocal from, JcType t) {
        String methodId = ii.methodId;
        _db.add(ASSIGN_CAST, ii.insn, str(ii.index), _rep.local(methodId, from), _rep.local(methodId, to), writeType(t), methodId);
    }

    void writeAssignCastNumericConstant(InstrInfo ii, JcLocal to, JcExpr constant, JcType t) {
        String methodId = ii.methodId;
        String val = constant.toString();
        writeNumConstantRaw(val, constant.getType().getTypeName());
        _db.add(ASSIGN_CAST_NUM_CONST, ii.insn, str(ii.index), val, _rep.local(methodId, to), writeType(t), methodId);
    }

    void writeAssignCastNull(InstrInfo ii, JcLocal to, JcType t) {
        String methodId = ii.methodId;
        _db.add(ASSIGN_CAST_NULL, ii.insn, str(ii.index), _rep.local(methodId, to), writeType(t), methodId);
    }

    void writeAssignThisToLocal(InstrInfo ii, JcLocal to) {
        String methodId = ii.methodId;
        writeAssignLocal(ii.insn, ii.index, _rep.thisVar(methodId), _rep.local(methodId, to), methodId);
    }

    void writePhiAssign(String methodId, JcAssignInst stmt, JcLocal left, JcPhiExpr phiExpr, SessionCounter session) {
        Collection<InstrInfo> newAssignments = new ArrayList<>();
        for (JcValue alternative : (phiExpr).getValues()) {
            InstrInfo altInstrInfo = new InstrInfo(methodId, "phi-assign", session);
            writeAssignLocal(altInstrInfo, left, (JcLocal) alternative);
            newAssignments.add(altInstrInfo);
        }
        expandedPhiNodes.put(stmt, newAssignments);
    }

    void writeAssignBinop(InstrInfo ii, JcLocal left, JcBinaryExpr right) {
        String methodId = ii.methodId;
        String insn = ii.insn;
        writeAssignBinop(insn, ii.index, _rep.local(methodId, left), methodId);

        if (right instanceof JcAddExpr)
            writeOperatorAt(insn, "+");
        else if (right instanceof JcSubExpr)
            writeOperatorAt(insn, "-");
        else if (right instanceof JcMulExpr)
            writeOperatorAt(insn, "*");
        else if (right instanceof JcDivExpr)
            writeOperatorAt(insn, "/");
        else if (right instanceof JcRemExpr)
            writeOperatorAt(insn, "%");
        else if (right instanceof JcAndExpr)
            writeOperatorAt(insn, "&");
        else if (right instanceof JcOrExpr)
            writeOperatorAt(insn, "|");
        else if (right instanceof JcXorExpr)
            writeOperatorAt(insn, "^");
        else if (right instanceof JcShlExpr)
            writeOperatorAt(insn, "<<");
        else if (right instanceof JcShrExpr)
            writeOperatorAt(insn, ">>");
        else if (right instanceof JcUshrExpr)
            writeOperatorAt(insn, ">>>");
        else if (right instanceof JcCmpExpr)
            writeOperatorAt(insn, "cmp");
        else if (right instanceof JcCmplExpr)
            writeOperatorAt(insn, "cmpl");
        else if (right instanceof JcCmpgExpr)
            writeOperatorAt(insn, "cmpg");
        else
            writeOperatorAt(insn, "??");

        if (right.getLhv() instanceof JcLocal) {
            JcLocal op1 = (JcLocal) right.getLhv();
            writeAssignOperFrom(insn, L_OP, _rep.local(methodId, op1));
        } else if (right.getLhv() instanceof JcNumericConstant) {
            JcNumericConstant cons = (JcNumericConstant) right.getLhv();
            writeAssignOperFromConstant(insn, L_OP, cons.toString());
        }

        if (right.getRhv() instanceof JcLocal) {
            JcLocal op2 = (JcLocal) right.getRhv();
            writeAssignOperFrom(insn, R_OP, _rep.local(methodId, op2));
        } else if (right.getRhv() instanceof JcNumericConstant) {
            JcNumericConstant cons = (JcNumericConstant) right.getRhv();
            writeAssignOperFromConstant(insn, R_OP, cons.toString());
        }
    }

    void writeAssignInstanceOf(InstrInfo ii, JcLocal to, JcLocal from, JcType t) {
        String methodId = ii.methodId;
        _db.add(ASSIGN_INSTANCE_OF, ii.insn, str(ii.index), _rep.local(methodId, from), _rep.local(methodId, to), writeType(t), methodId);
    }

    JcLocal writeClassConstantExpression(String methodId, JcClassConstant constant, SessionCounter session, JcClasspath cp) {
        ClassConstantInfo info = new ClassConstantInfo(constant, cp);
        // introduce a new temporary variable
        FreshAssignLocal fal = info.isMethodType ?
                newAssignForFreshLocal(methodId, "$methodtypeconstant", cp.findTypeOrNull("java.lang.invoke.MethodType"), session) :
                newAssignForFreshLocal(methodId, "$classconstant", cp.findTypeOrNull("java.lang.Class"), session);
        JcLocal l = fal.local;
        writeAssignClassConstant(fal.ii, l, info);
        return l;
    }

    private void writeAssignClassConstant(InstrInfo ii, JcLocal l, ClassConstantInfo info) {
        if (info.isMethodType)
            writeMethodTypeConstant(info.heap);
        else
            writeClassHeap(info.heap, info.actualType);

        // REVIEW: the class object is not explicitly written. Is this always ok?
        String methodId = ii.methodId;
        _db.add(ASSIGN_HEAP_ALLOC, ii.insn, str(ii.index), info.heap, _rep.local(methodId, l), methodId, "0");
    }

    void writeAssignClassConstant(InstrInfo ii, JcLocal l, JcClassConstant constant, JcClasspath cp) {
        writeAssignClassConstant(ii, l, new ClassConstantInfo(constant, cp));
    }

    void writeStoreArrayIndex(JcInst stmt, InstrInfo ii, JcLocal base, JcLocal from, JcValue arrIndex) {
        writeLoadOrStoreArrayIndex(stmt, ii, base, from, arrIndex, STORE_ARRAY_INDEX);
    }

    private void writeLoadOrStoreArrayIndex(JcInst stmt, InstrInfo ii, JcLocal base, JcLocal var, JcValue arrIndex, PredicateFile predicateFile) {
        String methodId = ii.methodId;
        String insn = ii.insn;
        _db.add(predicateFile, insn, str(ii.index), _rep.local(methodId, var), _rep.local(methodId, base), methodId);

        if (arrIndex instanceof JcLocal)
            _db.add(ARRAY_INSN_INDEX, insn, _rep.local(methodId, (JcLocal) arrIndex));
        else if (arrIndex instanceof JcNumericConstant && arrIndex.getType().getTypeName().equals("int"))
            _db.add(ARRAY_NUM_INDEX, insn, str((int)((JcNumericConstant) arrIndex).getValue()));
        else
            System.out.println("Cannot handle assignment: " + stmt + " (index: " + arrIndex.getClass() + ")");
    }

    void writeGoto(JcGotoInst stmt, InstrInfo ii, SessionCounter session) {
        session.calcInstructionIndex(stmt);
        writeWithPossiblePhiTarget(stmt.getTarget(), session, (indexTo -> _db.add(GOTO, ii.insn, str(ii.index), str(indexTo), ii.methodId)));
    }

    void writeWithPossiblePhiTarget(JcInstRef target, SessionCounter session,
                                    Consumer<Integer> writerLambda) {
        Collection<InstrInfo> phiNodes = expandedPhiNodes.get(target);
        if (phiNodes == null) {
            session.calcInstructionIndex(target);
            int indexTo = session.getInstructionIndex(target);
            writerLambda.accept(indexTo);
        } else {
            Collection<Integer> targetIndices = phiNodes.stream().map(ii -> ii.index).collect(Collectors.toList());
            for (int indexTo : targetIndices)
                writerLambda.accept(indexTo);
        }
    }

    void writeIf(JcIfInst stmt, InstrInfo ii, int indexTo, JcClasspath cp) {
        // index was already computed earlier
        int index = ii.index;
        String insn = ii.insn;

        String methodId = ii.methodId;
        writeIf(insn, index, indexTo, methodId);

        JcConditionExpr condition = stmt.getCondition();

        JcLocal dummy = new JcLocal() {
            @NotNull
            @Override
            public String getName() {
                return "tmp" + insn;
            }

            @NotNull
            @Override
            public JcType getType() {
                return cp.findTypeOrNull("bool");
            }

            @Override
            public <T> T accept(@NotNull JcExprVisitor<T> jcExprVisitor) {
                return null;
            }
        };
        writeDummyIfVar(insn, _rep.local(methodId, dummy));

        if (condition instanceof JcEqExpr)
            writeOperatorAt(insn, "==");
        else if (condition instanceof JcNeqExpr)
            writeOperatorAt(insn, "!=");
        else if (condition instanceof JcGeExpr)
            writeOperatorAt(insn, ">=");
        else if (condition instanceof JcGtExpr)
            writeOperatorAt(insn, ">");
        else if (condition instanceof JcLeExpr)
            writeOperatorAt(insn, "<=");
        else if (condition instanceof JcLtExpr)
            writeOperatorAt(insn, "<");

        if (condition.getLhv() instanceof JcLocal) {
            JcLocal op1 = (JcLocal) condition.getLhv();
            writeIfVar(insn, L_OP, _rep.local(methodId, op1));
        } else if (condition.getLhv() instanceof JcNumericConstant) {
            JcNumericConstant op1 = (JcNumericConstant) condition.getLhv();
            writeIfConstant(insn, L_OP, op1.toString());
        }

        if (condition.getRhv() instanceof JcLocal) {
            JcLocal op2 = (JcLocal) condition.getRhv();
            writeIfVar(insn, R_OP, _rep.local(methodId, op2));
        } else if (condition.getRhv() instanceof JcNumericConstant) {
            JcNumericConstant op2 = (JcNumericConstant)condition.getRhv();
            writeIfConstant(insn, R_OP, op2.toString());
        }
    }

    void writeReturn(InstrInfo ii, JcLocal l) {
        String methodId = ii.methodId;
        _db.add(RETURN, ii.insn, str(ii.index), _rep.local(methodId, l), methodId);
    }

    void writeReturnVoid(InstrInfo ii) {
        _db.add(RETURN_VOID, ii.insn, str(ii.index), ii.methodId);
    }

    void writeStoreInstanceField(InstrInfo ii, JcField f, JcLocal base, JcLocal from, JcClasspath cp) {
        writeInstanceField(ii, f, base, from, STORE_INST_FIELD, cp);
    }

    void writeLoadInstanceField(InstrInfo ii, JcField f, JcLocal base, JcLocal to, JcClasspath cp) {
        writeInstanceField(ii, f, base, to, LOAD_INST_FIELD, cp);
    }

    private void writeInstanceField(InstrInfo ii, JcField f, JcLocal base, JcLocal var, PredicateFile storeOrLoadInstField, JcClasspath cp) {
        String methodId = ii.methodId;
        String fieldId = writeField(f, cp);
        if (base == null || var == null) {
            System.out.println(ii);
        }
        if (fieldId != null)
            _db.add(storeOrLoadInstField, ii.insn, str(ii.index), _rep.local(methodId, var), _rep.local(methodId, base), fieldId, methodId);
    }
}
