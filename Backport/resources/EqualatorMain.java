import java.util.Map;
import java.util.AbstractMap;
/**
 * Created by Baxtalou on 19/05/2016.
 */
public class EqualatorMain {

    public static void main(String[] args) {
        Holder<double> h = new Holder<double>(45.2);
        Holder<double> h2 = new Holder<double>(45.2);
        Equalator<double> eq = new Equalator<double>(h, h2);
        System.out.println(Boolean.toString(eq.isEquals()));
        h.f();

        QuadrupleHolder<float, double, byte, char> quadrupleHolder = new QuadrupleHolder<float, double, byte, char>(6.6f, 2.3, (byte)7, 'b');
        quadrupleHolder.testPrint(4.5f);
        System.out.println(quadrupleHolder.test1(3.3f, 55.5, 7.7f));
        System.out.println(quadrupleHolder.test2(55.5, 3.3f, 2.2));
        quadrupleHolder.print();
    }
}
