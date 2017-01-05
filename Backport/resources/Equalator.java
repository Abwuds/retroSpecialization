import java.util.Map;
import java.util.AbstractMap;
/**
 * Created by Baxtalou on 19/05/2016.
 */
public class Equalator<any E> {
    Holder<E> holder1;
    Holder<E> holder2;

    public Equalator(Holder<E> holder1, Holder<E> holder2) {
        this.holder1 = holder1;
        this.holder2 = holder2;
    }

    public boolean isEquals() {
        return holder1.element.equals(holder2.element);
    }
}
