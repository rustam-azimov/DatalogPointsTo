package test1.resources;

public class A {
	public A(String initial) {
		element = initial;
	}

	private Object element;

	public void printSomething() {
		System.out.println("My element is a " + this.element.toString());
	}

	public void
	setElement(Object new_element) {
		this.element = new_element;
	}
}
