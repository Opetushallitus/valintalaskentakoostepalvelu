package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.converter.AsyncProcessorTypeConverter;
import org.apache.camel.util.AsyncProcessorConverterHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import fi.vm.sade.valinta.kooste.KoostepalveluRouteBuilder;
import fi.vm.sade.valinta.kooste.Reititys;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.Sijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.JatkuvaSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SuoritaSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.resource.SijoitteluResource;
import fi.vm.sade.valinta.kooste.sijoittelu.route.SijoittelunValvonta;
import static fi.vm.sade.valinta.kooste.sijoittelu.route.SijoitteluAktivointiRoute.*;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaAloitus;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaHaku;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Varoitus;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.AbstractDokumenttiRouteBuilder;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class SijoitteluRouteImpl extends KoostepalveluRouteBuilder<Sijoittelu>
		implements SijoittelunValvonta {

	private static final Logger LOG = LoggerFactory
			.getLogger(SijoitteluRouteImpl.class);
	private final String DEADLETTERCHANNEL = "direct:sijoittelun_deadletterchannel";
	private final SijoitteluResource sijoitteluResource;

	@Autowired
	public SijoitteluRouteImpl(SijoitteluResource sijoitteluResource) {
		this.sijoitteluResource = sijoitteluResource;
	}

	@Override
	protected Cache<String, Sijoittelu> configureCache() {
		return CacheBuilder.newBuilder().expireAfterWrite(60, TimeUnit.MINUTES)
				.removalListener(new RemovalListener<String, Sijoittelu>() {
					public void onRemoval(
							RemovalNotification<String, Sijoittelu> notification) {
						LOG.info("{} siivottu pois muistista",
								notification.getValue());
					}
				}).build();
	}

	@Override
	public Sijoittelu haeAktiivinenSijoitteluHaulle(String hakuOid) {
		return getKoostepalveluCache().getIfPresent(hakuOid);
	}

	@Override
	public void configure() throws Exception {
		interceptFrom(SIJOITTELU_REITTI).process(
				Reititys.<Sijoittelu> kuluttaja(l -> {
					Sijoittelu vanhaSijoittelu = getKoostepalveluCache()
							.getIfPresent(l.getHakuOid());
					if (vanhaSijoittelu != null
							&& vanhaSijoittelu.isTekeillaan()) {
						// varmistetaan etta uudelleen ajon reunatapauksessa
						// mahdollisesti viela suorituksessa oleva vanha
						// laskenta
						// lakkaa kayttamasta resursseja ja siivoutuu ajallaan
						// pois
						throw new RuntimeException("Sijoittelu haulle "
								+ l.getHakuOid() + " on jo kaynnissa!");
					}
					getKoostepalveluCache().put(l.getHakuOid(), l);
				}));

		from(DEADLETTERCHANNEL)
		//
				.routeId("Sijoittelun deadletterchannel")
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						LOG.error(
								"Sijoittelu paattyi virheeseen {}\r\n{}",
								simple("${exception.message}").evaluate(
										exchange, String.class),
								simple("${exception.stacktrace}").evaluate(
										exchange, String.class));
					}
				})
				//
				.stop();

		from(SIJOITTELU_REITTI)
		//
				.errorHandler(deadLetterChannel())
				//
				.routeId("Sijoittelureitti")
				//
				.threads()
				//
				.process(
						Reititys.<Sijoittelu> kuluttaja(
								(s -> {
									LOG.error(
											"Aloitetaan sijoittelu haulle {}",
											s.getHakuOid());
									sijoitteluResource.sijoittele(s
											.getHakuOid());
									s.setValmis();
								}),
								((s, e) -> {
									LOG.error(
											"Sijoittelu epaonnistui haulle {}. {}\r\n{}",
											s.getHakuOid(), e.getMessage(),
											e.getStackTrace());
									s.setOhitettu();
									return false;
								})));
	}

	@Override
	protected String deadLetterChannelEndpoint() {
		return DEADLETTERCHANNEL;
	}
}
