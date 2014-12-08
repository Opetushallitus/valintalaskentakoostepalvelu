package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import static rx.Observable.from;
import static rx.Observable.zip;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rx.Observable;
import rx.functions.Action3;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import static com.google.common.collect.Lists.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultiset;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.Organisaatio;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HyvaksymiskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.LueHakijapalvelunOsoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.predicate.*;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeetService;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class HyvaksymiskirjeetServiceImpl implements HyvaksymiskirjeetService {

	private static final Logger LOG = LoggerFactory
			.getLogger(HyvaksymiskirjeetServiceImpl.class);
	private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
	private final HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti;
	// private final SijoitteluKoulutuspaikkallisetKomponentti sijoitteluProxy;
	private final SijoitteluAsyncResource sijoitteluAsyncResource;
	private final ApplicationAsyncResource applicationAsyncResource;
	private final OrganisaatioAsyncResource organisaatioAsyncResource;
	private final HaeOsoiteKomponentti haeOsoiteKomponentti;

	@Autowired
	public HyvaksymiskirjeetServiceImpl(
			ViestintapalveluAsyncResource viestintapalveluAsyncResource,
			HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti,
			SijoitteluAsyncResource sijoitteluAsyncResource,
			ApplicationAsyncResource applicationAsyncResource,
			OrganisaatioAsyncResource organisaatioAsyncResource,
			HaeOsoiteKomponentti haeOsoiteKomponentti) {
		this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
		this.hyvaksymiskirjeetKomponentti = hyvaksymiskirjeetKomponentti;
		this.sijoitteluAsyncResource = sijoitteluAsyncResource;
		this.applicationAsyncResource = applicationAsyncResource;
		this.organisaatioAsyncResource = organisaatioAsyncResource;
		this.haeOsoiteKomponentti = haeOsoiteKomponentti;
	}
	
	private Organisaatio responseToOrganisaatio(Response organisaatioResponse) throws IOException {
		InputStream stream = (InputStream) organisaatioResponse.getEntity();
		String json = StringUtils.trimToEmpty(IOUtils.toString(stream));
		IOUtils.closeQuietly(stream);
		return new Gson().fromJson(json,Organisaatio.class);
	}
	private Osoite haeOsoiteHierarkisesti(String kieli, List<String> oids, Organisaatio rdto, Teksti organisaationimi) {
		Osoite hakijapalveluidenOsoite = null;
		try {
			if(organisaationimi.isArvoton()) {
				organisaationimi = new Teksti(rdto.getNimi());
			}
			hakijapalveluidenOsoite = LueHakijapalvelunOsoite
					.lueHakijapalvelunOsoite(haeOsoiteKomponentti,
							kieli,rdto, organisaationimi);
			if(rdto == null) {
				LOG.error("Organisaatiopalvelusta ei saatu organisaatiota tunnisteelle {}. Eli ei saatu hakijapalveluiden osoitetta.", Arrays.toString(oids.toArray()));
				return null;
			}
			oids.add(rdto.getParentOid());
			if(hakijapalveluidenOsoite != null) {
				LOG.error("Hakijapalveluiden osoite saatiin tarjoajalta {}.\r\n{}", Arrays.toString(oids.toArray()), new GsonBuilder().setPrettyPrinting().create().toJson(hakijapalveluidenOsoite));
				return hakijapalveluidenOsoite;
			}
			if(hakijapalveluidenOsoite==null && rdto.getParentOid() != null) {
				LOG.error("Ei saatu hakijapalveluiden osoitetta talta organisaatiolta. Tutkitaan seuraava {}", Arrays.toString(oids.toArray()));
				return haeOsoiteHierarkisesti(kieli, oids, responseToOrganisaatio(organisaatioAsyncResource
				.haeOrganisaatio(rdto.getParentOid()).get()), organisaationimi);
			} else {
				LOG.error("Ei saatu hakijapalveluiden osoitetta! Kaytiin lapi organisaatiot {}!", Arrays.toString(oids.toArray()));
				return null;
			}
		} catch (Exception e) {
			LOG.error(
					"Hakijapalveluiden osoitteen haussa odottamaton virhe {},\r\n{}",
					e.getMessage(),
					Arrays.toString(e.getStackTrace()));
		}
		return null;
	}
	private Osoite organisaatioResponseToHakijapalveluidenOsoite(List<String> oids, String kieli, Response organisaatioResponse){
		Organisaatio org;
		Teksti organisaationimi;
		try {
			 org = responseToOrganisaatio(organisaatioResponse);
			 organisaationimi = new Teksti(org.getNimi());
		}catch(Exception e) {
			LOG.error("Ei saatu organisaatiota! {} {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
			throw new RuntimeException(e);
		}
		return haeOsoiteHierarkisesti(
				kieli,oids, org, organisaationimi);
	}
	
	private Map<String, TreeMultiset<Integer>> todellisenJonosijanRatkaisin(Collection<HakijaDTO> hakukohteenHakijat) {
		Map<String,  TreeMultiset<Integer>> valintatapajonoToJonosijaToHakija = Maps.newHashMap();
		for (HakijaDTO hakija : hakukohteenHakijat) {
			for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {
				for (HakutoiveenValintatapajonoDTO valintatapajono : hakutoive
						.getHakutoiveenValintatapajonot()) {
					
					if(!valintatapajonoToJonosijaToHakija.containsKey(valintatapajono.getValintatapajonoOid())) {
						valintatapajonoToJonosijaToHakija.put(valintatapajono.getValintatapajonoOid(), 
								
								TreeMultiset.<Integer>create());
					}
					int kkJonosija = Optional.ofNullable(
							valintatapajono.getJonosija()).orElse(0)
							+ Optional.ofNullable(
									valintatapajono.getTasasijaJonosija())
									.orElse(0) - 1;
					//if(hakutoive.)
					valintatapajonoToJonosijaToHakija.get(valintatapajono.getValintatapajonoOid()).add(kkJonosija);
				}
			}
		}
		return valintatapajonoToJonosijaToHakija;
	}
	
	@Override
	public void hyvaksymiskirjeetHakemuksille(final KirjeProsessi prosessi,
			final HyvaksymiskirjeDTO hyvaksymiskirjeDTO,
			final List<String> hakemusOids) {
		Future<List<Hakemus>> hakemuksetFuture = applicationAsyncResource
				.getApplicationsByOids(hakemusOids);
		Future<HakijaPaginationObject> hakijatFuture = sijoitteluAsyncResource
				.getKoulutuspaikkallisetHakijat(
						hyvaksymiskirjeDTO.getHakuOid(),
						hyvaksymiskirjeDTO.getHakukohdeOid());
		Future<Response> organisaatioFuture = organisaatioAsyncResource
				.haeOrganisaatio(hyvaksymiskirjeDTO.getTarjoajaOid());
		final String hakukohdeOid = hyvaksymiskirjeDTO
				.getHakukohdeOid();
		
		zip(
				from(hakemuksetFuture),
				from(hakijatFuture),
				from(organisaatioFuture),
				(hakemukset, hakijat, organisaatioResponse) -> {
					LOG.info("Tehdaan valituille hakijoille hyvaksytyt filtterointi.");

					final Set<String> kohdeHakijat = Sets
							.newHashSet(hakemusOids);
					
					BiPredicate<HakijaDTO, String> kohdeHakukohteessaHyvaksytty = new SijoittelussaHyvaksyttyHakijaBiPredicate();

					Collection<HakijaDTO> kohdeHakukohteessaHyvaksytyt = hakijat
							.getResults()
							.stream()
							.filter(h -> kohdeHakukohteessaHyvaksytty.test(h,
									hakukohdeOid))
							//
							.filter(h -> kohdeHakijat.contains(h
									.getHakemusOid()))
							//
							.collect(Collectors.toList());
					Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet = hyvaksymiskirjeetKomponentti
							.haeKiinnostavatHakukohteet(kohdeHakukohteessaHyvaksytyt);
					MetaHakukohde kohdeHakukohde = hyvaksymiskirjeessaKaytetytHakukohteet
							.get(hyvaksymiskirjeDTO.getHakukohdeOid());
					
					return hyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
							todellisenJonosijanRatkaisin(hakijat
							.getResults()),
							organisaatioResponseToHakijapalveluidenOsoite(newArrayList(Arrays.asList(hyvaksymiskirjeDTO.getTarjoajaOid())), 
									kohdeHakukohde.getHakukohteenKieli(), organisaatioResponse),
							hyvaksymiskirjeessaKaytetytHakukohteet,
							kohdeHakukohteessaHyvaksytyt, hakemukset,
							hyvaksymiskirjeDTO.getHakukohdeOid(),
							hyvaksymiskirjeDTO.getHakuOid(),
							hyvaksymiskirjeDTO.getTarjoajaOid(),
							hyvaksymiskirjeDTO.getSisalto(),
							hyvaksymiskirjeDTO.getTag(),
							hyvaksymiskirjeDTO.getTemplateName(),
							hyvaksymiskirjeDTO.getPalautusPvm(),
							hyvaksymiskirjeDTO.getPalautusAika());
				})
		//
				.subscribeOn(Schedulers.newThread())
				//
				.subscribe(
						letterBatch -> {
							letterBatchToViestintapalvelu().call(letterBatch,
									prosessi, hyvaksymiskirjeDTO);
						},
						throwable -> {
							LOG.error(
									"Sijoittelu tai hakemuspalvelukutsu epaonnistui {} {}",
									throwable.getMessage(), Arrays.toString(throwable.getStackTrace()));
							prosessi.keskeyta();
						});
	}
	@Override
	public void jalkiohjauskirjeHakukohteelle(final KirjeProsessi prosessi,
			final HyvaksymiskirjeDTO hyvaksymiskirjeDTO) {
		Future<List<Hakemus>> hakemuksetFuture = applicationAsyncResource
				.getApplicationsByOid(hyvaksymiskirjeDTO.getHakuOid(),
						hyvaksymiskirjeDTO.getHakukohdeOid());
		Future<HakijaPaginationObject> hakijatFuture = sijoitteluAsyncResource
				.getKaikkiHakijat(
						hyvaksymiskirjeDTO.getHakuOid(),
						hyvaksymiskirjeDTO.getHakukohdeOid());
		Future<Response> organisaatioFuture = organisaatioAsyncResource
				.haeOrganisaatio(hyvaksymiskirjeDTO.getTarjoajaOid());
		final String hakukohdeOid = hyvaksymiskirjeDTO
				.getHakukohdeOid();
		zip(
				from(hakemuksetFuture),
				from(hakijatFuture),
				from(organisaatioFuture),
				(hakemukset, hakijat, organisaatioResponse) -> {
					
					LOG.error("Tehdaan hakukohteeseen valitsemattomille filtterointi. Saatiin hakijoita {}", hakijat.getResults().size());
					
					BiPredicate<HakijaDTO, String> kohdeHakukohteessaHylatytTest = new HaussaHylattyHakijaBiPredicate();

					Collection<HakijaDTO> kohdeHakukohteessaHylatyt = hakijat
							.getResults()
							.stream()
							.filter(h -> kohdeHakukohteessaHylatytTest.test(h,
									hakukohdeOid)).collect(Collectors.toList());
					if(kohdeHakukohteessaHylatyt.isEmpty()) {
						LOG.error(
								"Hakukohteessa {} ei ole jälkiohjattavia hakijoita!",
								hyvaksymiskirjeDTO.getHakukohdeOid());
						prosessi.keskeyta("Hakukohteessa ei ole jälkiohjattavia hakijoita!");
						throw new RuntimeException("Hakukohteessa "+hyvaksymiskirjeDTO.getHakukohdeOid()+" ei ole jälkiohjattavia hakijoita!");
					}
					Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet = hyvaksymiskirjeetKomponentti
							.haeKiinnostavatHakukohteet(kohdeHakukohteessaHylatyt);
					MetaHakukohde kohdeHakukohde = hyvaksymiskirjeessaKaytetytHakukohteet
							.get(hyvaksymiskirjeDTO.getHakukohdeOid());
					List<String> tarjoajaOidList = Arrays.asList(hyvaksymiskirjeDTO.getTarjoajaOid());
					Osoite hakijapalveluidenOsoite = organisaatioResponseToHakijapalveluidenOsoite(tarjoajaOidList, 
							kohdeHakukohde.getHakukohteenKieli(), organisaatioResponse);
					return hyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
							todellisenJonosijanRatkaisin(hakijat
									.getResults()),
							hakijapalveluidenOsoite,
							hyvaksymiskirjeessaKaytetytHakukohteet,
							kohdeHakukohteessaHylatyt, hakemukset,
							hyvaksymiskirjeDTO.getHakukohdeOid(),
							hyvaksymiskirjeDTO.getHakuOid(),
							hyvaksymiskirjeDTO.getTarjoajaOid(),
							hyvaksymiskirjeDTO.getSisalto(),
							hyvaksymiskirjeDTO.getTag(),
							hyvaksymiskirjeDTO.getTemplateName(),
							hyvaksymiskirjeDTO.getPalautusPvm(),
							hyvaksymiskirjeDTO.getPalautusAika());
				})
		//
				.subscribeOn(Schedulers.newThread())
				//
				.subscribe(
						letterBatch -> {
							letterBatchToViestintapalvelu().call(letterBatch,
									prosessi, hyvaksymiskirjeDTO);
						},
						throwable -> {
							LOG.error(
									"Sijoittelu tai hakemuspalvelukutsu epaonnistui {} {}",
									throwable.getMessage(), Arrays.toString(throwable.getStackTrace()));
							prosessi.keskeyta();
						});
	}
	@Override
	public void hyvaksymiskirjeetHakukohteelle(KirjeProsessi prosessi,
			final HyvaksymiskirjeDTO hyvaksymiskirjeDTO) {
		Future<List<Hakemus>> hakemuksetFuture = applicationAsyncResource
				.getApplicationsByOid(hyvaksymiskirjeDTO.getHakuOid(),
						hyvaksymiskirjeDTO.getHakukohdeOid());
		Future<HakijaPaginationObject> hakijatFuture = sijoitteluAsyncResource
				.getKoulutuspaikkallisetHakijat(
						hyvaksymiskirjeDTO.getHakuOid(),
						hyvaksymiskirjeDTO.getHakukohdeOid());
		Future<Response> organisaatioFuture = organisaatioAsyncResource
				.haeOrganisaatio(hyvaksymiskirjeDTO.getTarjoajaOid());
		final String hakukohdeOid = hyvaksymiskirjeDTO
				.getHakukohdeOid();
		zip(
				from(hakemuksetFuture),
				from(hakijatFuture),
				from(organisaatioFuture),
				(hakemukset, hakijat, organisaatioResponse) -> {

					LOG.info("Tehdaan hakukohteeseen valituille hyvaksytyt filtterointi.");
					
					BiPredicate<HakijaDTO, String> kohdeHakukohteessaHyvaksytty = new SijoittelussaHyvaksyttyHakijaBiPredicate();

					Collection<HakijaDTO> kohdeHakukohteessaHyvaksytyt = hakijat
							.getResults()
							.stream()
							.filter(h -> kohdeHakukohteessaHyvaksytty.test(h,
									hakukohdeOid)).collect(Collectors.toList());
					Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet = hyvaksymiskirjeetKomponentti
							.haeKiinnostavatHakukohteet(kohdeHakukohteessaHyvaksytyt);
					MetaHakukohde kohdeHakukohde = hyvaksymiskirjeessaKaytetytHakukohteet
							.get(hyvaksymiskirjeDTO.getHakukohdeOid());
					
					return hyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
							todellisenJonosijanRatkaisin(hakijat
									.getResults()),
							organisaatioResponseToHakijapalveluidenOsoite(newArrayList(Arrays.asList(hyvaksymiskirjeDTO.getTarjoajaOid())), 
									kohdeHakukohde.getHakukohteenKieli(), organisaatioResponse),
							hyvaksymiskirjeessaKaytetytHakukohteet,
							kohdeHakukohteessaHyvaksytyt, hakemukset,
							hyvaksymiskirjeDTO.getHakukohdeOid(),
							hyvaksymiskirjeDTO.getHakuOid(),
							hyvaksymiskirjeDTO.getTarjoajaOid(),
							hyvaksymiskirjeDTO.getSisalto(),
							hyvaksymiskirjeDTO.getTag(),
							hyvaksymiskirjeDTO.getTemplateName(),
							hyvaksymiskirjeDTO.getPalautusPvm(),
							hyvaksymiskirjeDTO.getPalautusAika());
				})
		//
				.subscribeOn(Schedulers.newThread())
				//
				.subscribe(
						letterBatch -> {
							letterBatchToViestintapalvelu().call(letterBatch,
									prosessi, hyvaksymiskirjeDTO);
						},
						throwable -> {
							LOG.error(
									"Sijoittelu tai hakemuspalvelukutsu epaonnistui {} {}",
									throwable.getMessage(), Arrays.toString(throwable.getStackTrace()));
							prosessi.keskeyta();
						});
	}

	public Action3<LetterBatch, KirjeProsessi, HyvaksymiskirjeDTO> letterBatchToViestintapalvelu() {
		return (letterBatch, prosessi, kirje) -> {
			try {
				if (prosessi.isKeskeytetty()) {
					LOG.error("Hyvaksymiskirjeiden luonti on keskeytetty kayttajantoimesta!");
					return;
				}
				LOG.info("Tehdaan viestintapalvelukutsu kirjeille.");
				final String batchId; 
				try {
					batchId=viestintapalveluAsyncResource
						.viePdfJaOdotaReferenssi(letterBatch).get(165L,
								TimeUnit.SECONDS);
				}catch(Exception e){
					LOG.error("Viestintapalvelukutsu epaonnistui virheeseen {}", e.getMessage());
					throw new RuntimeException(e);
				}
				LOG.info("Saatiin kirjeen seurantaId {}", batchId);
				prosessi.vaiheValmistui();
				PublishSubject<String> stop = PublishSubject.create();
				Observable
						.interval(1, TimeUnit.SECONDS)
						.take(ViestintapalveluAsyncResource.VIESTINTAPALVELUN_MAKSIMI_POLLAUS_SEKUNTIA)
						.takeUntil(stop)
						.subscribe(
								pulse -> {
									try {
										LOG.warn(
												"Tehdaan status kutsu seurantaId:lle {}",
												batchId);
										LetterBatchStatusDto status = viestintapalveluAsyncResource
												.haeStatus(batchId).get(900L,
														TimeUnit.MILLISECONDS);
										if (prosessi.isKeskeytetty()) {
											LOG.error("Hyvaksymiskirjeiden luonti on keskeytetty kayttajantoimesta!");
											stop.onNext(null);
											return;
										}
										if ("error".equals(status.getStatus())) {
											LOG.error("Hyvaksymiskirjeiden muodostus paattyi viestintapalvelun sisaiseen virheeseen!");
											prosessi.keskeyta();
											stop.onNext(null);
										}
										if ("ready".equals(status.getStatus())) {
											prosessi.vaiheValmistui();
											LOG.error("Hyvaksymiskirjeet valmistui!");
											prosessi.valmistui(batchId);
											stop.onNext(null);
										}
									} catch (Exception e) {
										LOG.error(
												"Statuksen haku epaonnistui {}",
												e.getMessage());
									}

								}, throwable -> {
									prosessi.keskeyta();
								}, () -> {
									prosessi.keskeyta();
								});
			} catch (Exception e) {
				LOG.error("Virhe hakukohteelle {}: {}",
						kirje.getHakukohdeOid(), e.getMessage());
				prosessi.keskeyta();
			}
		};
	}
}
