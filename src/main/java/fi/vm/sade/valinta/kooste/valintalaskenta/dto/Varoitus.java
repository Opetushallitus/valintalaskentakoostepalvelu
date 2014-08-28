package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

/**
 * 
 * @author Jussi Jartamo
 *
 * @Deprecated Dokumenttien tila siirretaan seurantapalveluun etta
 *             koostepalvelusta saadaan tilaton, eli koosteen tila DTO on
 *             poistumassa kaytosta
 * 
 */
@Deprecated
public class Varoitus {

	private final String oid;
	private final String selite;

	public Varoitus(String oid, String selite) {
		this.oid = oid;
		this.selite = selite;
	}

	public String getOid() {
		return oid;
	}

	public String getSelite() {
		return selite;
	}
}
