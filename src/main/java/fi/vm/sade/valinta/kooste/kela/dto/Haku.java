package fi.vm.sade.valinta.kooste.kela.dto;

public class Haku {
	private final String hakuTyyppiUri;
	private final String koulutuksenAlkamiskausiUri;
	private final int koulutuksenAlkamisVuosi;

	public Haku(String hakuTyyppiUri, String koulutuksenAlkamiskausiUri,
			int koulutuksenAlkamisVuosi) {
		if (hakuTyyppiUri == null || koulutuksenAlkamiskausiUri == null) {
			throw new RuntimeException(
					"Tarjonta palautti puutteelliset haun tiedot! "
							+ hakuTyyppiUri + " " + koulutuksenAlkamiskausiUri);
		}
		this.koulutuksenAlkamisVuosi = koulutuksenAlkamisVuosi;
		this.hakuTyyppiUri = hakuTyyppiUri;
		this.koulutuksenAlkamiskausiUri = koulutuksenAlkamiskausiUri;
	}

	public String getKoulutuksenAlkamiskausiUri() {
		return koulutuksenAlkamiskausiUri;
	}

	public String getHakuTyyppiUri() {
		return hakuTyyppiUri;
	}

}
