package test1.resources;

public class Main {
	public static void main(String[] args) {
		A a = new A(new String("Main-A-dummy"));
		A b = new B();
		A c = new C();

		a.printSomething();
		b.printSomething();
		c.printSomething();
	}
}
