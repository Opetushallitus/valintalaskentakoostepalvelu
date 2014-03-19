package fi.vm.sade.valinta.kooste.valintalaskenta.route.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.camel.CamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusList;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaMuistissaProsessi;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaMuistissaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaMuistissaRouteImpl.HakuAppHakemus;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaMuistissaRouteImpl.HakuAppHakemusOids;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaMuistissaRouteImpl.Valintalaskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaMuistissaRouteImpl.Valintaperusteet;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoService;
import fi.vm.sade.valinta.kooste.valvomo.service.impl.ValvomoServiceImpl;

/**
 * @author Jussi Jartamo.
 */
@Configuration
public class ValintalaskentaMuistissaConfig {
	private static final Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaMuistissaConfig.class);

	@Bean(name = "valintalaskentaMuistissaValvomo")
	public ValvomoService<ValintalaskentaMuistissaProsessi> getValvomoServiceImpl() {
		return new ValvomoServiceImpl<ValintalaskentaMuistissaProsessi>();
	}

	@Bean
	public ValintalaskentaTila getValintalaskentaTila() {
		return new ValintalaskentaTila();
	}

	@Bean
	public ValintalaskentaMuistissaRoute getValintalaskentaMuistissaRoute(
			@Value(ValintalaskentaMuistissaRoute.SEDA_VALINTALASKENTA_MUISTISSA) String routeId,
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper.createProxy(
				context.getEndpoint(routeId),
				ValintalaskentaMuistissaRoute.class);
	}

	@Bean
	public Valintalaskenta getValintalaskenta(
			final ValintalaskentaService valintalaskentaService) {
		return new Valintalaskenta() {
			@Override
			public void teeValintalaskenta(List<HakemusTyyppi> hakemukset,
					List<ValintaperusteetTyyppi> valintaperusteet) {
				valintalaskentaService.laske(hakemukset, valintaperusteet);
			}
		};
	}

	@Bean
	public Valintaperusteet getValintaperusteet(
			final ValintaperusteService valintaperusteService) {
		return new Valintaperusteet() {
			@Override
			public List<ValintaperusteetTyyppi> getValintaperusteet(
					String hakukohdeOid, Integer valinnanvaihe) {
				HakuparametritTyyppi params = new HakuparametritTyyppi();
				params.setHakukohdeOid(hakukohdeOid);
				params.setValinnanVaiheJarjestysluku(valinnanvaihe);
				return valintaperusteService.haeValintaperusteet(Arrays
						.asList(params));
			}
		};
	}

	@Bean
	public HakuAppHakemusOids getHakuAppHakemusOids(
			final ApplicationResource applicationResource) {
		final List<String> ACTIVE_AND_INCOMPLETE = Arrays.asList("ACTIVE",
				"INCOMPLETE");
		return new HakuAppHakemusOids() {
			public Collection<String> getHakemusOids(String hakuOid,
					String hakukohdeOid) throws Exception {
				HakemusList hakemusList = applicationResource.findApplications(
						null, ACTIVE_AND_INCOMPLETE, null, null, hakuOid,
						hakukohdeOid, 0, Integer.MAX_VALUE);
				if (hakemusList == null || hakemusList.getResults() == null) {
					LOG.error("Ei hakemuksia hakukohteelle {}", hakukohdeOid);
					return Collections.emptyList();
				}
				return Collections2.transform(hakemusList.getResults(),
						new Function<SuppeaHakemus, String>() {
							@Override
							public String apply(SuppeaHakemus input) {
								return input.getOid();
							}
						});
			}
		};
	}

	@Bean
	public HakuAppHakemus getHakuAppHakemus(
			final ApplicationResource applicationResource) {
		return new HakuAppHakemus() {
			public Hakemus getHakemus(String hakemusOid) throws Exception {
				return applicationResource.getApplicationByOid(hakemusOid);
			}
		};
	}

}
