package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.koodisto.service.types.common.KieliType;
import fi.vm.sade.koodisto.service.types.common.KoodiMetadataType;
import fi.vm.sade.koodisto.service.types.common.KoodiType;
import fi.vm.sade.koodisto.util.KoodiServiceSearchCriteriaBuilder;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.Yhteystieto;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.NimiPaattelyStrategy;
import fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Maakoodi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.OsoiteBuilder;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Component
public class HaeOsoiteKomponentti {
    private static final Logger LOG = LoggerFactory.getLogger(HaeOsoiteKomponentti.class);
    private static final String SUOMI = "fin";
    private final Cache<String, Maakoodi> koodiCache = CacheBuilder.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).build();
    private KoodiService koodiService;

    @Autowired
    public HaeOsoiteKomponentti(KoodiService koodiService) {
        this.koodiService = koodiService;
    }

    public Osoite haeOsoiteYhteystiedoista(Yhteystieto yhteystiedot, final KieliType preferoitutyyppi, String organisaationimi, String email, String numero) {
        Maakoodi maakoodi;
        // onko ulkomaalainen?
        // hae koodistosta maa
        String postCode = yhteystiedot.getPostinumeroUri();
        final String uri = postCode;
        try {
            maakoodi = koodiCache.get(uri, new Callable<Maakoodi>() {
                @Override
                public Maakoodi call() throws Exception {
                    String postitoimipaikka = StringUtils.EMPTY;
                    List<KoodiType> koodiTypes = koodiService.searchKoodis(KoodiServiceSearchCriteriaBuilder.latestKoodisByUris(uri));
                    for (KoodiType koodi : koodiTypes) {
                        if (koodi.getMetadata() == null) {
                            LOG.error("Koodistosta palautuu tyhjiä koodeja! Koodisto uri {}", uri);
                            continue;
                        }
                        // preferoidaan englantia
                        postitoimipaikka = getKuvaus(koodi.getMetadata(), preferoitutyyppi);
                        if (postitoimipaikka == null) {
                            postitoimipaikka = getNimi(koodi.getMetadata());
                            // jos suomea ei loydy kaikki kay
                        }
                        LOG.debug("Haettiin postitoimipaikka {} urille {}", new Object[]{postitoimipaikka, uri});
                        if (postitoimipaikka != null) {
                            break;
                        }
                    }
                    return new Maakoodi(postitoimipaikka, "FI");
                }
            });
        } catch (Exception e) {
            LOG.error("Yhteystiedoille ei saatu haettua maata koodistosta! Koodisto URI {}", uri);
            maakoodi = new Maakoodi(StringUtils.EMPTY, "FI");
        }
        String country = null;
        if (KieliType.EN.equals(preferoitutyyppi)) {
            country = "FINLAND";
        }
        return new OsoiteBuilder()
                .setAddressline(yhteystiedot.getOsoite())
                .setPostalCode(postinumero(yhteystiedot.getPostinumeroUri()))
                .setCity(StringUtils.capitalize(StringUtils.lowerCase(maakoodi.getPostitoimipaikka())))
                .setCountry(country)
                .setOrganisaationimi(organisaationimi)
                .setNumero(numero)
                .setEmail(email)
                .createOsoite();
    }

    private String postinumero(String url) {
        if (url != null) {
            String[] o = url.split("_");
            if (o.length > 0) {
                return o[1];
            }
        }
        return StringUtils.EMPTY;
    }

    private static String getNimi(List<KoodiMetadataType> meta) {
        for (KoodiMetadataType data : meta) {
            return data.getNimi();
        }
        return null;
    }

    private static String getKuvaus(List<KoodiMetadataType> meta, KieliType kieli) {
        for (KoodiMetadataType data : meta) {
            if (kieli.equals(data.getKieli())) {
                return data.getKuvaus();
            }
        }
        return null;
    }
}
