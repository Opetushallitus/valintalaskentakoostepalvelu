package fi.vm.sade.valinta.kooste.excel;

import java.util.Collection;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class Monivalinta extends Solu {

	private final Collection<String> vaihtoehdot;
	private final String teksti;
	private final boolean muokattava;
	private final int preferoituleveys;

	public Monivalinta(String teksti, Collection<String> vaihtoehdot,
			boolean muokattava) {
		super(null);
		this.vaihtoehdot = vaihtoehdot;
		this.teksti = teksti;
		this.muokattava = muokattava;
		this.preferoituleveys = 0;
	}

	public Monivalinta(String teksti, Collection<String> vaihtoehdot,
			boolean muokattava, int preferoituleveys) {
		super(null);
		this.vaihtoehdot = vaihtoehdot;
		this.teksti = teksti;
		this.muokattava = muokattava;
		this.preferoituleveys = preferoituleveys;
	}

	@Override
	public int preferoituLeveys() {
		// TODO Auto-generated method stub
		return preferoituleveys;
	}

	public boolean isMuokattava() {
		return muokattava;
	}

	@Override
	protected boolean validoi() {
		if (vaihtoehdot.isEmpty() || teksti == null) {
			return false;
		}
		return vaihtoehdot.contains(teksti);
	}

	public Collection<String> getVaihtoehdot() {
		return vaihtoehdot;
	}

	@Override
	public boolean isMonivalinta() {
		return true;
	}

	public boolean isTyhja() {
		return teksti == null;
	}

	@Override
	public Monivalinta toMonivalinta() {
		return this;
	}

	public Numero toNumero() {
		return new Numero();
	}

	@Override
	public boolean isTasausOikealle() {
		return true;
	}

	public Teksti toTeksti() {
		return new Teksti(teksti, true, false, preferoituleveys);
	}

}
