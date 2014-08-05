package fi.vm.sade.valinta.kooste.valintalaskenta.route;

import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaMaski;

public interface ValintalaskentaKerrallaRoute {

	final String SEDA_VALINTALASKENTA_KERRALLA = "seda:valintalaskentakerralla"
			+
			//
			"?purgeWhenStopping=true&waitForTaskToComplete=Never" +
			//
			"&concurrentConsumers=${valintalaskentakoostepalvelu.valintalaskentakerralla.threadpoolsize:10}";

	final String SEDA_VALINTALASKENTA_KERRALLA_HAKEMUKSET = "seda:valintalaskentakerralla_hakemukset"
			+
			//
			"?purgeWhenStopping=true&waitForTaskToComplete=Never" +
			//
			"&concurrentConsumers=${valintalaskentakoostepalvelu.valintalaskentakerralla.threadpoolsize:10}";

	final String SEDA_VALINTALASKENTA_KERRALLA_VALINTAPERUSTEET = "seda:valintalaskentakerralla_valintaperusteet"
			+
			//
			"?purgeWhenStopping=true&waitForTaskToComplete=Never" +
			//
			"&concurrentConsumers=${valintalaskentakoostepalvelu.valintalaskentakerralla.threadpoolsize:10}";

	final String SEDA_VALINTALASKENTA_KERRALLA_LASKENTA = "seda:valintalaskentakerralla_laskenta"
			+
			//
			"?purgeWhenStopping=true&waitForTaskToComplete=Never" +
			//
			"&concurrentConsumers=${valintalaskentakoostepalvelu.valintalaskentakerralla.threadpoolsize:10}";

	void suoritaValintalaskentaKerralla(LaskentaJaMaski laskentaJaMaski);
}
