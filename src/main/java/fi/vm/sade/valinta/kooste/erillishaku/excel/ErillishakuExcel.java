package fi.vm.sade.valinta.kooste.erillishaku.excel;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Lists;

import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.excel.RiviBuilder;
import fi.vm.sade.valinta.kooste.excel.Teksti;
import fi.vm.sade.valinta.kooste.excel.arvo.Arvo;
import fi.vm.sade.valinta.kooste.excel.arvo.TekstiArvo;
import fi.vm.sade.valinta.kooste.util.KonversioBuilder;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ErillishakuExcel {

	
	private final Excel excel;
	
	public ErillishakuExcel(Hakutyyppi tyyppi, ErillishakuRiviKuuntelija kuuntelija) {
		this(tyyppi, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, Collections.emptyList(),kuuntelija);
	}
	
	public ErillishakuExcel(Hakutyyppi tyyppi, String hakuNimi, String hakukohdeNimi,
			String tarjoajaNimi, Collection<ErillishakuRivi> erillishakurivit) {
		this(tyyppi, hakuNimi,hakukohdeNimi,tarjoajaNimi,erillishakurivit,new ErillishakuRiviKuuntelija() {
			
			@Override
			public void erillishakuRiviTapahtuma(ErillishakuRivi rivi) {
				
			}
		});
	}
	public ErillishakuExcel(final Hakutyyppi tyyppi, String hakuNimi, String hakukohdeNimi,
			String tarjoajaNimi, Collection<ErillishakuRivi> erillishakurivit,
			ErillishakuRiviKuuntelija kuuntelija) {
		
		List<Rivi> rivit = Lists.newArrayList();
		Collection<Collection<Arvo>> esittelyt = Lists.newArrayList();
		esittelyt.add(Arrays.asList(new TekstiArvo(hakuNimi, true, false, 4)));
		esittelyt.add(Arrays.asList(new TekstiArvo(hakukohdeNimi, true, false,
				4)));
		esittelyt.add(Arrays
				.asList(new TekstiArvo(tarjoajaNimi, true, false, 4)));
		esittelyt.add(Arrays.asList(new TekstiArvo(StringUtils.EMPTY)));
		esittelyt.add(Arrays.asList(new TekstiArvo("Sukunimi"), new TekstiArvo(
				"Etunimi"), new TekstiArvo("Henkilötunnus"), new TekstiArvo(
				"Syntymäaika"), new TekstiArvo("Hakemuksentila"),
				new TekstiArvo("Vastaanottotila"), new TekstiArvo(
						"Ilmoittautumistila")));
		ErillishakuDataRivi dataRivit = new ErillishakuDataRivi(
				kuuntelija,
				Stream.concat(
						esittelyt.stream(),
						erillishakurivit
								.stream()
								.map(rivi -> {
									Collection<Arvo> a = Lists.newArrayList();
									a.add(new TekstiArvo(rivi.getSukunimi(),
											true, true));
									a.add(new TekstiArvo(rivi.getEtunimi(),
											true, true));
									a.add(new TekstiArvo(rivi
											.getHenkilotunnus(), true, true));
									a.add(new TekstiArvo(rivi.getSyntymaAika(),
											true, true));
									
									a.add(ErillishakuDataRivi.hakemuksenTila(rivi
											.getHakemuksenTila()));
									a.add(ErillishakuDataRivi.vastaanottoTila(tyyppi,rivi
											.getVastaanottoTila()));
									a.add(ErillishakuDataRivi.ilmoittautumisTila(rivi
											.getIlmoittautumisTila()));
									return a;
								})).collect(Collectors.toList()));

		rivit.add(dataRivit);
		this.excel = new Excel("Erillishaku", rivit);
	}

	public Excel getExcel() {
		return excel;
	}
}
