package fi.vm.sade.valinta.kooste.kela.dto;

import static fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource.HENKILOTUNNUS;
import static fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource.SYNTYMAAIKA;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
import fi.vm.sade.valinta.kooste.kela.komponentti.HakemusSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.HakukohdeSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.LinjakoodiSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.OppilaitosSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.PaivamaaraSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.TilaSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.TutkinnontasoSource;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.LogEntry;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class KelaHaku extends KelaAbstraktiHaku {
	private final static Logger LOG = LoggerFactory.getLogger(KelaHaku.class);
	private final Collection<ValintaTulosServiceDto> hakijat;

	public KelaHaku(Collection<ValintaTulosServiceDto> hakijat,
			HakuV1RDTO haku, PaivamaaraSource paivamaaraSource) {
		super(haku, paivamaaraSource);
		this.hakijat = hakijat;
	}

	@Override
	public Collection<KelaHakijaRivi> createHakijaRivit(
			Date alkuPvm,
			Date loppuPvm,
			String hakuOid,
			KelaProsessi prosessi,
			HakemusSource hakemusSource, HakukohdeSource hakukohdeSource,
			LinjakoodiSource linjakoodiSource, OppilaitosSource oppilaitosSource, TutkinnontasoSource tutkinnontasoSource, TilaSource tilaSource) {
		
		Collection<KelaHakijaRivi> valitut = Lists.newArrayList();
		prosessi.setKokonaistyo(hakijat.size()-1);
		for (ValintaTulosServiceDto hakija : hakijat) {
			prosessi.inkrementoiTehtyjaToita();
			hakija.getHakutoiveet()
					.stream()
					//
					.filter(h -> h != null && h.getValintatila() != null
							&& h.getVastaanottotila() != null)
					//
					.filter(hakutoive ->
					//
					hakutoive.getValintatila().isHyvaksytty()
							&& hakutoive.getVastaanottotila()
									.isVastaanottanut())
					//
					.findFirst()
					.ifPresent(
							hakutoive -> {
								
								Hakemus hakemus = hakemusSource
										.getHakemusByOid(hakija.getHakemusOid());
								Map<String, String> henkilotiedot = henkilotiedot(hakemus);
								String hakukohdeOid = hakutoive
										.getHakukohdeOid();
								final String etunimi = henkilotiedot
										.get(ETUNIMET);
								final String sukunimi = henkilotiedot
										.get(SUKUNIMI);
								final String henkilotunnus = henkilotiedot
										.get(HENKILOTUNNUS);
								final String syntymaaika = henkilotiedot
										.get(SYNTYMAAIKA);
								final Date lukuvuosi = getPaivamaaraSource()
										.lukuvuosi(getHaku());
								final Date poimintapaivamaara = getPaivamaaraSource()
										.poimintapaivamaara(getHaku());
								LogEntry valintaEntry = tilaSource.getVastaanottopvm(hakemus.getOid(), hakuOid , hakukohdeOid, hakutoive.getValintatapajonoOid());
								
								if (valintaEntry==null) {
									LOG.error("ERROR vastaanottopaivamaaraa ei löytynyt (tila ei VASTAANOTTANUT_SITOVASTI, VASTAANOTTANUT tai EHDOLLISESTI_VASTAANOTTANUT) :"+hakutoive.getTarjoajaOid()+ ":" + sukunimi+ " "+ etunimi+ "("+henkilotunnus+") hakukohde:"+hakukohdeOid+ " ");
									return;
								}
								
								final Date valintapaivamaara = valintaEntry.getLuotu();

								if( valintapaivamaara == null 
										|| 	valintapaivamaara.compareTo(alkuPvm)<=0
										|| 	valintapaivamaara.compareTo(loppuPvm)>=0){
									return;
								}
								
								String organisaatioOid = hakukohdeSource.getHakukohdeByOid(hakukohdeOid).getTarjoajaOid();
								String oppilaitosnumero = "XXXXX";
								
								if (organisaatioOid==null || organisaatioOid.equalsIgnoreCase("undefined")) {
									LOG.error("ERROR : tarjoaja :'"+hakutoive.getTarjoajaOid()+ "':" + sukunimi+ " "+ etunimi+ "("+henkilotunnus+") hakukohde:"+hakukohdeOid+ " "+" oppilaitosnumero will be marked as X");
								} else {	
									oppilaitosnumero = oppilaitosSource.getOppilaitosnumero(organisaatioOid);
								}
								
								final String tutkinnontaso = tutkinnontasoSource.getTutkinnontaso(hakukohdeOid);
								final String siirtotunnus = tutkinnontasoSource.getKoulutusaste(hakukohdeOid);

								valitut.add(new KelaHakijaRivi(hakemus.getOid(), siirtotunnus, etunimi, sukunimi,
										henkilotunnus, lukuvuosi, poimintapaivamaara,
										valintapaivamaara, oppilaitosnumero,
										organisaatioOid, hakuOid, hakukohdeOid, syntymaaika, tutkinnontaso));
							});
		}
		return valitut;
	}

	@Override
	public Collection<String> getHakemusOids() {
		return hakijat.stream().map(h -> h.getHakemusOid())
				.collect(Collectors.toList());
	}
}
