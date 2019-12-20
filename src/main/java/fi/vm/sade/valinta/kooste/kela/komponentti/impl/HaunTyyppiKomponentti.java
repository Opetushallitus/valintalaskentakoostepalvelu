package fi.vm.sade.valinta.kooste.kela.komponentti.impl;

import java.lang.reflect.Type;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import fi.vm.sade.properties.OphProperties;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.cxf.helpers.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.koodisto.service.types.SearchKoodisCriteriaType;
import fi.vm.sade.koodisto.service.types.common.KoodiType;
import fi.vm.sade.valinta.kooste.exception.KoodistoException;
import fi.vm.sade.valinta.kooste.util.TarjontaUriToKoodistoUtil;



/**
 *         Use proxy instead of calling bean:hakukohdeTarjonnaltaKomponentti!
 *         Proxy provides retries!
 */
@Component
public class HaunTyyppiKomponentti {
    private static final Logger LOG = LoggerFactory.getLogger(HaunTyyppiKomponentti.class);
    private final Gson GSON = new GsonBuilder().create();
    private static final Type LIST_ITEM_TYPE = new TypeToken<List<Map<String,Object>>>() {}.getType();
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
        Integer koodiVersio = TarjontaUriToKoodistoUtil.stripVersion(haunTyyppiUri);
        SearchKoodisCriteriaType koodistoHaku = TarjontaUriToKoodistoUtil.toSearchCriteria(koodiUri, koodiVersio);

        return getKoodiForUri(haunTyyppiUri, koodiUri, koodiVersio, koodistoHaku);
    }

    public String haunKohdejoukko(String haunKohdejoukkoUri) {
        LOG.error("Tehdään koodistokutsu tuntemattomalle haunKohdejoukkoUri:lle {}", haunKohdejoukkoUri);
        String koodiUri = TarjontaUriToKoodistoUtil.cleanUri(haunKohdejoukkoUri);
        Integer koodiVersio = TarjontaUriToKoodistoUtil.stripVersion(haunKohdejoukkoUri);
        SearchKoodisCriteriaType koodistoHaku = TarjontaUriToKoodistoUtil.toSearchCriteria(koodiUri, koodiVersio);
        return getKoodiForUri(haunKohdejoukkoUri, koodiUri, koodiVersio, koodistoHaku);
    }

    private String getKoodiForUri(String haunKohdejoukkoUri, String koodiUri, Integer koodiVersio, SearchKoodisCriteriaType koodistoHaku) {
        try {
            return this.client.getJson(
                    this.urlConfiguration.url("koodisto-service.koodiuri", koodiUri),
                    Duration.ofMinutes(1),
                    LIST_ITEM_TYPE
            ).thenApplyAsync(
                    response -> {
                        LOG.error("got response: " + response.toString());
                        List<Map<String,Object>> json = GSON.fromJson(response.toString(), LIST_ITEM_TYPE);
                        Map<String, Object> kobject = json.iterator().next();
                        LOG.error("JSON PARSED, GET VALUE: " + kobject.get("koodiArvo").toString());
                        return kobject.get("koodiArvo").toString();
                    }
            ).get();
        } catch (Exception e) {
            LOG.error("Unable to fetch 'koodiuri' {} from koodisto!", koodiUri, e);
            throw new RuntimeException(e);
        }
    }

/*
    private String getKoodiForUris(String haunKohdejoukkoUri, String koodiUri, Integer koodiVersio, SearchKoodisCriteriaType koodistoHaku) {
        String koodistoJson = null;
        try {
            koodistoJson = IOUtils.toString(new AutoCloseInputStream(new URL(CONFIG.url("koodisto-service.koodiuri", koodiUri)).openStream()));
            List<Map<String,Object>> json = GSON.fromJson(koodistoJson, LIST_ITEM_TYPE);
            Map<String, Object> kobject = json.iterator().next();
            return kobject.get("koodiArvo").toString();
        } catch (Exception e) {
            LOG.error("Unable to fetch 'koodiuri' {} from koodisto!", koodiUri, e);
            throw new RuntimeException(e);
        }

    }
    */
}
