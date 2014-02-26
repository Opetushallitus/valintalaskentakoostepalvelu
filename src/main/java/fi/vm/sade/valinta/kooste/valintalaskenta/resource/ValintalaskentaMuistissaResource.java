package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import static fi.vm.sade.tarjonta.service.types.TarjontaTila.JULKAISTU;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Collection;

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
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.tarjonta.service.TarjontaPublicService;
import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.dto.Vastaus;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaCache;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaMuistissaProsessi;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaMuistissaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaTila;
import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoService;

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

	@GET
	@Path("/status")
	@Produces(APPLICATION_JSON)
	@ApiOperation(value = "Muistinvaraisen valintalaskennan tila", response = Collection.class)
	public Collection<ProsessiJaStatus<ValintalaskentaMuistissaProsessi>> status() {
		return valintalaskentaMuistissaValvomo
				.getUusimmatProsessitJaStatukset();
	}

	@GET
	@Path("/status/{uuid}")
	@Produces(APPLICATION_JSON)
	@ApiOperation(value = "Muistinvaraisen valintalaskennan tila", response = Collection.class)
	public ProsessiJaStatus<ValintalaskentaMuistissaProsessi> status(
			@PathParam("uuid") String uuid) {
		return valintalaskentaMuistissaValvomo.getProsessiJaStatus(uuid);
	}

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

	@GET
	@Path("/aktiivinenValintalaskenta")
	@Consumes("application/json")
	@ApiOperation(value = "Palauttaa suorituksessa olevan valintalaskentaprosessin", response = Vastaus.class)
	public Vastaus aktiivinenValintalaskentaProsessi() {
		return Vastaus.uudelleenOhjaus(valintalaskentaTila
				.getKaynnissaOlevaValintalaskenta().get().getId());
	}

	@POST
	@Path("/keskeytaAktiivinenValintalaskenta")
	@Consumes("application/json")
	@ApiOperation(value = "Keskeyttää suorituksessa olevan valintalaskentaprosessin", response = Vastaus.class)
	public Response keskeytaValintalaskentaProsessi() {
		valintalaskentaTila.getKaynnissaOlevaValintalaskenta().getAndSet(null)
				.peruuta();
		return Response.ok().build();
	}

	@POST
	@Path("/aktivoi")
	@Consumes("application/json")
	@ApiOperation(value = "Valintalaskennan aktivointi haulle ilman annettuja hakukohteita", response = Vastaus.class)
	public Vastaus aktivoiHaunValintalaskentaIlmanAnnettujaHakukohteita(
			@QueryParam("hakuOid") String hakuOid,
			Collection<String> blacklistOids) throws Exception {
		ValintalaskentaMuistissaProsessi prosessi = new ValintalaskentaMuistissaProsessi(
				hakuOid);
		ValintalaskentaMuistissaProsessi vanhaProsessi = valintalaskentaTila
				.getKaynnissaOlevaValintalaskenta().get();
		/**
		 * Vanha prosessi ylikirjoitetaan surutta jos siinä oli poikkeuksia
		 */
		if (vanhaProsessi != null && vanhaProsessi.hasPoikkeuksia()) {
			valintalaskentaTila.getKaynnissaOlevaValintalaskenta().set(null);
		}
		if (valintalaskentaTila.getKaynnissaOlevaValintalaskenta()
				.compareAndSet(null, prosessi)) {
			Collection<String> kasiteltavatHakukohteet;
			if (blacklistOids == null || blacklistOids.isEmpty()) {
				kasiteltavatHakukohteet = getHakukohdeOids(hakuOid);
			} else {
				kasiteltavatHakukohteet = getHakukohdeOids(hakuOid);
				kasiteltavatHakukohteet.removeAll(blacklistOids);
			}
			LOG.info("Käynnistetään valintalaskenta prosessille {}",
					prosessi.getId());
			valintalaskentaMuistissa.aktivoiValintalaskenta(prosessi,
					new ValintalaskentaCache(kasiteltavatHakukohteet), hakuOid,
					SecurityContextHolder.getContext().getAuthentication());
		} else {
			throw new RuntimeException("Valintalaskenta on jo käynnissä");
		}
		LOG.info("Valintalaskenta käynnissä");
		return Vastaus.uudelleenOhjaus(prosessi.getId());
	}

	private Collection<String> getHakukohdeOids(
			@Property(OPH.HAKUOID) String hakuOid) throws Exception {
		return Collections2.transform(Collections2.filter(tarjontaService
				.haeTarjonta(hakuOid).getHakukohde(),
				new Predicate<HakukohdeTyyppi>() {
					public boolean apply(HakukohdeTyyppi hakukohde) {
						return JULKAISTU == hakukohde.getHakukohteenTila();
					}
				}), new Function<HakukohdeTyyppi, String>() {
			public String apply(HakukohdeTyyppi input) {
				return input.getOid();
			}
		});

	}
}
