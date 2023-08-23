package datalogpt.src;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.jacodb.api.*;
import org.jacodb.api.cfg.*;
import org.jacodb.api.ext.HierarchyExtension;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;

import javax.sound.midi.Sequence;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FactGeneratorTask implements JcClassProcessingTask {
    //private HierarchyExtension hierarchyExt;
    private final JcClasspath _cp;
    private final FactWriter _writer = new FactWriter();
    /*private final ConcurrentHashMap.KeySetView<String, Boolean> allocMap = ConcurrentHashMap.<String>newKeySet();
    private final ConcurrentHashMap.KeySetView<String, Boolean> assignMap = ConcurrentHashMap.<String>newKeySet();
    private final ConcurrentHashMap.KeySetView<String, Boolean> storeMap = ConcurrentHashMap.<String>newKeySet();
    private final ConcurrentHashMap.KeySetView<String, Boolean> loadMap = ConcurrentHashMap.<String>newKeySet();
    private final ConcurrentHashMap.KeySetView<String, Boolean> sCallMap = ConcurrentHashMap.<String>newKeySet();
    private final ConcurrentHashMap.KeySetView<String, Boolean> vCallMap = ConcurrentHashMap.<String>newKeySet();
    private final ConcurrentHashMap.KeySetView<String, Boolean> actualReturnMap = ConcurrentHashMap.<String>newKeySet();
    private final ConcurrentHashMap.KeySetView<String, Boolean> formalReturnMap = ConcurrentHashMap.<String>newKeySet();
    private final ConcurrentHashMap.KeySetView<String, Boolean> actualArgMap = ConcurrentHashMap.<String>newKeySet();
    private final ConcurrentHashMap.KeySetView<String, Boolean> formalArgMap = ConcurrentHashMap.<String>newKeySet();
    private final ConcurrentHashMap.KeySetView<String, Boolean> mainMethodMap = ConcurrentHashMap.<String>newKeySet();
    */
    FactGeneratorTask(JcClasspath classpath)
    {
        this._cp = classpath;
    }

    /*public void flush() {
        try {
            File factsDir = new File("out/facts");
            FileUtils.deleteDirectory(factsDir);
            factsDir.mkdirs();
            BufferedWriter allocWriter = new BufferedWriter(
                    new FileWriter(new File(factsDir, "Alloc.facts"), true));
            BufferedWriter assignWriter = new BufferedWriter(
                    new FileWriter(new File(factsDir, "Assign.facts"), true));
            BufferedWriter storeWriter = new BufferedWriter(
                    new FileWriter(new File(factsDir, "Store.facts"), true));
            BufferedWriter loadWriter = new BufferedWriter(
                    new FileWriter(new File(factsDir, "Load.facts"), true));
            BufferedWriter sCallWriter = new BufferedWriter(
                    new FileWriter(new File(factsDir, "SCall.facts"), true));
            BufferedWriter vCallWriter = new BufferedWriter(
                    new FileWriter(new File(factsDir, "VCall.facts"), true));
            BufferedWriter actualReturnWriter = new BufferedWriter(
                    new FileWriter(new File(factsDir, "ActualReturn.facts"), true));
            BufferedWriter formalReturnWriter = new BufferedWriter(
                    new FileWriter(new File(factsDir, "FormalReturn.facts"), true));
            BufferedWriter actualArgWriter = new BufferedWriter(
                    new FileWriter(new File(factsDir, "ActualArg.facts"), true));
            BufferedWriter formalArgWriter = new BufferedWriter(
                    new FileWriter(new File(factsDir, "FormalArg.facts"), true));
            BufferedWriter mainMethodWriter = new BufferedWriter(
                    new FileWriter(new File(factsDir, "MainMethod.facts"), true));

            flushMap(allocMap, allocWriter);
            flushMap(assignMap, assignWriter);
            flushMap(storeMap, storeWriter);
            flushMap(loadMap, loadWriter);
            flushMap(sCallMap, sCallWriter);
            flushMap(vCallMap, vCallWriter);
            flushMap(actualReturnMap, actualReturnWriter);
            flushMap(formalReturnMap, formalReturnWriter);
            flushMap(actualArgMap, actualArgWriter);
            flushMap(formalArgMap, formalArgWriter);
            flushMap(mainMethodMap, mainMethodWriter);

            allocWriter.close();
            assignWriter.close();
            storeWriter.close();
            loadWriter.close();
            sCallWriter.close();
            vCallWriter.close();
            actualReturnWriter.close();
            formalReturnWriter.close();
            actualArgWriter.close();
            formalArgWriter.close();
            mainMethodWriter.close();

            allocMap.clear();
            assignMap.clear();
            storeMap.clear();
            loadMap.clear();
            sCallMap.clear();
            vCallMap.clear();
            actualReturnMap.clear();
            formalReturnMap.clear();
            actualArgMap.clear();
            formalArgMap.clear();
            mainMethodMap.clear();

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void flushMap(ConcurrentHashMap.KeySetView<String, Boolean> map, BufferedWriter bf) {
        try {
            for (String fact : map) {
                bf.write(fact);
                bf.newLine();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }*/

    public void addMainClass(JcClassOrInterface clazz) {
        _writer.writeMainClass(clazz);
    }

    public void flush() {
        _writer.flush();
    }

    @Override
    public void process(@NotNull JcClassOrInterface clazz) {
        _writer.writeApplicationClass(clazz);
        _writer.writeClassOrInterfaceType(clazz);

        for (String mod : getModifiers(clazz.getAccess(), false))
            if (!mod.trim().equals(""))
                _writer.writeClassModifier(clazz, mod);

        // the isInterface condition prevents Object as superclass of interface
        if (clazz.getSuperClass() != null && !clazz.isInterface()) {
            _writer.writeDirectSuperclass(clazz, clazz.getSuperClass());
        }

        for (JcClassOrInterface i : clazz.getInterfaces()) {
            _writer.writeDirectSuperinterface(clazz, i);
        }
        clazz.getDeclaredFields().forEach(this::generate);

        for (JcMethod m : clazz.getDeclaredMethods()) {
            SessionCounter session = new SessionCounter();
            try {
                generate(m, session);
            } catch (Throwable t) {
                // Map<Thread,StackTraceElement[]> liveThreads = Thread.getAllStackTraces();
                // for (Iterator<Thread> i = liveThreads.keySet().iterator(); i.hasNext(); ) {
                //     Thread key = i.next();
                //     System.err.println("Thread " + key.getName());
                //     StackTraceElement[] trace = liveThreads.getLibrary(key);
                //     for (int j = 0; j < trace.length; j++) {
                //         System.err.println("\tat " + trace[j]);
                //     }
                // }
                String msg = "Error while processing method: " + m + ": " + t.getMessage();
                System.err.println(msg);
            }
            /*String context = clazz.getName() + ":" + m.getName();
            //System.out.println(context);
            List<JcParameter> parameters = m.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                formalArgMap.add(context + factSep + i + factSep + context + ":arg$" + i);
            }
            try {
                for (JcInst inst : m.getInstList()) {
                    if (inst instanceof JcAssignInst) {
                        if (((JcAssignInst) inst).getLhv() instanceof JcLocalVar & ((JcAssignInst) inst)
                                .getRhv() instanceof JcNewExpr) {
                            allocMap.add(context + ":" + ((JcAssignInst) inst).getLhv() + factSep
                                    + context + ":" + inst.getLineNumber() + ":<" + ((JcAssignInst) inst).getRhv()
                                    + ">" + factSep + context);
                        }
                        if (((JcAssignInst) inst).getLhv() instanceof JcLocalVar & ((JcAssignInst) inst)
                                .getRhv() instanceof JcLocalVar){
                            assignMap.add(context + ":" + ((JcAssignInst) inst).getLhv()
                                    + factSep + context + ":" + ((JcAssignInst) inst).getRhv());
                        }
                        if (((JcAssignInst) inst).getLhv() instanceof JcLocalVar & ((JcAssignInst) inst)
                                .getRhv() instanceof JcFieldRef){
                            JcFieldRef fieldRef = (JcFieldRef) ((JcAssignInst) inst).getRhv();
                            loadMap.add(context + ":" + ((JcAssignInst) inst).getLhv()
                                    + factSep + context + ":" + fieldRef.getInstance() + factSep + fieldRef.getField().getName());
                        }
                        if (((JcAssignInst) inst).getLhv() instanceof JcFieldRef & ((JcAssignInst) inst)
                                .getRhv() instanceof JcLocalVar){
                            JcFieldRef fieldRef = (JcFieldRef) ((JcAssignInst) inst).getLhv();
                            storeMap.add(context + ":" + fieldRef.getInstance() + factSep + fieldRef.getField().getName()
                                    + factSep + context + ":" + ((JcAssignInst) inst).getRhv());
                        }
                        if (((JcAssignInst) inst).getLhv() instanceof JcLocalVar & ((JcAssignInst) inst)
                                .getRhv() instanceof JcStaticCallExpr) {
                            String called_method = ((JcStaticCallExpr) ((JcAssignInst) inst).getRhv()).getMethod()
                                    .getName();
                            String enclosing_class = ((JcStaticCallExpr) ((JcAssignInst) inst).getRhv()).getMethod()
                                    .getEnclosingType().getJcClass().getName();
                            sCallMap.add(enclosing_class + ":" + called_method + factSep + context + ":"
                                    + inst.getLineNumber() + ":" + called_method + factSep + context);
                            String to = ((JcAssignInst) inst).getLhv().toString();
                            actualReturnMap.add(context + ":" + inst.getLineNumber() + ":" + called_method
                                    + factSep + context + ":" + to);
                            List<JcValue> args = ((JcStaticCallExpr) ((JcAssignInst) inst).getRhv()).getArgs();
                            for (int i = 0; i < args.size(); i++) {
                                actualArgMap.add(context + ":" + inst.getLineNumber() + ":" + called_method
                                        + factSep + i + factSep + context + ":"
                                        + StringEscapeUtils.escapeJava(args.get(i).toString()));
                            }
                        }
                    } else if (inst instanceof JcCallInst) {
                        if (((JcCallInst) inst).getCallExpr() instanceof JcStaticCallExpr) {
                            String called_method = ((JcCallInst) inst).getCallExpr().getMethod().getName();
                            String enclosing_class = ((JcCallInst) inst).getCallExpr().getMethod().getEnclosingType()
                                    .getJcClass().getName();
                            sCallMap.add(enclosing_class + ":" + called_method + factSep + context + ":"
                                    + inst.getLineNumber() + ":" + called_method + factSep + context);
                            List<JcValue> args = ((JcCallInst) inst).getCallExpr().getArgs();
                            for (int i = 0; i < args.size(); i++) {
                                actualArgMap.add(context + ":" + inst.getLineNumber() + ":" + called_method
                                        + factSep + i + factSep + context + ":"
                                        + StringEscapeUtils.escapeJava(args.get(i).toString()));
                            }
                        }*/
                        /*else if (((JcCallInst) inst).getCallExpr() instanceof JcVirtualCallExpr) {
                        String called_method = ((JcCallInst) inst).getCallExpr().getMethod().getName();
                        String enclosing_class = ((JcCallInst) inst).getCallExpr().getMethod().getEnclosingType()
                                .getJcClass().getName();
                        //Sequence overrides = (Sequence) hierarchyExt.findOverrides(((JcCallInst) inst).getCallExpr().getMethod().getMethod(), false);
                        vCallMap.add(enclosing_class + ":" + called_method + factSep + context + ":"
                                + inst.getLineNumber() + ":" + called_method + factSep + context);
                        List<JcValue> args = ((JcCallInst) inst).getCallExpr().getArgs();
                        for (int i = 0; i < args.size(); i++) {
                            actualArgMap.add(context + ":" + inst.getLineNumber() + ":" + called_method
                                    + factSep + i + factSep + context + ":"
                                    + StringEscapeUtils.escapeJava(args.get(i).toString()));
                        }
                        }*/
            /*
                    } else if (inst instanceof JcReturnInst) {
                        JcValue value = ((JcReturnInst) inst).getReturnValue();
                        if (value == null) {
                            formalReturnMap.add(context + factSep + "void");
                        } else {
                            formalReturnMap.add(context + factSep + context + ":" + value);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(context + "---" + e);
            }
            */
        }
    }

    @Override
    public void process(@NotNull ClassSource source, @NotNull JcClasspath classpath) {
        System.out.println(classpath.toJcClass(source).getName());
        process(classpath.toJcClass(source));
    }

    private static Collection<String> getModifiers(int modifiers, boolean isMethod) {
        // Take the modifiers from Soot, so that we are robust against
        // changes of the JVM spec.
        String[] modifierStrings = Modifier.toString(modifiers).split(" ");
        // Fix modifiers that mean different things for methods.
        if (isMethod)
            for (int i = 0; i < modifierStrings.length; i++)
                if ("transient".equals(modifierStrings[i]))
                    modifierStrings[i] = "varargs";
                else if ("volatile".equals(modifierStrings[i]))
                    modifierStrings[i] = "bridge";
        // Handle modifiers that are not in the Modifier.toString() output.
        Collection<String> ret = new ArrayList<>(Arrays.asList(modifierStrings));
        if(Modifier.isSynthetic(modifiers))
            ret.add("synthetic");
        if(Modifier.isConstructor(modifiers))
            ret.add("constructor");
        if(Modifier.isDeclaredSynchronized(modifiers))
            ret.add("declared-synchronized");
        return ret;
    }

    private void generate(JcField f)
    {
        _writer.writeField(f, _cp);
        _writer.writeFieldInitialValue(f);
        for (String m : getModifiers(f.getAccess(), false))
            _writer.writeFieldModifier(f, m);
    }

    void generate(JcMethod m, SessionCounter session) {
        String metId = _writer.writeMethod(m, _cp);

        //TODO: Handle phantom methods
        /*if (m.isPhantom()) {
            _writer.writePhantomMethod(metId);
            return;
        }

        if (isPhantomBased(m))
            _writer.writePhantomBasedMethod(metId);
        */

        for (String mod : getModifiers(m.getAccess(), true))
            _writer.writeMethodModifier(metId, mod);

        if (!m.isStatic())
            _writer.writeThisVar(metId, m.getEnclosingClass());

        if (m.isNative()) {
            _writer.writeNativeMethodId(metId, m.getEnclosingClass().toString(), _writer._rep.simpleName(m));
            _writer.writeNativeReturnVar(metId, m.getReturnType(), _cp);
        }

        for(int i = 0 ; i < m.getParameters().size(); i++)
            _writer.writeFormalParam(metId, m.getParameters().get(i).getType(), i, _cp);

        // TODO: How we can get exception classes?
        /*for(JcClassOrInterface clazz: m.getExceptions())
            _writer.writeMethodDeclaresException(m, clazz);*/

        if(!(m.isAbstract() || m.isNative())) {
            // TODO: What is active bodies in JacoDB?
            try {
                String methodId = _writer._rep.signature(m);
                // TODO: Handle JcLocals
                /*for(Local l : b.getLocals())
                    _writer.writeLocal(methodId, l);*/

                Map<JcInst, InstrInfo> iis = new ConcurrentHashMap<>();
                for (JcInst inst : m.getInstList()) {
                    String insn = Representation.numberedInstructionId(methodId, Representation.getKind(inst), session);
                    InstrInfo ii = new InstrInfo(_writer.methodSig(m, null), insn, session.calcInstructionIndex(inst));
                    iis.put(inst, ii);
                    if (inst instanceof JcAssignInst) {
                        generate(m, (JcAssignInst) inst, ii, session);
                    } /*else if (inst instanceof IdentityStmt) {
                        generate((IdentityStmt) inst, ii, session);
                    }*/ else if (inst instanceof JcCallInst) {
                        _writer.writeInvoke(m, inst, null, ii, session, _cp);
                    } else if (inst instanceof JcReturnInst) {
                        generate((JcReturnInst) inst, _cp.findTypeOrNull(m.getReturnType().getTypeName()), ii, session);
                    } /*else if (u instanceof ThrowStmt) {
                        generate(methodId, (ThrowStmt) u, ii, session);
                    }*/ else if ((inst instanceof JcGotoInst) || (inst instanceof JcIfInst) ||
                            (inst instanceof JcSwitchInst)/* || (inst instanceof NopStmt)*/) {
                        // processed in second run: we might not know the number of
                        // the unit yet.
                    }/* else if (inst instanceof JcEnterMonitorInst) {
                        //TODO: how to handle EnterMonitorStmt when op is not a Local?
                        JcEnterMonitorInst stmt = (JcEnterMonitorInst) inst;
                        if (stmt.getOp() instanceof Local)
                            _writer.writeEnterMonitor(ii, (Local) stmt.getOp());
                    } else if (u instanceof ExitMonitorStmt) {
                        //TODO: how to handle ExitMonitorStmt when op is not a Local?
                        ExitMonitorStmt stmt = (ExitMonitorStmt) u;
                        if (stmt.getOp() instanceof Local)
                            _writer.writeExitMonitor(ii, (Local) stmt.getOp());
                    }*/ else {
                        System.out.println("Cannot handle instruction: " + inst);
                    }
                }

                for(JcInst inst : m.getInstList()) {
                    if (inst instanceof JcGotoInst) {
                        _writer.writeGoto((JcGotoInst)inst, iis.get(inst), session);
                    } else if (inst instanceof JcIfInst) {
                        JcIfInst ifStmt = (JcIfInst)inst;
                        _writer.writeWithPossiblePhiTarget(ifStmt.getTrueBranch(), session, (indexTo -> _writer.writeIf(ifStmt, iis.get(inst), indexTo, _cp)));
                    }/* else if (u instanceof TableSwitchStmt) {
                        _writer.writeTableSwitch((TableSwitchStmt) u, iis.get(u), session);
                    } else if (u instanceof LookupSwitchStmt) {
                        _writer.writeLookupSwitch((LookupSwitchStmt) u, iis.get(u), session);
                    } else if (!(inst instanceof JcInst)) {
                        System.out.println("Not a statement: " + u);
                    }*/
                }

                /*Trap previous = null;
                for (Trap t : b.getTraps()) {
                    _writer.writeExceptionHandler(methodId, t, session);
                    if (previous != null)
                        _writer.writeExceptionHandlerPrevious(methodId, t, previous, session);
                    previous = t;
                }*/

            } catch (RuntimeException ex) {
                System.out.println("Fact generation failed for method " + m + ".");
                //ex.printStackTrace();
                //throw ex;
            }
        }
    }

    private void generate(JcMethod inMethod, JcAssignInst stmt, InstrInfo ii, SessionCounter session) {
        if (stmt.getLhv() instanceof JcLocal)
            generateAssignToLocal(inMethod, stmt, ii, session);
        else
            generateAssignToNonLocal(stmt, ii, session);
    }

    private void generateAssignToLocal(JcMethod inMethod, JcAssignInst stmt, InstrInfo ii, SessionCounter session) {
        JcLocal left = (JcLocal) stmt.getLhv();
        JcExpr right = stmt.getRhv();

        if (right instanceof JcLocal)
            _writer.writeAssignLocal(ii, left, (JcLocal) right);
        else if (right instanceof JcCallExpr)
            _writer.writeAssignInvoke(inMethod, stmt, (JcCallExpr) right, ii, left, session, _cp);
        else if (right instanceof JcNewExpr || right instanceof JcNewArrayExpr)
            _writer.writeAssignHeapAllocation(stmt, ii, left, right, session);
        /*else if (right instanceof NewMultiArrayExpr)
            _writer.writeAssignNewMultiArrayExpr(stmt, ii, left, (NewMultiArrayExpr) right, session);*/
        else if (right instanceof JcStringConstant)
            _writer.writeAssignStringConstant(stmt, ii, left, (JcStringConstant) right);
        else if (right instanceof JcClassConstant)
            _writer.writeAssignClassConstant(ii, left, (JcClassConstant) right, _cp);
        else if (right instanceof JcNumericConstant || right instanceof JcInt || right instanceof JcFloat)
            _writer.writeAssignNumConstant(ii, left, right);
        else if (right instanceof JcNullConstant) {
            _writer.writeAssignNull(ii, left);
            // NoNullSupport: use the line below to remove Null Constants from the facts.
            // _writer.writeUnsupported(inMethod, stmt, session);
        } else if (right instanceof JcFieldRef) {
            JcFieldRef ref = (JcFieldRef) right;
            if (ref.getField().isStatic()) {
                _writer.writeLoadStaticField(ii, ref.getField().getField(), left, _cp);
            } else {
                _writer.writeLoadInstanceField(ii, ref.getField().getField(), (JcLocal) ref.getInstance(), left, _cp);
            }
        } else if (right instanceof JcLengthExpr) {
            //TODO
        } else if (right instanceof JcArrayAccess) {
            JcArrayAccess ref = (JcArrayAccess) right;
            JcLocal base = (JcLocal) ref.getArray();
            JcValue index = ref.getIndex();
            _writer.writeLoadArrayIndex(stmt, ii, base, left, index);
        } else if (right instanceof JcCastExpr) {
            JcCastExpr cast = (JcCastExpr) right;
            JcValue op = cast.getOperand();

            if (op instanceof JcLocal)
                _writer.writeAssignCast(ii, left, (JcLocal) op, cast.getType());
            else if (op instanceof JcNumericConstant || op instanceof JcInt || op instanceof JcFloat) {
                // seems to always get optimized out, do we need this?
                _writer.writeAssignCastNumericConstant(ii, left, op, cast.getType());
            } else if (op instanceof JcNullConstant || op instanceof JcClassConstant || op instanceof JcStringConstant)
                _writer.writeAssignCastNull(ii, left, cast.getType());
            else
                System.out.println("Cannot handle assignment: " + stmt + " (op: " + op.getClass() + ")");
        } else if (right instanceof JcPhiExpr) {
            _writer.writePhiAssign(_writer.methodSig(inMethod, null), stmt, left, (JcPhiExpr) right, session);
        } else if (right instanceof JcBinaryExpr)
            _writer.writeAssignBinop(ii, left, (JcBinaryExpr) right);
        /*else if (right instanceof UnopExpr)
            _writer.writeAssignUnop(ii, left, (UnopExpr) right);*/
        else if (right instanceof JcInstanceOfExpr) {
            JcInstanceOfExpr expr = (JcInstanceOfExpr) right;
            if (expr.getOperand() instanceof JcLocal)
                _writer.writeAssignInstanceOf(ii, left, (JcLocal) expr.getOperand(), expr.getType());
            /*else // TODO check if this is possible (instanceof on something that is not a local var)
                _writer.writeUnsupported(stmt, ii, session);*/
        } else
            System.out.println("Cannot handle assignment: " + stmt + " (right: " + right.getClass() + ")");
    }

    private void generateAssignToNonLocal(JcAssignInst stmt, InstrInfo ii, SessionCounter session) {
        JcValue left = stmt.getLhv();
        JcExpr right = stmt.getRhv();

        // first make sure we have local variable for the right-hand-side.
        JcLocal rightLocal = null;

        if (right instanceof JcLocal)
            rightLocal = (JcLocal) right;
        else if (right instanceof JcStringConstant)
            rightLocal = _writer.writeStringConstantExpression(stmt, ii.methodId, (JcStringConstant) right, session, _cp);
        else if (right instanceof JcNumericConstant || right instanceof JcInt || right instanceof JcFloat)
            rightLocal = _writer.writeNumConstantExpression(ii.methodId, right, left.getType(), session);
        else // NoNullSupport: use the line below to remove Null Constants from the facts.
            // _writer.writeUnsupported(inMethod, stmt, session);
            if (right instanceof JcNullConstant)
                rightLocal = _writer.writeNullExpression(ii.methodId, left.getType(), session);
            else if (right instanceof JcClassConstant)
                rightLocal = _writer.writeClassConstantExpression(ii.methodId, (JcClassConstant) right, session, _cp);
            /*else if (right instanceof MethodHandle)
                rightLocal = _writer.writeMethodHandleConstantExpression(ii.methodId, (MethodHandle) right, session);*/
            else
                System.out.println("Cannot handle rhs: " + stmt + " (right: " + right.getClass() + ")");

        // arrays
        //
        // NoNullSupport: use the line below to remove Null Constants from the facts.
        // if(left instanceof ArrayRef && rightLocal != null)
        if (left instanceof JcArrayAccess) {
            JcArrayAccess ref = (JcArrayAccess) left;
            JcLocal base = (JcLocal) ref.getArray();
            JcValue index = ref.getIndex();
            _writer.writeStoreArrayIndex(stmt, ii, base, rightLocal, index);
        }
        // NoNullSupport: use the line below to remove Null Constants from the facts.
        // else if(left instanceof InstanceFieldRef && rightLocal != null)
        else if(left instanceof JcFieldRef) {
            JcFieldRef ref = (JcFieldRef) left;
            _writer.writeStoreInstanceField(ii, ref.getField().getField(), (JcLocal) ref.getInstance(), rightLocal, _cp);
        }/*
        // NoNullSupport: use the line below to remove Null Constants from the facts.
        // else if(left instanceof StaticFieldRef && rightLocal != null)
        else if(left instanceof StaticFieldRef) {
            StaticFieldRef ref = (StaticFieldRef) left;
            _writer.writeStoreStaticField(ii, ref.getField(), rightLocal);
        }
         */
        // NoNullSupport: use the else part below to remove Null Constants from the facts.
        /*else if(right instanceof NullConstant)
        {
            _writer.writeUnsupported(inMethod, stmt, session);
            // skip, not relevant for pointer analysis
        }*/
        else
            System.out.println("Cannot handle assignment: " + stmt + " (right: " + right.getClass() + ")");
    }

    /**
     * Return statement
     */
    private void generate(JcReturnInst stmt, JcType returnType, InstrInfo ii, SessionCounter session)
    {
        JcValue v = stmt.getReturnValue();

        if (v == null)
            _writer.writeReturnVoid(ii);
        else if (v instanceof JcLocal)
            _writer.writeReturn(ii, (JcLocal) v);
        else if (v instanceof JcStringConstant) {
            JcLocal tmp = _writer.writeStringConstantExpression(stmt, ii.methodId, (JcStringConstant) v, session, _cp);
            _writer.writeReturn(ii, tmp);
        }
        else if (v instanceof JcClassConstant) {
            JcLocal tmp = _writer.writeClassConstantExpression(ii.methodId, (JcClassConstant) v, session, _cp);
            _writer.writeReturn(ii, tmp);
        }
        else if (v instanceof JcNumericConstant || v instanceof JcInt || v instanceof JcFloat) {
            JcLocal tmp = _writer.writeNumConstantExpression(ii.methodId, v, returnType, session);
            _writer.writeReturn(ii, tmp);
        }/* else if(v instanceof MethodHandle) {
            Local tmp = _writer.writeMethodHandleConstantExpression(ii.methodId, (MethodHandle) v, session);
            _writer.writeReturn(ii, tmp);
        }*/ else if (v instanceof JcNullConstant) {
            JcLocal tmp = _writer.writeNullExpression(ii.methodId, returnType, session);
            _writer.writeReturn(ii, tmp);
            // NoNullSupport: use the line below to remove Null Constants from the facts.
            // _writer.writeUnsupported(inMethod, stmt, session);
        } else
            System.out.println("Unhandled return statement: " + stmt);
    }
};
