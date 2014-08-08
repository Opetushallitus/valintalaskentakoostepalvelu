package fi.vm.sade.valinta.kooste;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.collect.Sets;

/**
 * 
 * @author jussija
 *
 * Ei kopioi listaa ellei ole pakko
 */
public class Util {

	public static <T> Collection<T> whitelist(Collection<T> w, Collection<T> t, Consumer<Collection<T>> ylimaaraiset) {
		if(t.containsAll(w)) {
			return w;
		} else {
			Set<T> s = Sets.newHashSet(w);
			s.removeAll(t);
			ylimaaraiset.accept(s);
			Set<T> copy = Sets.newHashSet(w);
			copy.removeAll(s);
			return copy;
		}
	}
	
	public static <T> Collection<T> blacklist(Collection<T> b, Collection<T> t, Consumer<Collection<T>> ylimaaraiset) {
		Set<T> copy = Sets.newHashSet(t);
		copy.removeAll(b);
		if(t.containsAll(b)) {
			return copy;
		} else {
			Set<T> s = Sets.newHashSet(b);
			s.removeAll(t);
			ylimaaraiset.accept(s);
			return copy;
		}
	}
	
	
}
