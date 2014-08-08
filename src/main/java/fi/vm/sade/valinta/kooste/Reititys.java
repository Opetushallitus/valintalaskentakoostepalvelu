package fi.vm.sade.valinta.kooste;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.camel.Expression;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Reititys {

	private final static Logger LOG = LoggerFactory
			.getLogger(Reititys.class);
	
	public static Predicate vaadi(final Class<?> c) {
		return new KoostepalveluEhto<Object>(t -> {
			boolean onko = t.getClass().isInstance(c);
			if(!onko) {
				LOG.error("Reittia ei voida kaynnistaa ilman tyyppia {}. Reitti kaynnistettiin tyypilla {}", c, t.getClass());
			}
			return onko;
		});
	}
	
	public static <T> Predicate ehto(java.util.function.Predicate<T> ehto) {
		return new KoostepalveluEhto<T>(ehto);
	}
	
	public static <I> Processor kuluttaja(Consumer<I> c) {
		return new KoostepalveluKuluttaja<I>(c);
	}
	public static <I> Processor kuluttaja(Consumer<I> c, BiPredicate<I,Exception> virhekasittelija) {
		return new KoostepalveluKuluttaja<I>(c, virhekasittelija);
	}
	public static <I> Processor kuluttaja(Consumer<I> c, BiPredicate<I,Exception> virhekasittelija, int retries, int delay) {
		return new KoostepalveluKuluttaja<I>(c, virhekasittelija, retries, delay);
	}
	public static <I,O> Expression lauseke(Function<I,O> f) {
		return new KoostepalveluLauseke<I, O>(f);
	}
	
	public static <I,O> Expression lauseke(Function<I,O> f, BiPredicate<I,Exception> virhekasittelija, O vakio) {
		return new KoostepalveluLauseke<I, O>(f, virhekasittelija, vakio);
	}
	public static <I,O> Expression lauseke(Function<I,O> f, BiConsumer<I,Exception> virhekasittelija) {
		return new KoostepalveluLauseke<I, O>(f, virhekasittelija);
	}
	
	public static <I,O> Processor funktio(Function<I,O> f) {
		return new KoostepalveluFunktio<I,O>(f);
	}
	public static <I,O> Processor funktio(Function<I,O> f, BiPredicate<I,Exception> virhekasittelija, Function<I, O> oletusluoja) {
		return new KoostepalveluFunktio<I,O>(f, virhekasittelija, oletusluoja);
	}
}
