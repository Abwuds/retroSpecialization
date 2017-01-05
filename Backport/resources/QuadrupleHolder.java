public class QuadrupleHolder<any T, any U, any V, any W> {
    T t;
    U u;
    V v;
    W w;

    public QuadrupleHolder(T t, U u, V v, W w) {
        this.t = t;
        this.u = u;
        this.v = v;
        this.w = w;
    }

    public void print() {
        System.out.println("The 4 values held are :\n\tt : " + t.toString() + "\n\tu : " + u.toString() + "\n\tv : " + v.toString()
        + "\n\tw : " + w.toString());
    }

    public void testPrint(T t) {
        System.out.println("testPrint : Value t: " + t.toString());
    }

    public T test1(T t, U u, T t2) {
        T t3 = t = t2;
        System.out.println("test1 : Value t: " + t.toString());
        return t3;
    }

    public U test2(U u, T t, U u2) {
        U u3 = u = u2;
        return u3;
    }
}