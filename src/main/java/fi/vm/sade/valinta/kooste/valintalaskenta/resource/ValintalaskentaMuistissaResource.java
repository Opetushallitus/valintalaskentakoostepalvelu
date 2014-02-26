package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import static fi.vm.sade.tarjonta.service.types.TarjontaTila.JULKAISTU;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Collection;

import javax.annotation.Resource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.camel.Property;
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
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaCache;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaMuistissaProsessi;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaMuistissaRoute;
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

	@POST
	@Path("/aktivoi")
	@Consumes("application/json")
	@ApiOperation(value = "Valintalaskennan aktivointi haulle ilman annettuja hakukohteita", response = Response.class)
	public Response aktivoiHaunValintalaskentaIlmanAnnettujaHakukohteita(
			@QueryParam("hakuOid") String hakuOid,
			Collection<String> blacklistOids) throws Exception {
		Collection<String> kasiteltavatHakukohteet;
		if (blacklistOids == null || blacklistOids.isEmpty()) {
			kasiteltavatHakukohteet = getHakukohdeOids(hakuOid);
		} else {
			kasiteltavatHakukohteet = getHakukohdeOids(hakuOid);
			kasiteltavatHakukohteet.removeAll(blacklistOids);
		}
		valintalaskentaMuistissa.aktivoiValintalaskentaAsync(
				new ValintalaskentaCache(kasiteltavatHakukohteet), hakuOid,
				SecurityContextHolder.getContext().getAuthentication());
		return Response.ok().build();
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
