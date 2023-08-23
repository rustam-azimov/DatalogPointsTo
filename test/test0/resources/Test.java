public class Test {
    public static void main(String[] args) {
        foo();
        bar();
    }
    static void foo() {
        Object a = new A1();
        Object b = id(a);
    }
    static void bar() {
        Object a = new A2();
        Object b = id(a);
    }
    static Object id(Object a) {
        return a;
    }
}

class A1 {
    int f = 0;
}

class A2 {
    int g = 0;
}

