package fi.vm.sade.valinta.kooste.kela.komponentti.impl;

import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVAYHVA;
import fi.vm.sade.valinta.kooste.kela.dto.KelaHakijaRivi;

@Component
public class KelaHakijaRiviKomponenttiImpl {

	private static final Integer KESAKUU = 6;

	public TKUVAYHVA luo(KelaHakijaRivi hakija)
	// @Body HakijaDTO hakija,
	// @Property("lukuvuosi") Date lukuvuosi,
	// @Property("poimintapaivamaara") Date poimintapaivamaara)
	{
		TKUVAYHVA.Builder builder = new TKUVAYHVA.Builder();
		builder.setOppilaitosnumero(hakija.getOppilaitosnumero());
		builder.setOrganisaatio(hakija.getOrganisaatio());
		builder.setHakukohde(hakija.getHakukohde());
		builder.setLukuvuosi(hakija.getLukuvuosi());
		builder.setValintapaivamaara(hakija.getValintapaivamaara());
		builder.setSukunimi(hakija.getSukunimi());
		builder.setEtunimet(hakija.getEtunimi());
		if (hakija.hasHenkilotunnus()) {
			builder.setHenkilotunnus(hakija.getHenkilotunnus());
		} else { // Ulkomaalaisille syntyma-aika hetun
					// sijaan
			String syntymaAika = hakija.getSyntymaaika(); // henkilotiedot.get(SYNTYMAAIKA);
															// // esim
															// 04.05.1965
			// Poistetaan pisteet ja tyhjaa loppuun
			String syntymaAikaIlmanPisteita = syntymaAika.replace(".", "");
			builder.setHenkilotunnus(syntymaAikaIlmanPisteita.toString());
		}
		builder.setPoimintapaivamaara(hakija.getPoimintapaivamaara());
		DateTime dateTime = new DateTime(hakija.getLukuvuosi());
		if (dateTime.getMonthOfYear() > KESAKUU) { // myohemmin
													// kuin
													// kesakuussa!
			builder.setSyksyllaAlkavaKoulutus();
		} else {
			builder.setKevaallaAlkavaKoulutus();
		}
		return builder.build();
	}

}
