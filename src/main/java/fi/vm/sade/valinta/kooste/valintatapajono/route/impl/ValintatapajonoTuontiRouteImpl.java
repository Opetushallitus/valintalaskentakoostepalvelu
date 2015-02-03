package fi.vm.sade.valinta.kooste.valintatapajono.route.impl;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.HakukohdeResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakuTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohdeNimiTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.util.EnumConverter;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoDataRiviListAdapter;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoExcel;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoRivi;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoRiviAsJonosijaConverter;
import fi.vm.sade.valinta.kooste.valintatapajono.route.ValintatapajonoTuontiRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.AbstractDokumenttiRouteBuilder;
import fi.vm.sade.valintalaskenta.domain.dto.JonosijaDTO;
import fi.vm.sade.valintalaskenta.domain.dto.ValintatapajonoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.Tasasijasaanto;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValintatapajonoDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class ValintatapajonoTuontiRouteImpl extends
		AbstractDokumenttiRouteBuilder {
	private static final Logger LOG = LoggerFactory
			.getLogger(ValintatapajonoTuontiRouteImpl.class);

	private final ValintaperusteetAsyncResource valintaperusteetResource;
	private final ApplicationResource applicationResource;
	private final HaeHakukohdeNimiTarjonnaltaKomponentti hakukohdeTarjonnalta;
	private final HaeHakuTarjonnaltaKomponentti hakuTarjonnalta;
	private final HakukohdeResource hakukohdeResource;

	@Autowired
	public ValintatapajonoTuontiRouteImpl(
			ApplicationResource applicationResource,
			ValintaperusteetAsyncResource valintaperusteetResource,
			HaeHakukohdeNimiTarjonnaltaKomponentti hakukohdeTarjonnalta,
			HaeHakuTarjonnaltaKomponentti hakuTarjonnalta,
			HakukohdeResource hakukohdeResource) {
		super();
		this.applicationResource = applicationResource;
		this.valintaperusteetResource = valintaperusteetResource;
		this.hakukohdeTarjonnalta = hakukohdeTarjonnalta;
		this.hakuTarjonnalta = hakuTarjonnalta;
		this.hakukohdeResource = hakukohdeResource;
	}

	@Override
	public void configure() throws Exception {
		Endpoint valintatapajonoTuonti = endpoint(ValintatapajonoTuontiRoute.SEDA_VALINTATAPAJONO_TUONTI);
		Endpoint luontiEpaonnistui = endpoint("direct:valintatapajono_tuonti_deadletterchannel");
		from(valintatapajonoTuonti)
		//
				.errorHandler(
				//
						deadLetterChannel(luontiEpaonnistui)
								//
								.maximumRedeliveries(0)
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))
				//
				.process(new Processor() {

					@Override
					public void process(Exchange exchange) throws Exception {
						dokumenttiprosessi(exchange).setKokonaistyo(
						// haun nimi ja hakukohteen nimi
								1 + 1 +
								// osallistumistiedot + valintaperusteet +
								// hakemuspistetiedot
										1 + 1
										// luonti
										+ 1
										// dokumenttipalveluun vienti
										+ 1);
						String hakuOid = hakuOid(exchange);
						String hakukohdeOid = hakukohdeOid(exchange);
						String hakuNimi = new Teksti(hakuTarjonnalta.getHaku(
								hakuOid).getNimi()).getTeksti();
						dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
						HakukohdeDTO hnimi = hakukohdeTarjonnalta
								.haeHakukohdeNimi(hakukohdeOid);
						dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
						// String tarjoajaOid = hnimi.getTarjoajaOid();
						String hakukohdeNimi = new Teksti(hnimi
								.getHakukohdeNimi()).getTeksti();
						// String tarjoajaNimi = new
						// Teksti(hnimi.getTarjoajaNimi()).getTeksti();
						//
						//
						//
						String valintatapajonoOid = valintatapajonoOid(exchange);
						if (hakukohdeOid == null || hakuOid == null
								|| valintatapajonoOid == null) {
							LOG.error(
									"Pakolliset tiedot reitille puuttuu hakuOid = {}, hakukohdeOid = {}, valintatapajonoOid = {}",
									hakuOid, hakukohdeOid, valintatapajonoOid);

							dokumenttiprosessi(exchange).getPoikkeukset().add(
									new Poikkeus(Poikkeus.KOOSTEPALVELU,
											"Puutteelliset lähtötiedot"));
							throw new RuntimeException(
									"Pakolliset tiedot reitille puuttuu hakuOid, hakukohdeOid, valintatapajonoOid");
						}
						final List<Hakemus> hakemukset;

						try {
							hakemukset = applicationResource
									.getApplicationsByOid(
											hakuOid,
											hakukohdeOid,
											ApplicationResource.ACTIVE_AND_INCOMPLETE,
											ApplicationResource.MAX);
							LOG.debug("Saatiin hakemukset {}",
									hakemukset.size());
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();
						} catch (Exception e) {
							LOG.error("Hakemuspalvelun virhe: {}\r\n{}",
									e.getMessage(),
									Arrays.toString(e.getStackTrace()));

							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.HAKU,
											"Hakemuspalvelulta ei saatu hakemuksia hakukohteelle",
											""));
							throw e;
						}
						if (hakemukset.isEmpty()) {
							LOG.error("Nolla hakemusta!");

							dokumenttiprosessi(exchange).getPoikkeukset().add(
									new Poikkeus(Poikkeus.HAKU,
											"Hakukohteella ei ole hakemuksia!",
											""));
							throw new RuntimeException(
									"Hakukohteelle saatiin tyhjä hakemusjoukko!");
						}
						final List<ValintatietoValinnanvaiheDTO> valinnanvaiheet;
						try {
							valinnanvaiheet = hakukohdeResource
									.hakukohde(hakukohdeOid);
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();
						} catch (Exception e) {
							LOG.error("Valinnanvaiheiden haku virhe: {}\r\n{}",
									e.getMessage(),
									Arrays.toString(e.getStackTrace()));

							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.VALINTALASKENTA,
											"Valintalaskennalta ei saatu valinnanvaiheita",
											""));
							throw e;
						}
						ValintatapajonoDataRiviListAdapter listaus = new ValintatapajonoDataRiviListAdapter();
						try {

							ValintatapajonoExcel valintatapajonoExcel = new ValintatapajonoExcel(
									hakuOid, hakukohdeOid, valintatapajonoOid,
									hakuNimi, hakukohdeNimi,
									//
									valinnanvaiheet, hakemukset, Arrays
											.asList(listaus));
							try {
								valintatapajonoExcel.getExcel().tuoXlsx(
										exchange.getIn().getBody(
												InputStream.class));
							} catch (Exception e) {
								valintatapajonoExcel.getExcel().tuoXlsx(
										exchange.getIn().getBody(
												InputStream.class));
							}
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();
						} catch (Exception e) {
							LOG.error(
									"Valintatapajono excelin luonti virhe: {}\r\n{}",
									e.getMessage(),
									Arrays.toString(e.getStackTrace()));

							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.KOOSTEPALVELU,
											"Valintatapajono exceliä ei saatu luotua!",
											""));
							throw e;
						}

						try {
							ValintatietoValinnanvaiheDTO vaihe = haeValinnanVaihe(
									valintatapajonoOid, valinnanvaiheet);
							if (vaihe == null) {
								vaihe = luoValinnanVaihe(hakukohdeOid, hakuOid,
										Optional.ofNullable(valintatapajonoOid));

							}
							ValintatapajonoDTO jono = haeValintatapajono(
									valintatapajonoOid, vaihe);
							List<JonosijaDTO> jonosijat = Lists.newArrayList();
							Map<String, Hakemus> hakemusmappaus = mapHakemukset(hakemukset);
							for (ValintatapajonoRivi rivi : listaus.getRivit()) {

								if (rivi.isValidi()) {
									jonosijat
											.add(ValintatapajonoRiviAsJonosijaConverter
													.convert(
															hakukohdeOid,
															rivi,
															hakemusmappaus.get(rivi
																	.getOid())));
								} else {
									LOG.warn("Rivi ei ole validi {} {} {}",
											rivi.getOid(), rivi.getJonosija(),
											rivi.getNimi());

								}
							}
							jono.setJonosijat(jonosijat);
							// LOG.error("\r\n{}", new GsonBuilder()
							// .setPrettyPrinting().create().toJson(vaihe));
							vaihe.setHakuOid(hakuOid);// jos valintalaskenta
														// osaa palauttaa
														// hakuOidittoman
														// vastauksen mutta ei
														// ottaa vastaan
														// sellaista
							vaihe.setCreatedAt(null); // ei konvertteria
														// paivamaaralle
							Response response = hakukohdeResource
									.lisaaTuloksia(hakukohdeOid, vaihe);
							if (Response.Status.ACCEPTED.getStatusCode() != response
									.getStatus()) {
								throw new RuntimeException(
										"Valintalaskenta ei hyväksynyt syötettyjä tietoja! Koodi "
												+ response.getStatus());
							}
						} catch (Exception e) {
							LOG.error(
									"Valintatapajono excelin luonti virhe: {}\r\n{}",
									e.getMessage(),
									Arrays.toString(e.getStackTrace()));

							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.VALINTALASKENTA,
											"Valinnanvaihetta ei saatu tallennettua!",
											""));
							throw e;
						}
						dokumenttiprosessi(exchange).setDokumenttiId("valmis");
						dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
					}

				})
				//
				.stop();
		/**
		 * DEAD LETTER CHANNEL
		 */
		from(luontiEpaonnistui)
		//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						String syy;
						if (exchange.getException() == null) {
							syy = "Valintatapajonon taulukkolaskentaan tuonti epäonnistui. Ota yheys ylläpitoon.";
						} else {
							syy = exchange.getException().getMessage();
						}
						dokumenttiprosessi(exchange).getPoikkeukset().add(
								new Poikkeus(Poikkeus.KOOSTEPALVELU,
										"Valintatapajonon tuonti", syy));
					}
				})
				//
				.stop();
	}

	private ValinnanVaiheJonoillaDTO haeVaihe(String oid,
			List<ValinnanVaiheJonoillaDTO> jonot) {
		for (ValinnanVaiheJonoillaDTO jonoilla : jonot) {
			for (fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoDTO v : jonoilla
					.getJonot()) {
				if (oid.equals(v.getOid())) {
					return jonoilla;
				}
			}
		}
		return null;
	}

	private fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoDTO haeJono(
			String oid, ValinnanVaiheJonoillaDTO vaihe) {

		for (fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoDTO v : vaihe
				.getJonot()) {
			if (oid.equals(v.getOid())) {
				return v;
			}
		}

		return null;
	}

	private ValintatietoValinnanvaiheDTO luoValinnanVaihe(String hakukohdeOid,
			String hakuOid, Optional<String> valintatapajonoOid) throws InterruptedException, ExecutionException {

        final List<ValinnanVaiheJonoillaDTO> ilmanLaskentaaVaiheet = valintaperusteetResource.ilmanLaskentaa(hakukohdeOid).get();
        List<fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoDTO> ilmanLaskentaaJonot;

        String jonoOid;

        if(!valintatapajonoOid.isPresent()) {
            ilmanLaskentaaJonot =
                    ilmanLaskentaaVaiheet
                            .stream()
                            .flatMap(v -> v.getJonot().stream())
                            .collect(Collectors.toList());
            if(ilmanLaskentaaJonot.isEmpty()) {
                throw new RuntimeException(
                        "Yhtään valintatapajonoa ilman laskentaa ei löytynyt");
            }

            if(ilmanLaskentaaJonot.size() > 1) {
                throw new RuntimeException(
                        "ValintatapajonoOidia ei annettu ja löytyi useampia kuin yksi valintatapajono ilman laskentaa");
            }

            jonoOid = ilmanLaskentaaJonot.get(0).getOid();
        } else {
            jonoOid = valintatapajonoOid.get();
        }

        ValinnanVaiheJonoillaDTO vaihe = haeVaihe(jonoOid,
				valintaperusteetResource.ilmanLaskentaa(hakukohdeOid).get());
		if (vaihe == null) {
			throw new RuntimeException(
					"Tälle valintatapajonolle ei löydy valintaperusteista valinnanvaihetta!");
		}
		fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoDTO jono = haeJono(
				jonoOid, vaihe);

		// luodaan uusi
		LOG.warn(
				"Valinnanvaihetta ei löytynyt valintatapajonolle({}) joten luodaan uusi!",
				valintatapajonoOid);
		ValintatietoValinnanvaiheDTO v0 = new ValintatietoValinnanvaiheDTO();
		v0.setCreatedAt(new Date());
		v0.setHakuOid(hakuOid);
		v0.setJarjestysnumero(0);
		v0.setNimi(vaihe.getNimi());
		v0.setValinnanvaiheoid(vaihe.getOid());
		ValintatietoValintatapajonoDTO vx = new ValintatietoValintatapajonoDTO();
		vx.setAloituspaikat(jono.getAloituspaikat());
		vx.setEiVarasijatayttoa(jono.getEiVarasijatayttoa());
		vx.setKaikkiEhdonTayttavatHyvaksytaan(jono
				.getKaikkiEhdonTayttavatHyvaksytaan());
		vx.setKaytetaanValintalaskentaa(jono.getKaytetaanValintalaskentaa());
		vx.setNimi(jono.getNimi());
		vx.setOid(jonoOid);
		vx.setPoissaOlevaTaytto(jono.getPoissaOlevaTaytto());
		vx.setPrioriteetti(0);
		vx.setSiirretaanSijoitteluun(jono.getSiirretaanSijoitteluun());
		vx.setTasasijasaanto(EnumConverter.convert(Tasasijasaanto.class,
				jono.getTasapistesaanto()));
		vx.setValintatapajonooid(jonoOid);
		v0.getValintatapajonot().add(vx);
		return v0;
	}

	private ValintatietoValinnanvaiheDTO haeValinnanVaihe(
			String valintatapajonoOid,
			Collection<ValintatietoValinnanvaiheDTO> v) {
		for (ValintatietoValinnanvaiheDTO v0 : v) {
			if (haeValintatapajono(valintatapajonoOid, v0) != null) {
				return v0;
			}
		}
		return null;
	}

	private ValintatapajonoDTO haeValintatapajono(String valintatapajonoOid,
			ValintatietoValinnanvaiheDTO v) {
		for (ValintatapajonoDTO vx : v.getValintatapajonot()) {
			if (valintatapajonoOid.equals(vx.getValintatapajonooid())) {
				return vx;
			}
		}
		return null;
	}

	private Map<String, Hakemus> mapHakemukset(Collection<Hakemus> hakemukset) {
		Map<String, Hakemus> tmp = Maps.newHashMap();
		for (Hakemus h : hakemukset) {
			tmp.put(h.getOid(), h);
		}
		return tmp;
	}

}
