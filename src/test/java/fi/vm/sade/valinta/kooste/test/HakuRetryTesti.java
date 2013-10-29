package fi.vm.sade.valinta.kooste.test;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusList;
import fi.vm.sade.valinta.kooste.haku.HakemusProxy;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Testaa uudelleen yrityksia, eli epavarmaa verkkoyhteytta!
 */
@Configuration
@ContextConfiguration(classes = HakuRetryTesti.class)
@ImportResource({ "classpath:test-context.xml", "classpath:META-INF/spring/context/haku-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class HakuRetryTesti {

    @Bean
    public ApplicationResource getApplicationResource() {
        ApplicationResource mock = new ApplicationResource() {
            final boolean[] fail = { true, true, false, true, false, false, true };
            volatile int counter = 0;

            public void failRandomly() {
                ++counter;
                if (fail[counter % fail.length])
                    throw new RuntimeException("satunnainen verkkovirhe!");
            }

            public Hakemus getApplicationByOid(String oid) {
                failRandomly();
                return new Hakemus();
            }

            public HakemusList findApplications(String query, List<String> state, String aoid, String lopoid,
                    String asId, String aoOid, int start, int rows) {
                failRandomly();
                return new HakemusList();
            }
        };

        return mock;
    }

    @Autowired
    HakemusProxy hakemusProxy;

    @Test
    public void hakuWithRandomChaos() {
        for (int times = 0; times < 20; ++times) {
            hakemusProxy.haeHakemus(StringUtils.EMPTY);
        }
    }
}
