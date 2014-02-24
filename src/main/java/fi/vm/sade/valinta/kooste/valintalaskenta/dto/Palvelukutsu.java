package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Sortable by kesto
 */
public class Palvelukutsu implements Comparable<Palvelukutsu> {

	private final long kesto;
	private final String url;

	public Palvelukutsu(long kesto, String url) {
		this.kesto = kesto;
		this.url = url;
	}

	public long getKesto() {
		return kesto;
	}

	public String getUrl() {
		return url;
	}

	public int compareTo(Palvelukutsu o) {
		return new Long(kesto).compareTo(o.kesto);
	}
}
