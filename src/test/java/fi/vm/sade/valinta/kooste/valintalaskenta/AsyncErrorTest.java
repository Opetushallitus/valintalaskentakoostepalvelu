package fi.vm.sade.valinta.kooste.valintalaskenta;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.impl.ApplicationAsyncResourceImpl;

/**
 * 
 * @author Jussi Jartamo
 * 
 * @Ignore lokaaliin testaukseen. CAS virheen todentamiseen.
 */
@Ignore
public class AsyncErrorTest {

	@Test
	public void v() throws InterruptedException {
		ApplicationContext context = null;
		ApplicationAsyncResourceImpl a = new ApplicationAsyncResourceImpl(
				"https://test-virkailija.oph.ware.fi/cas",
				//
				"https://test-virkailija.oph.ware.fi/haku-app/j_spring_cas_security_check",
				//
				"valintalaskentakoostepalvelu",
				//
				"joku virheellinen salasana",
				//
				"https://test-virkailija.oph.ware.fi/haku-app",
				context);
		String hakuOid = "1.2.246.562.5.2013080813081926341927";
		String hakukohdeOid = "1.2.246.562.5.25812040993";

		a.getApplicationAdditionalData(hakuOid, hakukohdeOid, callback -> {

		}, failureCallback -> {

		});
		Thread.sleep(5000L);
	}
}
