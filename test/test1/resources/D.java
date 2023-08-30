package test1.resources;

public class D extends A {
	public D() {
		super(new String("D-dummy"));
	}

	@Override
	public void printSomething() {
		System.out.println("This should never end up getting called...");
	}
}
