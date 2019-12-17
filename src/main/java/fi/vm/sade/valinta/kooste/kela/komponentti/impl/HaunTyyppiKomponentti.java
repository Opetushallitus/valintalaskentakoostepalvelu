package fi.vm.sade.valinta.kooste.kela.komponentti.impl;

import java.time.Duration;
import java.util.List;

import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.util.TarjontaUriToKoodistoUtil;



/**
 *         Use proxy instead of calling bean:hakukohdeTarjonnaltaKomponentti!
 *         Proxy provides retries!
 */
@Component
public class HaunTyyppiKomponentti {
    private static final Logger LOG = LoggerFactory.getLogger(HaunTyyppiKomponentti.class);
    private final HttpClient client;
    private final UrlConfiguration urlConfiguration;

    @Autowired
    public HaunTyyppiKomponentti(@Qualifier("KoodistoHttpClient") HttpClient client) {
        this.client = client;
        this.urlConfiguration = UrlConfiguration.getInstance();
    }

    public String haunTyyppi(String haunTyyppiUri) {
        LOG.error("Tehdään koodistokutsu tuntemattomalle haunTyyppiUri:lle {}",
                haunTyyppiUri);
        String koodiUri = TarjontaUriToKoodistoUtil.cleanUri(haunTyyppiUri);
        return getKoodiForUri(koodiUri);
    }

    public String haunKohdejoukko(String haunKohdejoukkoUri) {
        LOG.error("Tehdään koodistokutsu tuntemattomalle haunKohdejoukkoUri:lle {}", haunKohdejoukkoUri);
        String koodiUri = TarjontaUriToKoodistoUtil.cleanUri(haunKohdejoukkoUri);
        return getKoodiForUri(koodiUri);
    }

    private String getKoodiForUri(String koodiUri) {
        try {
            return this.client.<List<Koodi>>getJson(
                    this.urlConfiguration.url("koodisto-service.koodiuri", koodiUri),
                    Duration.ofMinutes(1),
                    new TypeToken<List<Koodi>>() {}.getType()
            ).thenApplyAsync(
                    response -> {
                        if (response.iterator().hasNext()) {
                            return response.iterator().next().getKoodiArvo();
                        } else {
                            throw new RuntimeException("Koodisto-response was empty for koodiuri: " + koodiUri);
                        }
                    }
            ).get();
        } catch (Exception e) {
            LOG.error("Unable to fetch 'koodiuri' {} from koodisto!", koodiUri, e);
            throw new RuntimeException(e);
        }
    }
}
