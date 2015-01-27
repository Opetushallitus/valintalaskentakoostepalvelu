package fi.vm.sade.valinta.kooste.excel;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class Rivi {

	private final Collection<Solu> solut;

	public Rivi() {
		this.solut = Collections.emptyList();
	}

	public Collection<Rivi> getToisteisetRivit() {
		return Arrays.asList(this);
	}

	public Rivi(Solu solu) {
		this.solut = Arrays.asList(solu);
	}

	public Rivi(Collection<Solu> solut) {
		this.solut = solut;
	}

	public boolean isNakyvissa() {
		return true;
	}

	public boolean isTyhja() {

		if(solut.isEmpty()) {
			return true;
		} else {
			return solut.stream().allMatch(s -> StringUtils.isBlank(s.toTeksti().getTeksti()));
		}
	}

	public boolean validoi(Rivi rivi) {
		return false;
	}

	private final static Rivi TYHJA = new Rivi();

	public static Rivi tyhjaRivi() {
		return TYHJA;
	}

	public Collection<Solu> getSolut() {
		return solut;
	}

	public static Rivi tekstiRivi(Collection<String> tekstit) {
		Collection<Solu> solut = Lists.newArrayList();
		for (String teksti : tekstit) {
			solut.add(new Teksti(teksti));
		}
		return new Rivi(solut);
	}

	protected static Collection<Solu> valuta(Iterator<Solu> soluIterator,
			int maara) {
		if (soluIterator.hasNext()) {
			Collection<Solu> solut = Lists.newArrayList();
			for (int i = 0; i < maara; ++i) {
				solut.add(soluIterator.next());
				if (!soluIterator.hasNext()) {
					break;
				}
			}
			return solut;
		}
		return Collections.emptyList();
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		for (Solu s : solut) {
			b.append(s).append(", ");
		}
		return b.toString();
	}
}
