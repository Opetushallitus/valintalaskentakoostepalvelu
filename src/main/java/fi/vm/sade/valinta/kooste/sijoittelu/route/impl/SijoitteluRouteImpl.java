package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import fi.vm.sade.valinta.kooste.external.resource.laskenta.ValintatietoResource;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakuDTO;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.JatkuvaSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluIlmankoulutuspaikkaaKomponentti;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluKoulutuspaikkallisetKomponentti;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluSuoritaKomponentti;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SuoritaSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.route.SijoitteluAktivointiRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Varoitus;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.AbstractDokumenttiRouteBuilder;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class SijoitteluRouteImpl extends AbstractDokumenttiRouteBuilder {

	private static final Logger LOG = LoggerFactory
			.getLogger(SijoitteluRouteImpl.class);
	private ValintatietoResource valintatietoService;
	private JatkuvaSijoittelu jatkuvaSijoittelu;
	private SijoitteluIlmankoulutuspaikkaaKomponentti sijoitteluIlmankoulutuspaikkaaKomponentti;
	private SijoitteluKoulutuspaikkallisetKomponentti sijoitteluKoulutuspaikkallisetKomponentti;
	private SijoitteluSuoritaKomponentti sijoitteluSuoritaKomponentti;
	private SuoritaSijoittelu suoritaSijoittelu;
	private final String quartzInput;
	private final String sijoitteluAktivoi;

	@Autowired
	public SijoitteluRouteImpl(
			// Quartz timer endpoint configuration
			@Value("quartz://timerName?cron=${valintalaskentakoostepalvelu.jatkuvasijoittelu.cron}") String quartzInput,
			@Value(SijoitteluAktivointiRoute.SEDA_SIJOITTELU_AKTIVOI) String sijoitteluAktivoi,
			SuoritaSijoittelu suoritaSijoittelu,
			SijoitteluSuoritaKomponentti sijoitteluSuoritaKomponentti,
			SijoitteluKoulutuspaikkallisetKomponentti sijoitteluKoulutuspaikkallisetKomponentti,
			SijoitteluIlmankoulutuspaikkaaKomponentti sijoitteluIlmankoulutuspaikkaaKomponentti,
			JatkuvaSijoittelu jatkuvaSijoittelu,
			@Qualifier("ValintatietoRestClient") ValintatietoResource valintatietoService) {
		super();
		this.sijoitteluAktivoi = sijoitteluAktivoi;
		this.quartzInput = quartzInput;
		this.suoritaSijoittelu = suoritaSijoittelu;
		this.sijoitteluSuoritaKomponentti = sijoitteluSuoritaKomponentti;
		this.sijoitteluKoulutuspaikkallisetKomponentti = sijoitteluKoulutuspaikkallisetKomponentti;
		this.sijoitteluIlmankoulutuspaikkaaKomponentti = sijoitteluIlmankoulutuspaikkaaKomponentti;
		this.jatkuvaSijoittelu = jatkuvaSijoittelu;
		this.valintatietoService = valintatietoService;
	}

	@Override
	public void configure() throws Exception {
		from(quartzInput).bean(jatkuvaSijoittelu);

		from("direct:sijoitteluKoulutuspaikallisetReitti").bean(
				sijoitteluKoulutuspaikkallisetKomponentti);

		from("direct:sijoitteluIlmankoulutuspaikkaaReitti").bean(
				sijoitteluIlmankoulutuspaikkaaKomponentti);

		from("direct:sijoitteluSuoritaReitti")
		//
				.process(SecurityPreprocessor.SECURITY)
				//
				.bean(sijoitteluSuoritaKomponentti);


		// LOG.info("KOOSTEPALVELU: Haetaan valintatiedot haulle {}", new
		// Object[] { hakuOid });
		from(sijoitteluAktivoi)
		//
				.process(SecurityPreprocessor.SECURITY)
				//
				.process(asetaKokonaistyo(1))
				//

				.process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
//                        HakuTyyppi hakutyyppi = hakutyyppi(exchange);
                        String hakuOid = hakuOid(exchange);

                        try {
                            LOG.error(
                                    "Siirretään sijoitteluun valintatiedot haulle({}). Operaatio saattaa kestää pitkään!",
                                    hakuOid);
                            dokumenttiprosessi(exchange)
                                    .getVaroitukset()
                                    .add(new Varoitus(hakuOid,
                                            "Siirretään sijoitteluun valintatiedot. Operaatio saattaa kestää pitkään!"));
                            suoritaSijoittelu.sijoittele(
                                    hakuOid);
                            dokumenttiprosessi(exchange)
                                    .getVaroitukset()
                                    .add(new Varoitus(hakuOid,
                                            "Tiedot on siirretty sijoitteluun!"));
                            LOG.error(
                                    "Tiedot on siirretty sijoitteluun haulle({})!",
                                    hakuOid);
                        } catch (Exception e) {
                            e.printStackTrace();
                            LOG.error(
                                    "Sijoittelun suorittaminen epäonnistui haulle({})",
                                    hakuOid(exchange), e.getMessage());

                            dokumenttiprosessi(exchange)
                                    .getPoikkeukset()
                                    .add(new Poikkeus(
                                            Poikkeus.SIJOITTELU,
                                            "Sijoittelun suorittaminen epäonnistui.",
                                            e.getMessage(), Poikkeus
                                            .hakuOid(hakuOid(exchange))));
                            throw e;
                        }
                    }
                })
				//
				.process(merkkaaTyoTehdyksi())
				//
				.process(new Processor() {

					@Override
					public void process(Exchange exchange) throws Exception {
						dokumenttiprosessi(exchange).setDokumenttiId(
								"sijoittelu on valmis");
					}
				});

	}

	private HakuDTO hakutyyppi(Exchange exchange) {
		return exchange.getIn().getBody(HakuDTO.class);
	}
}
