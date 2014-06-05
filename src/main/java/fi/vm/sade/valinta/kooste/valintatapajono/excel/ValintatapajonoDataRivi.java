package fi.vm.sade.valinta.kooste.valintatapajono.excel;

import java.util.Collection;

import fi.vm.sade.valinta.kooste.excel.DataRivi;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.excel.SoluLukija;
import fi.vm.sade.valinta.kooste.excel.arvo.Arvo;

public class ValintatapajonoDataRivi extends DataRivi {

	public ValintatapajonoDataRivi(Collection<Collection<Arvo>> arvot) {
		super(arvot);
	}

	public boolean validoi(Rivi rivi) {
		SoluLukija lukija = new SoluLukija(false, rivi.getSolut());
		String oid = lukija.getArvoAt(0);
		// String nimi = lukija.getArvoAt(1);
		// Collection<PistesyottoArvo> arvot = Lists.newArrayList();
		// {
		// int i = 2;
		// for (PistesyottoDataArvo dataArvo : dataArvot) {
		// arvot.add(dataArvo.asPistesyottoArvo(lukija.getArvoAt(i),
		// lukija.getArvoAt(i + 1)));
		// // arvot.add(new PistesyottoArvo());
		// i += 2;
		// }
		// }
		// PistesyottoRivi pistesyottorivi = new PistesyottoRivi(oid, nimi,
		// arvot);
		// for (PistesyottoDataRiviKuuntelija kuuntelija : kuuntelijat) {
		// kuuntelija.pistesyottoDataRiviTapahtuma(pistesyottorivi);
		// }
		return true;
	}

}
