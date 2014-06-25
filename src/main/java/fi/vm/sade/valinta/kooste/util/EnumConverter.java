package fi.vm.sade.valinta.kooste.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Geneerinen enum convertteri
 */
public class EnumConverter {
	public static <T extends Enum<T>> T convert(Class<T> e1, Enum<?> e2) {
		if (e2 == null) {
			return null;
		}
		return Enum.<T> valueOf(e1, e2.toString());
	}

	public static <T extends Enum<T>> List<T> convert(Class<T> e1,
			List<Enum<?>> e2) {
		List<T> list = new ArrayList<T>();
		for (Enum e : e2) {
			list.add(convert(e1, e));
		}
		return list;
	}

	public static <T extends Enum<T>> List<T> convertStringListToEnum(
			Class<T> e1, List<String> e2) {
		List<T> list = new ArrayList<T>();
		for (String s : e2) {
			list.add(T.valueOf(e1, s));
		}
		return list;
	}
}