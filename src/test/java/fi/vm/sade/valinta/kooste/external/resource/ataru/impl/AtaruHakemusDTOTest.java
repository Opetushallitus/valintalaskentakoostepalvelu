package fi.vm.sade.valinta.kooste.external.resource.ataru.impl;

import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakukohdeDTO;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AtaruHakemusDTOTest {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Test
    @Ignore // Tätä testiä on tarkoitus ajaa ainoastaan paikallisesti kehittäjän koneelta
    public void testCreatingHakemusDTOFromAtaruHakemusWrapperWithRealData() throws ExecutionException, InterruptedException {
        final String hakemusOid = "1.2.246.562.11.00000000000000196362";
        final String hakukohdeOid = "1.2.246.562.20.755368276710";
        final String hakuOid = "1.2.246.562.29.70000333388";

        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("/spring/application-context.xml");
        AtaruAsyncResource ataruResource = applicationContext.getBean(AtaruAsyncResource.class);
        List<HakemusWrapper> hakemusWrappers = ataruResource
                .getApplicationsByOids(Collections.singletonList(hakemusOid))
                .get();
        HakemusWrapper hakemusWrapper = hakemusWrappers.get(0);
        HakemusDTO hakemusDto = hakemusWrapper.toHakemusDto(new Valintapisteet(), Collections.emptyMap());
        assertEquals(hakemusOid, hakemusDto.getHakemusoid());
        assertAvainArvo(hakemusDto.getAvaimet(), "preference1-Koulutus-id", "1.2.246.562.20.755368276710");
        assertAvainArvo(hakemusDto.getAvaimet(), "preference1-Koulutus-id-eligibility", "NOT_CHECKED");
        assertAvainArvo(hakemusDto.getAvaimet(), "preference1-Koulutus-id-processing", "UNPROCESSED");
        assertAvainArvo(hakemusDto.getAvaimet(), "preference1-Koulutus-id-paymentObligation", "UNREVIEWED");
        assertAvainArvo(hakemusDto.getAvaimet(), "preference1-Koulutus-id-languageRequirement", "UNREVIEWED");

        List<HakukohdeDTO> hakukohdeDtos = hakemusDto.getHakukohteet();
        assertEquals(1, hakukohdeDtos.size());

        HakukohdeDTO hakukohdeDto = hakukohdeDtos.get(0);
        assertEquals(0, hakukohdeDto.getHakijaryhma().size());
        assertNull(hakukohdeDto.getHakukohdeRyhmatOids());
        assertEquals(hakuOid, hakukohdeDto.getHakuoid());
        assertEquals(hakukohdeOid, hakukohdeDto.getOid());
        assertEquals(1, hakukohdeDto.getPrioriteetti());
        assertNull(hakukohdeDto.getTarjoajaoid());
        assertEquals(0, hakukohdeDto.getValinnanvaihe().size());
    }

    private void assertAvainArvo(List<AvainArvoDTO> avaimet, String expectedAvain, String expectedArvo) {
        assertTrue(
                "HakemusDTO did not have avain " + expectedAvain + " with arvo " + expectedArvo,
                avaimet
                        .stream()
                        .anyMatch(avainArvoDto -> avainArvoDto.getAvain().equals(expectedAvain) && avainArvoDto.getArvo().equals(expectedArvo))
        );
    }
}
