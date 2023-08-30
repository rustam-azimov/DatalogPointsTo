package test1.resources;

public class B extends A {
	public B() {
		super(new String("B-dummy"));
	}

	@Override
	public void printSomething() {
		super.printSomething();
		setElement(new String("B-dummy2"));
		super.printSomething();
	}
}
