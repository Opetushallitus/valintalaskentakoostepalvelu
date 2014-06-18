package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.tarjonta.service.TarjontaPublicService;
import fi.vm.sade.tarjonta.service.resources.v1.HakukohdeV1ResourceWrapper;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeHakutulosV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakutuloksetV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.TarjoajaHakutulosV1RDTO;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.dto.Vastaus;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaCache;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaMuistissaProsessi;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Varoitus;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaMuistissaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaTila;
import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;

/**
 * @author Jussi Jartamo
 */
@Controller
@Path("valintalaskentamuistissa")
@PreAuthorize("isAuthenticated()")
@Api(value = "/valintalaskentamuistissa", description = "Valintalaskenta muistinvaraisesti")
public class ValintalaskentaMuistissaResource {
	private static final Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaMuistissaResource.class);
	@Autowired
	private ValintalaskentaTila valintalaskentaTila;

	@Resource(name = "valintalaskentaMuistissaValvomo")
	private ValvomoService<ValintalaskentaMuistissaProsessi> valintalaskentaMuistissaValvomo;
	@Autowired
	private ValintalaskentaMuistissaRoute valintalaskentaMuistissa;
	@Autowired
	private TarjontaPublicService tarjontaService;
	@Autowired
	private HakukohdeV1ResourceWrapper hakukohdeResource;
	@Autowired
	private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;

	@GET
	@Path("/aktiivinenValintalaskenta")
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	@ApiOperation(value = "Palauttaa suorituksessa olevan valintalaskentaprosessin", response = Vastaus.class)
	public Vastaus aktiivinenValintalaskentaProsessi() {
		return Vastaus.uudelleenOhjaus(valintalaskentaTila
				.getKaynnissaOlevaValintalaskenta().get().getId());
	}

	@POST
	@Path("/keskeytaAktiivinenValintalaskenta")
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	@ApiOperation(value = "Keskeyttää suorituksessa olevan valintalaskentaprosessin", response = Vastaus.class)
	public Response keskeytaValintalaskentaProsessi(
			@QueryParam("uuid") String uuid) {
		ValintalaskentaMuistissaProsessi v = (ValintalaskentaMuistissaProsessi) dokumenttiProsessiKomponentti
				.haeProsessi(uuid);// (prosessi);
		v.peruuta();
		// valintalaskentaTila.getKaynnissaOlevaValintalaskenta().getAndSet(null)
		// .peruuta();
		return Response.ok().build();
	}

	/**
	 * 
	 * @param hakuOid
	 * @param hakukohdeOid
	 * @param valinnanvaihe
	 * @param onkoWhitelist
	 *            blacklist jos false
	 * @param blacklistOids
	 * @return
	 * @throws Exception
	 */
	@POST
	@Path("/aktivoi")
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	@ApiOperation(value = "Valintalaskennan aktivointi haulle ilman annettuja hakukohteita. Valinnaisen muuttujan onkoWhitelist ollessa true annettu oid-blacklist tulkitaan whitelistiksi.", response = Vastaus.class)
	public Vastaus aktivoiHaunValintalaskentaIlmanAnnettujaHakukohteita(
			@QueryParam("hakuOid") String hakuOid,
			@QueryParam("hakukohdeOid") String hakukohdeOid,
			@QueryParam("valinnanvaihe") Integer valinnanvaihe,
			@QueryParam("onkoWhitelist") Boolean onkoWhitelist,
			Collection<String> blacklistOids) throws Exception {
		ValintalaskentaMuistissaProsessi prosessi = new ValintalaskentaMuistissaProsessi(
				hakuOid);
		if (hakukohdeOid != null) {
			// jos tehdään yksittäiselle haulle niin ei blokata yhtäaikaisia
			// ajoja.
			kaynnistaLaskenta(hakuOid, hakukohdeOid, valinnanvaihe,
					blacklistOids, false, prosessi);
			return Vastaus.uudelleenOhjaus(prosessi.getId());
		} else {
			ValintalaskentaMuistissaProsessi vanhaProsessi = valintalaskentaTila
					.getKaynnissaOlevaValintalaskenta().get();

			/**
			 * Vanha prosessi ylikirjoitetaan surutta jos siinä oli poikkeuksia
			 */
			if (vanhaProsessi != null) {
				if (vanhaProsessi.hasPoikkeuksia()
						|| vanhaProsessi.getValintalaskenta().isValmis()) {
					// saa ylittaa
					valintalaskentaTila.getKaynnissaOlevaValintalaskenta()
							.compareAndSet(vanhaProsessi, null);
				}
			}
			if (valintalaskentaTila.getKaynnissaOlevaValintalaskenta()
					.compareAndSet(null, prosessi)) {
				kaynnistaLaskenta(hakuOid, hakukohdeOid, valinnanvaihe,
						blacklistOids, Boolean.TRUE.equals(onkoWhitelist),
						prosessi);
			} else {
				throw new RuntimeException("Valintalaskenta on jo käynnissä");
			}
			LOG.info("Valintalaskenta käynnissä");
			return Vastaus.uudelleenOhjaus(prosessi.getId());
		}
	}

	private void kaynnistaLaskenta(String hakuOid, String hakukohdeOid,
			Integer valinnanvaihe, Collection<String> blacklistOids,
			boolean onkoWhitelist, ValintalaskentaMuistissaProsessi prosessi)
			throws Exception {
		Set<String> kasiteltavatHakukohteet;
		if (onkoWhitelist && (blacklistOids == null || blacklistOids.isEmpty())) {
			prosessi.addException("Laskenta tyhjällä whitelistillä on päättynyt!");
			throw new RuntimeException(
					"Yritettiin käynnistää laskenta ilman hakukohteita laskennalle!");
		}
		try {
			if (blacklistOids == null || blacklistOids.isEmpty()) {

				if (hakukohdeOid != null) {
					// Vain yhdelle hakukohteelle
					kasiteltavatHakukohteet = Sets.newHashSet(hakukohdeOid);
				} else {
					kasiteltavatHakukohteet = getHakukohdeOids(hakuOid);
				}
			} else {
				kasiteltavatHakukohteet = getHakukohdeOids(hakuOid);
				if (onkoWhitelist) {

					kasiteltavatHakukohteet.retainAll(blacklistOids);
					// whitelistissa enemman kuin haussa
					if (kasiteltavatHakukohteet.size() != blacklistOids.size()) {
						Set<String> ylimaaraiset = Sets
								.newHashSet(kasiteltavatHakukohteet);
						ylimaaraiset.removeAll(blacklistOids);
						for (String tuntematonOid : ylimaaraiset) {
							prosessi.getVaroitukset()
									.add(new Varoitus(tuntematonOid,
											"Whitelistissä oli oideja joita ei löydy hausta!"));
						}
					}
				} else {
					kasiteltavatHakukohteet.removeAll(blacklistOids);
				}
			}
		} catch (Exception e) {
			prosessi.addException("Tarjonnalta ei saatu haulle hakukohteita! "
					+ e.getMessage());
			throw e;
		}
		LOG.info("Käynnistetään valintalaskenta prosessille {}",
				prosessi.getId());
		valintalaskentaMuistissa.aktivoiValintalaskenta(prosessi,
				new ValintalaskentaCache(kasiteltavatHakukohteet), hakuOid,
				valinnanvaihe, SecurityContextHolder.getContext()
						.getAuthentication());
		dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
	}

	private Set<String> getHakukohdeOids(@Property(OPH.HAKUOID) String hakuOid)
			throws Exception {
		ResultV1RDTO<HakutuloksetV1RDTO<HakukohdeHakutulosV1RDTO>> r = hakukohdeResource
				.search(hakuOid, Arrays.asList("JULKAISTU"));

		return Sets
				.newHashSet(FluentIterable
				//
						.from(r.getResult().getTulokset())
						//
						.transformAndConcat(
								new Function<TarjoajaHakutulosV1RDTO<HakukohdeHakutulosV1RDTO>, List<HakukohdeHakutulosV1RDTO>>() {
									@Override
									public List<HakukohdeHakutulosV1RDTO> apply(
											TarjoajaHakutulosV1RDTO<HakukohdeHakutulosV1RDTO> input) {

										return input.getTulokset();
									}

								})
						//
						.transform(
								new Function<HakukohdeHakutulosV1RDTO, String>() {
									@Override
									public String apply(
											HakukohdeHakutulosV1RDTO i) {
										return i.getOid();
									}
								}));
	}

	/**
	 * @Deprecated Valvomossa ajatuksena oli saada statusta uusimmista käynnissä
	 *             olevista prosesseista. Koska käyttäjän on hyvä pystyä
	 *             välittämään komentoja käynnissä oleville töille niin
	 *             tarvitaan parempi tapa säilöä käynnissä olevat työt.
	 * 
	 * @return
	 */
	@Deprecated
	@GET
	@Path("/status")
	@Produces(APPLICATION_JSON)
	@ApiOperation(value = "Muistinvaraisen valintalaskennan tila", response = Collection.class)
	public Collection<ProsessiJaStatus<ValintalaskentaMuistissaProsessi>> status() {
		return valintalaskentaMuistissaValvomo
				.getUusimmatProsessitJaStatukset();
	}

	/**
	 * @Deprecated Valvomossa ajatuksena oli saada statusta uusimmista käynnissä
	 *             olevista prosesseista. Koska käyttäjän on hyvä pystyä
	 *             välittämään komentoja käynnissä oleville töille niin
	 *             tarvitaan parempi tapa säilöä käynnissä olevat työt.
	 * 
	 * @return
	 */
	@Deprecated
	@GET
	@Path("/status/{uuid}")
	@Produces(APPLICATION_JSON)
	@ApiOperation(value = "Muistinvaraisen valintalaskennan tila", response = Collection.class)
	public ProsessiJaStatus<ValintalaskentaMuistissaProsessi> status(
			@PathParam("uuid") String uuid) {
		return valintalaskentaMuistissaValvomo.getProsessiJaStatus(uuid);
	}

	/**
	 * @Deprecated Valvomossa ajatuksena oli saada statusta uusimmista käynnissä
	 *             olevista prosesseista. Koska käyttäjän on hyvä pystyä
	 *             välittämään komentoja käynnissä oleville töille niin
	 *             tarvitaan parempi tapa säilöä käynnissä olevat työt.
	 * 
	 * @return
	 */
	@Deprecated
	@GET
	@Path("/exceptions")
	@Produces(APPLICATION_JSON)
	@ApiOperation(value = "Muistinvaraisen valintalaskennan poikkeukset", response = Collection.class)
	public Collection<Collection<Exception>> poikkeukset() {
		return Collections2
				.transform(
						valintalaskentaMuistissaValvomo.getUusimmatProsessit(),
						new Function<ValintalaskentaMuistissaProsessi, Collection<Exception>>() {
							public Collection<Exception> apply(
									ValintalaskentaMuistissaProsessi input) {
								return input.getKokonaistyo().getPoikkeukset();
							}
						});
	}
}
