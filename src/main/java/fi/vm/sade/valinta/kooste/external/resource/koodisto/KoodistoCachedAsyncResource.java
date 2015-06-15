package fi.vm.sade.valinta.kooste.external.resource.koodisto;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Futures;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class KoodistoCachedAsyncResource {
    public static final String MAAT_JA_VALTIOT_1 = "maatjavaltiot1";
    public static final String POSTI = "posti";
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
        Map<String, Koodi> koodisto =
                koodistoCache.getIfPresent(koodistoUri);
        if (koodisto == null) {
            try {
                return koodistoCache.get(koodistoUri, () -> {
                    try {
                        List<Koodi> koodit = koodistoAsyncResource.haeKoodisto(koodistoUri).get();
                        Map<String, Koodi> koodistoMappaus = konversio(koodit);
                        return koodistoMappaus;
                    } catch (Exception e) {
                        LOG.error("Koodistosta luku epäonnistui: {} {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                LOG.error("Koodistosta luku epäonnistui:", e);
                throw new RuntimeException(e);
            }
        } else {
            return koodisto;
        }
    }

    private Map<String, Koodi> konversio(List<Koodi> koodit) {
        return koodit.stream().collect(Collectors.toMap(a -> a.getKoodiArvo(), a -> a,
                // Mergefunktiossa suuremmalla versiolla oleva palautetaan
                (a, b) -> {
                    if (a.getVersio() > b.getVersio()) {
                        return a;
                    }
                    return b;
                }));
    }

    public static String haeKoodistaArvo(Koodi koodi, final String preferoituKieli, String defaultArvo) {
        if (koodi == null || koodi.getMetadata() == null) { // || koodi.getMetadata().isEmpty()
            return defaultArvo;
        } else {
            return Stream.of(
                    // Nimi halutulla kielellä
                    koodi.getMetadata().stream().filter(m -> preferoituKieli.equals(m.getKieli())),
                    // tai suomenkielellä
                    koodi.getMetadata().stream().filter(m -> KieliUtil.SUOMI.equals(m.getKieli())),
                    // tai millä vaan kielellä
                    koodi.getMetadata().stream()).flatMap(a -> a).findFirst().map(m -> m.getNimi())
                    // tai tyhjä merkkijono
                    .orElse(defaultArvo);
        }
    }
}
