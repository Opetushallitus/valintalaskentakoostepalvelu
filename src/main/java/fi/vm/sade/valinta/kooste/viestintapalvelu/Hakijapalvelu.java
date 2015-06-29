package fi.vm.sade.valinta.kooste.viestintapalvelu;

import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.util.TarjontaUriToKoodistoUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.OsoiteBuilder;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class Hakijapalvelu {

    public static Optional<Osoite> osoite(HakutoimistoDTO hakutoimisto, String hakukohteenKieli) {

        Optional<String> nimiOpt = extract(hakukohteenKieli, hakutoimisto.nimi);
        Optional<HakutoimistoDTO.HakutoimistonYhteystiedotDTO> yhteystiedotOpt = extract(hakukohteenKieli, hakutoimisto.yhteystiedot);

        return yhteystiedotOpt.flatMap(yhteystiedot -> {

            HakutoimistoDTO.OsoiteDTO osoite = yhteystiedot.posti != null ? yhteystiedot.posti : yhteystiedot.kaynti;
            return Optional.of(
                    new OsoiteBuilder()
                            .setAddressline(osoite.katuosoite)
                            .setPostalCode(postinumero(osoite.postinumero))
                            .setCity(postitoimipaikka(osoite.postitoimipaikka))
                            .setCountry(country(hakukohteenKieli))
                            .setOrganisaationimi(nimiOpt.orElse(""))
                            .setNumero(yhteystiedot.puhelin)
                            .setEmail(yhteystiedot.email)
                            .setWww(yhteystiedot.www)
                            .createOsoite()
            );
        });
    }

    private static String country(String hakukohteenKieli) {
        return hakukohteenKieli.equals(KieliUtil.ENGLANTI) ? "FINLAND" : null;
    }

    private static String postitoimipaikka(String postitoimipaikka) {
        return postitoimipaikka != null ? StringUtils.capitalize(postitoimipaikka.toLowerCase()) : StringUtils.EMPTY;
    }

    private static String postinumero(String url) {
        if (url != null) {
            String[] o = url.split("_");
            if (o.length > 0) {
                return o[1];
            }
        }
        return StringUtils.EMPTY;
    }


    private static <T> Optional<T> extract(String hakukohteenKieli, Map<String, T> map) {
        return kielikoodi(hakukohteenKieli, map.keySet()).map(map::get);
    }

    private static Optional<String> kielikoodi(String hakukohteenKieli, Set<String> providedLangs) {
        Optional<String> preferredLang = languageKeyIfExists(hakukohteenKieli, providedLangs);
        if (preferredLang.isPresent()) {
            return preferredLang;
        } else {
            Optional<String> fi = languageKeyIfExists(KieliUtil.SUOMI, providedLangs);
            return fi.isPresent() ? fi : providedLangs.stream().findFirst();
        }
    }

    private static Optional<String> languageKeyIfExists(String hakukohteenKieli, Set<String> keys) {
        return keys.stream().filter(langMatch(hakukohteenKieli)).findAny();
    }

    private static String normalizeLang(String lang) {
        return KieliUtil.normalisoiKielikoodi(TarjontaUriToKoodistoUtil.cleanUri(lang));
    }

    private static Predicate<String> langMatch(String hakukohteenKieli) {
        return lang -> normalizeLang(lang).equals(hakukohteenKieli);
    }
}
