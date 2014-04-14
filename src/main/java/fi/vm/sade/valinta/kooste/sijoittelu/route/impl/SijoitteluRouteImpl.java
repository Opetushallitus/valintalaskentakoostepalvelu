package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.JatkuvaSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluIlmankoulutuspaikkaaKomponentti;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluKoulutuspaikkallisetKomponentti;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluSuoritaKomponentti;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SuoritaSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.route.SijoitteluAktivointiRoute;
import fi.vm.sade.valinta.kooste.valintatieto.komponentti.ValintatietoKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.AbstractDokumenttiRouteBuilder;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class SijoitteluRouteImpl extends AbstractDokumenttiRouteBuilder {

	private ValintatietoKomponentti valintatietoKomponentti;
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
			ValintatietoKomponentti valintatietoKomponentti) {
		super();
		this.sijoitteluAktivoi = sijoitteluAktivoi;
		this.quartzInput = quartzInput;
		this.suoritaSijoittelu = suoritaSijoittelu;
		this.sijoitteluSuoritaKomponentti = sijoitteluSuoritaKomponentti;
		this.sijoitteluKoulutuspaikkallisetKomponentti = sijoitteluKoulutuspaikkallisetKomponentti;
		this.sijoitteluIlmankoulutuspaikkaaKomponentti = sijoitteluIlmankoulutuspaikkaaKomponentti;
		this.jatkuvaSijoittelu = jatkuvaSijoittelu;
		this.valintatietoKomponentti = valintatietoKomponentti;
	}

	@Override
	public void configure() throws Exception {
		from(quartzInput).bean(jatkuvaSijoittelu);
		// <route errorHandlerRef="sijoitteluRetryHandler">
		// <from uri="direct:sijoitteluKoulutuspaikallisetReitti"/>
		// <setProperty
		// propertyName="hakuOid"><simple>${body.args[0]}</simple></setProperty>
		// <setProperty
		// propertyName="hakukohdeOid"><simple>${body.args[1]}</simple></setProperty>
		// <setProperty
		// propertyName="sijoitteluajoId"><simple>${body.args[2]}</simple></setProperty>
		// <to uri="bean:sijoitteluKoulutuspaikkallisetKomponentti"/>
		// </route>

		from("direct:sijoitteluKoulutuspaikallisetReitti").bean(
				sijoitteluKoulutuspaikkallisetKomponentti);

		// <route errorHandlerRef="sijoitteluRetryHandler">
		// <from uri="direct:sijoitteluIlmankoulutuspaikkaaReitti"/>
		// <setProperty
		// propertyName="hakuOid"><simple>${body.args[0]}</simple></setProperty>
		// <setProperty
		// propertyName="sijoitteluajoId"><simple>${body.args[1]}</simple></setProperty>
		// <to uri="bean:sijoitteluIlmankoulutuspaikkaaKomponentti"/>
		// </route>

		from("direct:sijoitteluIlmankoulutuspaikkaaReitti").bean(
				sijoitteluIlmankoulutuspaikkaaKomponentti);

		// <route errorHandlerRef="sijoitteluRetryHandler">
		// <from uri="direct:sijoitteluSuoritaReitti"/>
		// <setProperty
		// propertyName="hakutyyppi"><simple>${body}</simple></setProperty>
		// <to uri="bean:sijoitteluSuoritaKomponentti"/>
		// </route>

		from("direct:sijoitteluSuoritaReitti")
		//
				.process(new SecurityPreprocessor())
				//
				.bean(sijoitteluSuoritaKomponentti);

		// <route>
		// <from uri="direct:kaynnistaSijoitteluReitti"/>
		// <setProperty propertyName="hakuOid">
		// <simple>${body.args[0]}</simple>
		// </setProperty>
		// <policy ref="admin">
		// <process ref="securityPreprocessor"/>
		// <to uri="bean:suoritaSijoittelu"/>
		// </policy>
		// </route>

		// LOG.info("KOOSTEPALVELU: Haetaan valintatiedot haulle {}", new
		// Object[] { hakuOid });
		from(sijoitteluAktivoi)
		//
				.process(new SecurityPreprocessor())
				//
				.process(asetaKokonaistyo(2))
				//
				.bean(valintatietoKomponentti)
				//
				.process(merkkaaTyoTehdyksi())
				//
				.bean(suoritaSijoittelu)
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
}
