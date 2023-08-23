package datalogpt.src;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TypeUtils {
    private static final String BOOLEAN = "boolean";
    private static final String BOOLEAN_JVM = "Z";
    private static final String INT = "int";
    private static final String INT_JVM = "I";
    private static final String LONG = "long";
    private static final String LONG_JVM = "J";
    private static final String DOUBLE = "double";
    private static final String DOUBLE_JVM = "D";
    private static final String VOID = "void";
    private static final String VOID_JVM = "V";
    private static final String FLOAT = "float";
    private static final String FLOAT_JVM = "F";
    private static final String CHAR = "char";
    private static final String CHAR_JVM = "C";
    private static final String SHORT = "short";
    private static final String SHORT_JVM = "S";
    private static final String BYTE = "byte";
    private static final String BYTE_JVM = "B";
    private static final boolean DEBUG = false;
    private static final Map<String, String> cachedRaisedTypes = new ConcurrentHashMap();
    private static final Pattern slashPat = Pattern.compile("/", 16);
    private static final String dotRepl = Matcher.quoteReplacement(".");

    private TypeUtils() {
    }

    public static String raiseTypeId(String id) {
        String cached = (String)cachedRaisedTypes.get(id);
        if (cached != null) {
            return cached;
        } else {
            int typePrefixEndIdx;
            for(typePrefixEndIdx = 0; id.charAt(typePrefixEndIdx) == '['; ++typePrefixEndIdx) {
            }

            StringBuilder sb;
            if (id.charAt(typePrefixEndIdx) == 'L' && id.charAt(id.length() - 1) == ';') {
                sb = new StringBuilder(replaceSlashesWithDots(id.substring(typePrefixEndIdx + 1, id.length() - 1)));
            } else {
                sb = new StringBuilder(decodePrimType(id.substring(typePrefixEndIdx)));
            }

            if (typePrefixEndIdx != 0) {
                for(int i = 0; i < typePrefixEndIdx; ++i) {
                    sb.append("[]");
                }
            }

            String ret = sb.toString();
            cachedRaisedTypes.put(id, ret);
            return ret;
        }
    }

    public static List<String> raiseSignature(String sig) {
        int lParenIdx = sig.indexOf(40);
        int rParenIdx = sig.indexOf(41);
        if (lParenIdx >= 0 && rParenIdx >= 0) {
            List<String> ret = new LinkedList();
            ret.add(raiseTypeId(sig.substring(rParenIdx + 1)));
            boolean array = false;
            int pos = lParenIdx + 1;

            while(pos < rParenIdx) {
                String ch = sig.substring(pos, pos + 1);

                try {
                    ret.add(decodePrimType(ch) + (array ? "[]" : ""));
                    array = false;
                    ++pos;
                } catch (RuntimeException var8) {
                    if (ch.equals("L")) {
                        int semiPos = sig.indexOf(59, pos);
                        if (semiPos >= 0) {
                            ret.add(raiseTypeId(sig.substring(pos, semiPos + 1)) + (array ? "[]" : ""));
                            array = false;
                            pos = semiPos + 1;
                            continue;
                        }
                    } else if (ch.equals("[")) {
                        array = true;
                        ++pos;
                        continue;
                    }

                    throw new RuntimeException("Could not raise signature: " + sig + ", problem at string position " + pos);
                }
            }

            return ret;
        } else {
            throw new RuntimeException("Malformed JVM signature found: " + sig);
        }
    }

    private static String decodePrimType(String id) {
        switch (id) {
            case "Z":
                return "boolean";
            case "I":
                return "int";
            case "J":
                return "long";
            case "D":
                return "double";
            case "V":
                return "void";
            case "F":
                return "float";
            case "C":
                return "char";
            case "S":
                return "short";
            case "B":
                return "byte";
            default:
                System.out.println("Invalid type id format (decodePrimType): " + id);
                return "ERROR";
        }
    }

    public static boolean isPrimitiveType(String s) {
        return s.equals("boolean") || s.equals("int") || s.equals("long") || s.equals("double") || s.equals("void") || s.equals("float") || s.equals("char") || s.equals("short") || s.equals("byte");
    }

    public static boolean isLowLevelType(char first, String s) {
        return first == '[' || first == 'L' && s.endsWith(";");
    }

    public static String replaceSlashesWithDots(CharSequence s) {
        return slashPat.matcher(s).replaceAll(dotRepl);
    }
}

