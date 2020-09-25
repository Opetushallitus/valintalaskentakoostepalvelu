package fi.vm.sade.valinta.kooste.util;

import java.util.Iterator;

public class IterableUtil {
  public static <T> T singleton(Iterable<T> i) {
    Iterator<T> iterator = i.iterator();
    if (iterator.hasNext()) {
      T t = iterator.next();
      if (!iterator.hasNext()) {
        return t;
      }
    }
    return null;
  }
}
