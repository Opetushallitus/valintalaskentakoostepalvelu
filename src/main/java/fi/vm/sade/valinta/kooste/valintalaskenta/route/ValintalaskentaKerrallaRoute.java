package fi.vm.sade.valinta.kooste.valintalaskenta.route;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.camel.Body;
import org.apache.camel.InOnly;
import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaHaku;

/**
 * @author Jussi Jartamo.
 */
public interface ValintalaskentaKerrallaRoute {
	static final String LOPETUSEHTO = "lopetusehto";
	static final String SEDA_VALINTALASKENTA_KERRALLA = "direct:valintalaskentakerralla";

	static final String SEDA_VALINTALASKENTA_KERRALLA_HAKEMUKSET = "direct:valintalaskentakerralla_hakemukset";
	// +
	// //
	// "?purgeWhenStopping=true&waitForTaskToComplete=Never" +
	// //
	// "&concurrentConsumers=${valintalaskentakoostepalvelu.valintalaskentakerralla.threadpoolsize:5}";

	static final String SEDA_VALINTALASKENTA_KERRALLA_VALINTAPERUSTEET = "direct:valintalaskentakerralla_valintaperusteet";
	// +
	// //
	// "?purgeWhenStopping=true&waitForTaskToComplete=Never" +
	// //
	// "&concurrentConsumers=${valintalaskentakoostepalvelu.valintalaskentakerralla.threadpoolsize:5}";

	static final String SEDA_VALINTALASKENTA_KERRALLA_LASKENTA = "direct:valintalaskentakerralla_laskenta";

	// +
	// //
	// "?purgeWhenStopping=true&waitForTaskToComplete=Never" +
	// //
	// "&concurrentConsumers=${valintalaskentakoostepalvelu.valintalaskentakerralla.threadpoolsize:5}";
	static final String SEDA_VALINTALASKENTA_KERRALLA_LISATIEDOT = "direct:valintalaskentakerralla_lisatiedot";

	@InOnly
	void suoritaValintalaskentaKerralla(@Body LaskentaJaHaku laskentaJaHaku,
			@Property(LOPETUSEHTO) AtomicBoolean lopetusehto);
}
