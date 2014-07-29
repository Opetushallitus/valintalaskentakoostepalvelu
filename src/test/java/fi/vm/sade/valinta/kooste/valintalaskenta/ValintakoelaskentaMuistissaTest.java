package fi.vm.sade.valinta.kooste.valintalaskenta;

import java.util.concurrent.TimeUnit;

import fi.vm.sade.valinta.kooste.external.resource.laskenta.ValintalaskentaResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetRestResource;
import org.apache.camel.CamelContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakukohteenHakemuksetKomponentti;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHaunHakemuksetKomponentti;
import fi.vm.sade.valinta.kooste.valintakokeet.route.impl.ValintakoelaskentaMuistissaRouteImpl;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaTila;

@Configuration
@Import({ ValintakoelaskentaMuistissaRouteImpl.class,
		HaeHaunHakemuksetKomponentti.class,
		HaeHakukohteenHakemuksetKomponentti.class })
@ContextConfiguration(classes = { ValintakoelaskentaMuistissaTest.class,
		KoostepalveluContext.CamelConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class ValintakoelaskentaMuistissaTest {
	private final int VALINTALASKENTA_TAKES_TO_COMPLETE_AT_MOST = (int) TimeUnit.SECONDS
			.toMillis(8);
	@Autowired
	private CamelContext camelContext;
	@Autowired
	private ApplicationResource hakuAppHakemus;

    @Autowired
    private ValintaperusteetRestResource valintaperusteetRestResource;
    @Autowired
    private ValintalaskentaResource valintalaskentaResource;

	@Test
	public void testaaMaskaus() throws Exception {
		String hakuOid = "h0";
		Hakemus hak1 = new Hakemus();
		hak1.setOid("hak1");
		Hakemus hak2 = new Hakemus();
		hak2.setOid("hak2");

	}

	@Bean
	public ValintalaskentaTila getValintalaskentaTila() {
		return new ValintalaskentaTila();
	}

	@Bean
	public ApplicationResource getApplicationResource() {
		return Mockito.mock(ApplicationResource.class);
	}

    @Bean
    public ValintaperusteetRestResource getValintaperusteetRestResource() {
        return Mockito.mock(ValintaperusteetRestResource.class);
    }

    @Bean
    public ValintalaskentaResource getValintalaskentaResource() {
        return Mockito.mock(ValintalaskentaResource.class);
    }

}
