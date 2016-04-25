package fi.vm.sade.valinta.kooste.external.resource.koodisto;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Futures;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Metadata;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class KoodistoCachedAsyncResource {
    public static final String MAAT_JA_VALTIOT_1 = "maatjavaltiot1";
    public static final String MAAT_JA_VALTIOT_2 = "maatjavaltiot2";
    public static final String POSTI = "posti";
    public static final String KIELI = "kieli";
    public static final String KUNTA = "kunta";

    private final Logger LOG = LoggerFactory.getLogger(KoodistoCachedAsyncResource.class);
    private final KoodistoAsyncResource koodistoAsyncResource;
    private final Cache<String, Map<String, Koodi>> koodistoCache = CacheBuilder.newBuilder().expireAfterAccess(7, TimeUnit.HOURS).build();

    @Autowired
    public KoodistoCachedAsyncResource(KoodistoAsyncResource koodistoAsyncResource) {
        this.koodistoAsyncResource = koodistoAsyncResource;
    }

    public Peruutettava haeKoodisto(String koodistoUri, Consumer<Map<String, Koodi>> callback, Consumer<Throwable> failureCallback) {
        try {
            Map<String, Koodi> koodisto = koodistoCache.getIfPresent(koodistoUri);
            if (koodisto != null) {
                callback.accept(koodisto);
                return new PeruutettavaImpl(Futures.immediateFuture(koodisto));
            } else {
                return koodistoAsyncResource.haeKoodisto(koodistoUri, uusiKoodisto -> {
                    Map<String, Koodi> konversio = konversio(uusiKoodisto);
                    koodistoCache.put(koodistoUri, konversio);
                    callback.accept(konversio);
                }, failureCallback);
            }
        } catch (Throwable t) {
            failureCallback.accept(t);
            return new PeruutettavaImpl(Futures.immediateFailedFuture(t));
        }
    }

    public Map<String, Koodi> haeKoodisto(String koodistoUri) {
        Map<String, Koodi> koodisto = koodistoCache.getIfPresent(koodistoUri);
        return koodisto != null ? koodisto : loadKoodistoToCache(koodistoUri);
    }

    private Map<String, Koodi> loadKoodistoToCache(String koodistoUri) {
        try {
            return koodistoCache.get(koodistoUri, () -> konversio(koodistoAsyncResource.haeKoodisto(koodistoUri).get()));
        } catch (Exception e) {
            LOG.error("Koodistosta luku epäonnistui:", e);
            throw new RuntimeException(e);
        }
    }

    private Map<String, Koodi> konversio(List<Koodi> koodit) {
        return koodit.stream().collect(
                Collectors.toMap(Koodi::getKoodiArvo, a -> a, (a, b) -> a.getVersio() > b.getVersio() ? a : b)
        );
    }

    public static String haeKoodistaArvo(Koodi koodi, final String preferoituKieli, String defaultArvo) {
        if (koodi == null || koodi.getMetadata() == null) {
            return defaultArvo;
        } else {
            return Stream.of(nameInPreferredLanguage(koodi, preferoituKieli), nameInFinnish(koodi), nameInAnyLanguage(koodi))
                    .flatMap(a -> a)
                    .findFirst()
                    .map(m -> m.getNimi())
                    .orElse(defaultArvo);
        }
    }

    private static Stream<Metadata> nameInFinnish(Koodi koodi) {
        return nameInPreferredLanguage(koodi, KieliUtil.SUOMI);
    }

    private static Stream<Metadata> nameInPreferredLanguage(Koodi koodi, String preferoituKieli) {
        return nameInAnyLanguage(koodi).filter(m -> preferoituKieli.equals(m.getKieli()));
    }

    private static Stream<Metadata> nameInAnyLanguage(Koodi koodi) {
        return koodi.getMetadata().stream();
    }
}
