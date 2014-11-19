package fi.vm.sade.valinta.kooste.erillishaku.excel;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.tulos.dto.ValintatuloksenTila;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.excel.DataRivi;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.excel.SoluLukija;
import fi.vm.sade.valinta.kooste.excel.arvo.Arvo;
import fi.vm.sade.valinta.kooste.excel.arvo.MonivalintaArvo;
/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ErillishakuDataRivi extends DataRivi {
	
	private static final DateTimeFormatter SYNTYMAAIKA = DateTimeFormat.forPattern("dd.MM.yyyy");
	private final ErillishakuRiviKuuntelija kuuntelija;
	public ErillishakuDataRivi(ErillishakuRiviKuuntelija kuuntelija, Collection<Collection<Arvo>> s) {
		super(s);
		this.kuuntelija = kuuntelija;
	}

	@Override
	public boolean validoi(Rivi rivi) {
		SoluLukija lukija = new SoluLukija(false, rivi.getSolut());
		String sukunimi = lukija.getArvoAt(0);
		String etunimi = lukija.getArvoAt(1);
		String henkilotunnus = lukija.getArvoAt(2);
		String syntymaAika = lukija.getArvoAt(3);
		try {
			DateTime p = SYNTYMAAIKA.parseDateTime(syntymaAika);
		} catch(Exception e){
			return true;
		}
		
		String hakemuksenTila = lukija.getArvoAt(4);
		String vastaanottoTila = lukija.getArvoAt(5);
		String ilmoittautumisTila = lukija.getArvoAt(6);
		kuuntelija.erillishakuRiviTapahtuma(new ErillishakuRivi(sukunimi,etunimi, henkilotunnus, syntymaAika, hakemuksenTila, vastaanottoTila, ilmoittautumisTila));
		return true;
	}
	private static final Collection<String> HAKEMUKSENTILA_ARVOT =Arrays.asList(HakemuksenTila.values()).stream().map(t -> t.toString()).collect(Collectors.toList()); 
	private static final Collection<String> VASTAANOTTOTILA_ARVOT =Arrays.asList(ValintatuloksenTila.values()).stream().map(t -> t.toString()).collect(Collectors.toList());
	private static final Collection<String> VASTAANOTTOTILA_ARVOT_KK =
			Arrays.asList(
					// KORKEAKOULUJEN VALINTATULOKSEN TILAT
					ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA,
					ValintatuloksenTila.PERUNUT,
					ValintatuloksenTila.PERUUTETTU,
					ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI,
					ValintatuloksenTila.KESKEN
					//
					).stream().map(t -> t.toString()).collect(Collectors.toList());
	private static final Collection<String> VASTAANOTTOTILA_ARVOT_TOINEN_ASTE =
			Arrays.asList(
					// TOISEN ASTEEN VALINTATULOKSEN TILAT
					ValintatuloksenTila.VASTAANOTTANUT,
					ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA,
					ValintatuloksenTila.PERUNUT,
					ValintatuloksenTila.KESKEN
					//
					).stream().map(t -> t.toString()).collect(Collectors.toList());
	private static final Collection<String> ILMOITTAUTUMISTILA_ARVOT =Arrays.asList(IlmoittautumisTila.values()).stream().map(t -> t.toString()).collect(Collectors.toList()); 
	public static MonivalintaArvo hakemuksenTila(String arvo) {
		
		return new MonivalintaArvo(arvo, HAKEMUKSENTILA_ARVOT);
	}
	public static MonivalintaArvo vastaanottoTila(Hakutyyppi hakutyyppi, String arvo) {
		if(Hakutyyppi.TOISEN_ASTEEN_OPPILAITOS.equals(hakutyyppi)) {
			return new MonivalintaArvo(arvo, VASTAANOTTOTILA_ARVOT_TOINEN_ASTE);
		} else if(Hakutyyppi.KORKEAKOULU.equals(hakutyyppi)) {
			return new MonivalintaArvo(arvo, VASTAANOTTOTILA_ARVOT_KK);
		} else{
		return new MonivalintaArvo(arvo, VASTAANOTTOTILA_ARVOT);
		}
	}
	public static MonivalintaArvo ilmoittautumisTila(String arvo) {
		
		return new MonivalintaArvo(arvo, ILMOITTAUTUMISTILA_ARVOT);
	}
}
