package fi.vm.sade.valinta.kooste.valintakokeet.dto;

import java.util.Arrays;

import fi.vm.sade.valinta.kooste.valvomo.dto.KokonaisTyo;
import fi.vm.sade.valinta.kooste.valvomo.dto.OsaTyo;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.valvomo.dto.Tyo;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ValintakoeProsessi extends DokumenttiProsessi {

	private final OsaTyo valintaperusteet;
	private final OsaTyo hakemukset;
	private final OsaTyo valintakoelaskenta;
	private final KokonaisTyo kokonaistyo;

	public ValintakoeProsessi(String hakuOid) {
		super("Valintakoelaskenta",
				"Muistinvarainen valintakoelaskenta haulle", hakuOid, Arrays
						.asList("valintakoelaskenta"));
		this.valintaperusteet = new OsaTyo(Poikkeus.VALINTAPERUSTEET);
		this.valintakoelaskenta = new OsaTyo(Poikkeus.VALINTAKOELASKENTA);
		this.hakemukset = new OsaTyo(Poikkeus.HAKU);
		this.kokonaistyo = new KokonaisTyo(Arrays.asList(valintaperusteet,
				valintakoelaskenta, hakemukset));
	}

	public OsaTyo getValintakoelaskenta() {
		return valintakoelaskenta;
	}

	public OsaTyo getValintaperusteet() {
		return valintaperusteet;
	}

	public OsaTyo getHakemukset() {
		return hakemukset;
	}

	@Override
	public Tyo getKokonaistyo() {
		return kokonaistyo;
	}
}
