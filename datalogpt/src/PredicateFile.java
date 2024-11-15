package datalogpt.src;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public enum PredicateFile
{
    ACTUAL_PARAMETER("ActualParam"),
    //ANNOTATION_ELEMENT("AnnotationElement"),
    APP_CLASS("ApplicationClass"),
    //ARRAY_INITIAL_VALUE_FROM_LOCAL("ArrayInitialValueFromLocal"),
    //ARRAY_INITIAL_VALUE_FROM_CONST("ArrayInitialValueFromConst"),
    ARRAY_INSN_INDEX("ArrayInsnIndex"),
    ARRAY_NUM_INDEX("ArrayNumIndex"),
    ARRAY_TYPE("ArrayType"),
    ARRAY_ALLOC("ArrayAllocation"),
    ARRAY_ALLOC_CONST_SIZE("ArrayAllocationConstSize"),
    ASSIGN_BINOP("AssignBinop"),
    ASSIGN_CAST("AssignCast"),
    ASSIGN_CAST_NUM_CONST("AssignCastNumConstant"),
    ASSIGN_CAST_NULL("AssignCastNull"),
    ASSIGN_HEAP_ALLOC("AssignHeapAllocation"),
    ASSIGN_INSTANCE_OF("AssignInstanceOf"),
    ASSIGN_LOCAL("AssignLocal"),
    ASSIGN_NUM_CONST("AssignNumConstant"),
    ASSIGN_NULL("AssignNull"),
    ASSIGN_OPER_FROM("AssignOperFrom"),
    ASSIGN_OPER_FROM_CONSTANT("AssignOperFromConstant"),
    ASSIGN_RETURN_VALUE("AssignReturnValue"),
    ASSIGN_UNOP("AssignUnop"),
    CLASS_HEAP("ClassHeap"),
    CLASS_TYPE("ClassType"),
    CLASS_MODIFIER("ClassModifier"),
    COMPONENT_TYPE("ComponentType"),
    DIRECT_SUPER_IFACE("DirectSuperinterface"),
    DIRECT_SUPER_CLASS("DirectSuperclass"),
    DUMMY_IF_VAR("DummyIfVar"),
    DYNAMIC_METHOD_INV("DynamicMethodInvocation"),
    DYNAMIC_METHOD_INV_PARAM_TYPE("DynamicMethodInvocation-ParamType"),
    EMPTY_ARRAY("EmptyArray"),
    EXCEPT_HANDLER_PREV("ExceptionHandler-Previous"),
    EXCEPTION_HANDLER("ExceptionHandler"),
    EXCEPTION_HANDLER_FORMAL_PARAM("ExceptionHandler-FormalParam"),
    FIELD_ANNOTATION("Field-Annotation"),
    //FIELD_INITIAL_VALUE("FieldInitialValue"),
    FIELD_MODIFIER("Field-Modifier"),
    FIELD_SIGNATURE("Field"),
    FORMAL_PARAM("FormalParam"),
    //GENERIC_TYPE_PARAMETERS("GenericTypeParameters"),
    //GENERIC_TYPE_ERASED_TYPE("GenericType-ErasedType"),
    //GENERIC_FIELD("GenericField"),
    GOTO("Goto"),
    IF("If"),
    IF_CONSTANT("IfConstant"),
    IF_VAR("IfVar"),
    INTERFACE_TYPE("InterfaceType"),
    LAYOUT_CONTROL("LayoutControl"),
    LOAD_ARRAY_INDEX("LoadArrayIndex"),
    LOAD_INST_FIELD("LoadInstanceField"),
    LOAD_STATIC_FIELD("LoadStaticField"),
    //LOOKUP_SWITCH("LookupSwitch"),
    //LOOKUP_SWITCH_DEFAULT("LookupSwitch-Default"),
    //LOOKUP_SWITCH_TARGET("LookupSwitch-Target"),
    MAIN_CLASS("MainClass"),
    METHOD("Method"),
    METHOD_ANNOTATION("Method-Annotation"),
    METHOD_DECL_EXCEPTION("Method-DeclaresException"),
    METHOD_HANDLE_CONSTANT("MethodHandleConstant"),
    METHOD_INV_LINE("MethodInvocation-Line"),
    METHOD_TYPE_CONSTANT("MethodTypeConstant"),
    METHOD_TYPE_CONSTANT_PARAM("MethodTypeConstantParam"),
    METHOD_MODIFIER("Method-Modifier"),
    NATIVE_METHOD_ID("NativeMethodId"),
    NATIVE_RETURN_VAR("NativeReturnVar"),
    NORMAL_HEAP("NormalHeap"),
    NUM_CONSTANT_RAW("NumConstantRaw"),
    OPERATOR_AT("OperatorAt"),
    PARAM_ANNOTATION("Param-Annotation"),
    POLYMORPHIC_INVOCATION("PolymorphicInvocation"),
    PROPERTIES("Properties"),
    RETURN("Return"),
    RETURN_VOID("ReturnVoid"),
    SENSITIVE_LAYOUT_CONTROL("SensitiveLayoutControl"),
    SERVICE("Service"),
    SPECIAL_METHOD_INV("SpecialMethodInvocation"),
    STATEMENT_TYPE("StatementType"),
    STATIC_METHOD_INV("StaticMethodInvocation"),
    STORE_ARRAY_INDEX("StoreArrayIndex"),
    STORE_INST_FIELD("StoreInstanceField"),
    STORE_STATIC_FIELD("StoreStaticField"),
    STRING_CONST("StringConstant"),
    STRING_RAW("StringRaw"),
    THIS_VAR("ThisVar"),
    TYPE_ANNOTATION("Type-Annotation"),
    VAR_TYPE("Var-Type"),
    VAR_DECLARING_METHOD("Var-DeclaringMethod"),
    VAR_SIMPLENAME("Var-SimpleName"),
    VIRTUAL_METHOD_INV("VirtualMethodInvocation");


    private final String name;

    PredicateFile(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public Writer getWriter(File directory, String suffix) throws IOException {
        File factsFile = new File(directory, name + suffix);
        FileUtils.touch(factsFile);
        return new FileWriter(factsFile, true);
    }
}
