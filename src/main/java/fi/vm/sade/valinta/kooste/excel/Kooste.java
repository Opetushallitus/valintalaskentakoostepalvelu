package fi.vm.sade.valinta.kooste.excel;

import java.util.Collection;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Koostaa useasta rivista yhden rivin
 */
public class Kooste extends Rivi {

	private static final Logger LOG = LoggerFactory.getLogger(Kooste.class);
	private final Collection<Rivi> rivit;
	private final boolean nakyvissa;

	public Kooste(Rivi... rivit) {
		this(Lists.newArrayList(rivit));
	}

	public Kooste(Collection<Rivi> rivit) {
		super(pura(rivit));
		this.rivit = rivit;
		this.nakyvissa = true;
	}

	public Kooste(Collection<Rivi> rivit, boolean nakyvissa) {
		super(pura(rivit));
		this.rivit = rivit;
		this.nakyvissa = nakyvissa;
	}

	@Override
	public boolean isNakyvissa() {
		return nakyvissa;
	}

	@Override
	public boolean validoi(Rivi rivi) {
		Iterator<Solu> soluIterator = rivi.getSolut().iterator();
		// LOG.error("KOOSTE VALIDOI");
		for (Rivi r : rivit) {
			int size = 0;
			for (Solu s : r.getSolut()) {
				size += s.ulottuvuus();
			}
			r.validoi(new Rivi(valuta(soluIterator, size)));
		}
		return false;
	}

	private static Collection<Solu> pura(Collection<Rivi> rivit) {
		Collection<Solu> solut = Lists.newArrayList();
		for (Rivi rivi : rivit) {
			solut.addAll(rivi.getSolut());
		}
		return solut;
	}
}
