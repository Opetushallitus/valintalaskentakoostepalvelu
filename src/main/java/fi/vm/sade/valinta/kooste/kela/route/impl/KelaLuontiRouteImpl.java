package fi.vm.sade.valinta.kooste.kela.route.impl;

import static fi.vm.sade.valinta.kooste.kela.route.KelaLuontiRoute.SEDA_KELA_LUONTI;
import static org.apache.camel.ExchangePattern.InOnly;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import fi.vm.sade.tarjonta.service.resources.v1.HakukohdeV1Resource;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.KoostepalveluRouteBuilder;
import fi.vm.sade.valinta.kooste.Reititys;
import fi.vm.sade.valinta.kooste.external.resource.haku.HakuV1Resource;
import fi.vm.sade.valinta.kooste.kela.dto.Haku;
import fi.vm.sade.valinta.kooste.kela.dto.Luonti;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaHaku;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaHakukohde;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.seuranta.resource.DokumentinSeurantaResource;

public class KelaLuontiRouteImpl extends KoostepalveluRouteBuilder<Luonti> {
	private final static Logger LOG = LoggerFactory
			.getLogger(KelaLuontiRouteImpl.class);
	private final static String ROUTE_ID = "KELALUONTI";
	private final static String ROUTE_ID_DEADLETTERCHANNEL = "KELALUONTI_DEADLETTERCHANNEL";
	private final static String ROUTE_ID_HAKEMUKSET = "KELALUONTI_HAKEMUKSET";
	private final static String DEADLETTERCHANNEL = "direct:kela_luonti_route_deadletterchannel";
	private final static String HAKEMUKSET = "direct:kela_luonti_route_hakemukset";
	private final boolean ohitakoodisto;
	// fi.vm.sade.valinta.kooste.external.resource.haku.
	private final HakuV1Resource hakuResource;
	private final DokumentinSeurantaResource seurantaResource;

	@Autowired
	public KelaLuontiRouteImpl(
			@Value("${kela.luonti.ohitakoodisto:true}") boolean ohitakoodisto,
			HakuV1Resource hakuResource,
			DokumentinSeurantaResource seurantaResource) {
		this.ohitakoodisto = ohitakoodisto;
		this.hakuResource = hakuResource;
		this.seurantaResource = seurantaResource;
	}

	@Override
	public void configure() throws Exception {
		//
		interceptFrom(SEDA_KELA_LUONTI).process(
				Reititys.<Luonti> kuluttaja(l -> {
					getKoostepalveluCache().put(l.getUuid(), l);
				}));
		//
		intercept().when(simple("${property.lopetusehto?.get()}")).stop();
		from(SEDA_KELA_LUONTI)
		//
				.errorHandler(deadLetterChannel())
				//
				.routeId(ROUTE_ID)
				//
				.convertBodyTo(Luonti.class)
				//
				.threads()
				//
				.split(Reititys.<Luonti, List<Haku>> lauseke(
						luonti -> {

							return luonti
									.getHakuOids()
									.parallelStream()
									.map(oid -> hakuResource.findByOid(oid)
											.getResult())
									//
									// .map(haku -> haku
									// .getKoulutuksenAlkamiskausiUri())
									.map(haku -> new Haku(haku
											.getHakutyyppiUri(), haku
											.getKoulutuksenAlkamiskausiUri(),
											haku.getKoulutuksenAlkamisVuosi()))
									//
									.collect(Collectors.toList());

						}, (luonti, poikkeus) -> {
							LOG.error("Ei saatu hakuja tarjonnalta! Haut {}",
									Arrays.toString(luonti.getHakuOids()
											.toArray()));
							seurantaResource.keskeyta(luonti.getUuid());
						}))
				//
				.to(InOnly, HAKEMUKSET);
		from(HAKEMUKSET)
		//
				.errorHandler(deadLetterChannel())
				//
				.routeId(ROUTE_ID_HAKEMUKSET)
				//
				.split(Reititys.<Luonti, List<String>> lauseke(luonti -> {
					return Collections.emptyList();
				}))
				//
				.process(Reititys.<String> kuluttaja(oid -> {

				}));

		from(DEADLETTERCHANNEL)
		//
				.routeId(ROUTE_ID_DEADLETTERCHANNEL)
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						LOG.error(
								"Kelatiedoston luonti paattyi virheeseen\r\n{}",
								simple("${exception.message}").evaluate(
										exchange, String.class));
						exchange.getProperty(
								ValintalaskentaKerrallaRoute.LOPETUSEHTO,
								AtomicBoolean.class).set(true);
					}
				})
				//
				.stop();
	}

	@Override
	protected String deadLetterChannelEndpoint() {
		return DEADLETTERCHANNEL;
	}
}
