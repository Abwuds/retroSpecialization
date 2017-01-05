import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.Base64;

public class LookupProxyTest {
  static final String PROXY_CLASS =
        "yv66vgAAADMAGgoABQASBwAUCgACABUHABYHABcBAAY8aW5pdD4BAAMoKVYB"
      + "AARDb2RlAQAPTGluZU51bWJlclRhYmxlAQAGbG9va3VwAQAGTG9va3VwAQAM"
      + "SW5uZXJDbGFzc2VzAQA6KExqYXZhL2xhbmcvQ2xhc3M7KUxqYXZhL2xhbmcv"
      + "aW52b2tlL01ldGhvZEhhbmRsZXMkTG9va3VwOwEACVNpZ25hdHVyZQEAPShM"
      + "amF2YS9sYW5nL0NsYXNzPCo+OylMamF2YS9sYW5nL2ludm9rZS9NZXRob2RI"
      + "YW5kbGVzJExvb2t1cDsBAApTb3VyY2VGaWxlAQAQTG9va3VwUHJveHkuamF2"
      + "YQwABgAHBwAYAQAlamF2YS9sYW5nL2ludm9rZS9NZXRob2RIYW5kbGVzJExv"
      + "b2t1cAwABgAZAQAcamF2YS9sYW5nL2ludm9rZS9Mb29rdXBQcm94eQEAEGph"
      + "dmEvbGFuZy9PYmplY3QBAB5qYXZhL2xhbmcvaW52b2tlL01ldGhvZEhhbmRs"
      + "ZXMBABQoTGphdmEvbGFuZy9DbGFzczspVgAhAAQABQAAAAAAAgABAAYABwAB"
      + "AAgAAAAdAAEAAQAAAAUqtwABsQAAAAEACQAAAAYAAQAAAAUACQAKAA0AAgAI"
      + "AAAAIQADAAEAAAAJuwACWSq3AAOwAAAAAQAJAAAABgABAAAABwAOAAAAAgAP"
      + "AAIAEAAAAAIAEQAMAAAACgABAAIAEwALABk=";

  public static void main(String[] args) throws Throwable {
    byte[] array = Base64.getDecoder().decode(PROXY_CLASS);
    Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
    theUnsafe.setAccessible(true);
    sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
    Class<?> proxy = unsafe.defineAnonymousClass(java.lang.invoke.MethodHandles.class, array, null);
    MethodHandle mh = MethodHandles.lookup().findStatic(proxy, "lookup",
        MethodType.methodType(Lookup.class, Class.class));
    Lookup trusted = (Lookup) mh.invokeExact(LookupProxyTest.class);
    System.out.println(trusted);
  }
}
