public class Holder<any E> {
    E element;

    public Holder(E e) {
        element = e;
    }

    public void f() {
        f2(element);
    }

    public void f2(E e) {
        System.out.println("Hello from f2: " + e.toString());
    }
}