package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import java.io.InputStream;



import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;














import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import rx.schedulers.Schedulers;
import static rx.Observable.*;
import fi.vm.sade.sijoittelu.domain.HakemuksenTila;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.IlmoittautumisTila;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuExcel;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRiviKuuntelija;
import fi.vm.sade.valinta.kooste.erillishaku.service.ErillishaunTuontiService;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class ErillishaunTuontiServiceImpl implements ErillishaunTuontiService {
	
	private static final Logger LOG = LoggerFactory
			.getLogger(ErillishaunTuontiServiceImpl.class);
	private final TilaAsyncResource tilaAsyncResource;
	private final ApplicationAsyncResource applicationAsyncResource;

	@Autowired
	public ErillishaunTuontiServiceImpl(
			TilaAsyncResource tilaAsyncResource,
			ApplicationAsyncResource applicationAsyncResource) {
		this.applicationAsyncResource = applicationAsyncResource;
		this.tilaAsyncResource = tilaAsyncResource;
	}
	
	@Override
	public void tuo(KirjeProsessi prosessi, ErillishakuDTO erillishaku, InputStream data) {
		
		from(applicationAsyncResource.getApplicationsByOid(erillishaku.getHakuOid(), erillishaku.getHakukohdeOid()))
		//
				.subscribeOn(Schedulers.newThread())
				//
				.subscribe(hakemukset -> {
					final Map<String,Hakemus> hetuToHakemus = hakemukset.stream().collect(Collectors.toMap(
							h -> new HakemusWrapper(h).getHenkilotunnusTaiSyntymaaika(),h -> h
							));
					final Collection<ErillishaunHakijaDTO> hakijat = Lists.newArrayList();
					ErillishakuRiviKuuntelija kuuntelija = new ErillishakuRiviKuuntelija() {
						public void erillishakuRiviTapahtuma(ErillishakuRivi rivi) {
							ErillishaunHakijaDTO hakija = new ErillishaunHakijaDTO();
							Hakemus hakemus = null;
							
							if(hetuToHakemus.containsKey(rivi.getHenkilotunnus())) {
								hakemus = hetuToHakemus.get(rivi.getHenkilotunnus());
							} else if(hetuToHakemus.containsKey(rivi.getSyntymaAika())) {
								hakemus = hetuToHakemus.get(rivi.getSyntymaAika());
							} else {
								LOG.error("Hakija rivia {} ei voitu mapata oikeaan hakemukseen!");
								// ohitus vai poikkeus?
								return; // ohitus
							}
							HakemusWrapper wrapper = new HakemusWrapper(hakemus);
							try {
							hakija.setHakemuksenTila(HakemuksenTila.valueOf(rivi.getHakemuksenTila()));
							}catch(Exception e) {
								// tuntematon tila tai todennakoisesti asettamatta
								hakija.setHakemuksenTila(HakemuksenTila.HYLATTY);
							}
							hakija.setHakemusOid(hakemus.getOid());
							hakija.setHakijaOid(hakemus.getPersonOid());
							hakija.setHakukohdeOid(erillishaku.getHakukohdeOid());
							hakija.setHakuOid(erillishaku.getHakuOid());
							
							try {
								hakija.setIlmoittautumisTila(fi.vm.sade.sijoittelu.domain.IlmoittautumisTila.valueOf(rivi.getIlmoittautumisTila()));
								}catch(Exception e) {
									// tuntematon tila tai todennakoisesti asettamatta
									hakija.setIlmoittautumisTila(null);
								}
							try {
								hakija.setValintatuloksenTila(fi.vm.sade.sijoittelu.domain.ValintatuloksenTila.valueOf(rivi.getVastaanottoTila()));
								}catch(Exception e) {
									// tuntematon tila tai todennakoisesti asettamatta
									hakija.setIlmoittautumisTila(null);
								}
							hakija.setJulkaistavissa(wrapper.getLupaJulkaisuun());
							hakija.setTarjoajaOid(erillishaku.getTarjoajaOid());
							hakija.setValintatapajonoOid(erillishaku.getValintatapajonoOid());
							hakijat.add(hakija);
						}
					};
					ErillishakuExcel erillishakuExcel = new ErillishakuExcel(erillishaku.getHakutyyppi(), kuuntelija);
					try {
						erillishakuExcel.getExcel().tuoXlsx(data);
					} catch (Exception e) {
						LOG.error("Excelin tuonti epaonnistui! {}", e.getMessage());
						throw new RuntimeException(e);
					}
					if(!hakijat.isEmpty()) {
						tilaAsyncResource.tuoErillishaunTilat(erillishaku.getHakuOid(), erillishaku.getHakukohdeOid(), hakijat);
					} else {
						LOG.error("Taulukkolaskentatiedostosta ei saatu poimittua yhtaan hakijaa sijoitteluun tuotavaksi!");
						throw new RuntimeException("Taulukkolaskentatiedostosta ei saatu poimittua yhtaan hakijaa sijoitteluun tuotavaksi!");
					}
					prosessi.vaiheValmistui();
					prosessi.valmistui("ok");
				},
				poikkeus-> {
					LOG.error("Erillishaun tuonti keskeytyi virheeseen {}. {}", poikkeus.getMessage(), Arrays.toString(poikkeus.getStackTrace()));
					prosessi.keskeyta();
				});
	}
	
	public void erillishaunTuonti() {
		
	}
}
