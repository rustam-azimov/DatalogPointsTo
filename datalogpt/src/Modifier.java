package datalogpt.src;
import org.objectweb.asm.Opcodes;

public class Modifier {
    public static final int ABSTRACT = Opcodes.ACC_ABSTRACT;
    public static final int FINAL = Opcodes.ACC_FINAL;
    public static final int INTERFACE = Opcodes.ACC_INTERFACE;
    public static final int NATIVE = Opcodes.ACC_NATIVE;
    public static final int PRIVATE = Opcodes.ACC_PRIVATE;
    public static final int PROTECTED = Opcodes.ACC_PROTECTED;
    public static final int PUBLIC = Opcodes.ACC_PUBLIC;
    public static final int STATIC = Opcodes.ACC_STATIC;
    public static final int SYNCHRONIZED = Opcodes.ACC_SYNCHRONIZED;
    public static final int TRANSIENT = Opcodes.ACC_TRANSIENT;
    public static final int VOLATILE = Opcodes.ACC_VOLATILE;
    public static final int STRICTFP = Opcodes.ACC_STRICT;
    public static final int ANNOTATION = Opcodes.ACC_ANNOTATION;
    public static final int ENUM = Opcodes.ACC_ENUM;
    public static final int SYNTHETIC = Opcodes.ACC_SYNTHETIC;
    public static final int CONSTRUCTOR = 65536;
    public static final int DECLARED_SYNCHRONIZED = 131072;
    public static final int REQUIRES_TRANSITIVE = 32;
    public static final int REQUIRES_STATIC = 64;
    public static final int REQUIRES_SYNTHETIC = 4096;
    public static final int REQUIRES_MANDATED = 32768;

    private Modifier() {
    }

    public static boolean isAbstract(int m) {
        return (m & 1024) != 0;
    }

    public static boolean isFinal(int m) {
        return (m & 16) != 0;
    }

    public static boolean isInterface(int m) {
        return (m & 512) != 0;
    }

    public static boolean isNative(int m) {
        return (m & 256) != 0;
    }

    public static boolean isPrivate(int m) {
        return (m & 2) != 0;
    }

    public static boolean isProtected(int m) {
        return (m & 4) != 0;
    }

    public static boolean isPublic(int m) {
        return (m & 1) != 0;
    }

    public static boolean isStatic(int m) {
        return (m & 8) != 0;
    }

    public static boolean isSynchronized(int m) {
        return (m & 32) != 0;
    }

    public static boolean isTransient(int m) {
        return (m & 128) != 0;
    }

    public static boolean isVolatile(int m) {
        return (m & 64) != 0;
    }

    public static boolean isStrictFP(int m) {
        return (m & 2048) != 0;
    }

    public static boolean isAnnotation(int m) {
        return (m & 8192) != 0;
    }

    public static boolean isEnum(int m) {
        return (m & 16384) != 0;
    }

    public static boolean isSynthetic(int m) {
        return (m & 4096) != 0;
    }

    public static boolean isConstructor(int m) {
        return (m & 65536) != 0;
    }

    public static boolean isDeclaredSynchronized(int m) {
        return (m & 131072) != 0;
    }

    public static String toString(int m) {
        StringBuffer buffer = new StringBuffer();
        if (isPublic(m)) {
            buffer.append("public ");
        } else if (isPrivate(m)) {
            buffer.append("private ");
        } else if (isProtected(m)) {
            buffer.append("protected ");
        }

        if (isAbstract(m)) {
            buffer.append("abstract ");
        }

        if (isStatic(m)) {
            buffer.append("static ");
        }

        if (isFinal(m)) {
            buffer.append("final ");
        }

        if (isSynchronized(m)) {
            buffer.append("synchronized ");
        }

        if (isNative(m)) {
            buffer.append("native ");
        }

        if (isTransient(m)) {
            buffer.append("transient ");
        }

        if (isVolatile(m)) {
            buffer.append("volatile ");
        }

        if (isStrictFP(m)) {
            buffer.append("strictfp ");
        }

        if (isAnnotation(m)) {
            buffer.append("annotation ");
        }

        if (isEnum(m)) {
            buffer.append("enum ");
        }

        if (isInterface(m)) {
            buffer.append("interface ");
        }

        return buffer.toString().trim();
    }
}
