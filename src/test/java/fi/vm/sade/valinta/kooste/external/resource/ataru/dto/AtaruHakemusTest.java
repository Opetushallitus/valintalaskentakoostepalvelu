package fi.vm.sade.valinta.kooste.external.resource.ataru.dto;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.Charset;

import static org.junit.Assert.*;

public class AtaruHakemusTest {
    private final Gson GSON = Converters.registerOffsetDateTime(new GsonBuilder()).create();

    @Test
    public void testCreatingAtaruHakemusFromJson() throws Exception {
        AtaruHakemus ataruHakemus = GSON.<AtaruHakemus>fromJson(
                IOUtils.toString(
                        new ClassPathResource("ataru/postValintalaskentaResponse.json").getInputStream(),
                        Charset.defaultCharset()
                ),
                new TypeToken<AtaruHakemus>() {
                }.getType()
        );
        assertEquals("1.2.246.562.11.00000000000000196362", ataruHakemus.getHakemusOid());

        AtaruHakutoive ataruHakutoive = ataruHakemus.getHakutoiveet().get(0);
        assertEquals("1.2.246.562.20.755368276710", ataruHakutoive.getHakukohdeOid());
        assertEquals("eligible", ataruHakutoive.getEligibilityState());
        assertEquals("unreviewed", ataruHakutoive.getPaymentObligation());
        assertEquals("unprocessed", ataruHakutoive.getProcessingState());
        assertEquals("unfulfilled", ataruHakutoive.getLanguageRequirement());
        assertEquals("fulfilled", ataruHakutoive.getDegreeRequirement());
    }
}
