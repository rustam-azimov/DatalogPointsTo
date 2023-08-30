package test1.resources;

public class C extends A {
	public C() {
		super(new String("C-dummy"));
	}

	@Override
	public void printSomething() {
		setElement(this);
		printSomething();
	}

	@Override
	public String
	toString() {
		return new String("C");
	}
}
