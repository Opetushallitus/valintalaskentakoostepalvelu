package fi.vm.sade.valinta.kooste.viestintapalvelu;

import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.util.TarjontaUriToKoodistoUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.OsoiteBuilder;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.Optional;

public class Hakijapalvelu {

    public static Optional<Osoite> osoite(HakutoimistoDTO hakutoimisto, String kieli) {
        Optional<String> nimi = byKieliOrSuomi(kieli, hakutoimisto.nimi);
        Optional<HakutoimistoDTO.HakutoimistonYhteystiedotDTO> kielenYhteystieto =
                byKieliOrSuomi(kieli, hakutoimisto.yhteystiedot);
        Optional<HakutoimistoDTO.OsoiteDTO> kielenOsoite = kielenYhteystieto.flatMap(Hakijapalvelu::osoite);
        if (!kielenOsoite.isPresent()) {
            kielenOsoite = byKieli(KieliUtil.SUOMI, hakutoimisto.yhteystiedot).flatMap(Hakijapalvelu::osoite);
        }
        Optional<HakutoimistoDTO.OsoiteDTO> fOsoite = kielenOsoite;
        return kielenYhteystieto.flatMap(yhteystiedot ->
                        fOsoite.map(osoite ->
                                        new OsoiteBuilder()
                                                .setAddressline(osoite.katuosoite)
                                                .setPostalCode(postinumero(osoite.postinumero))
                                                .setCity(postitoimipaikka(osoite.postitoimipaikka))
                                                .setCountry(country(kieli))
                                                .setOrganisaationimi(nimi.orElse(""))
                                                .setNumero(yhteystiedot.puhelin)
                                                .setEmail(yhteystiedot.email)
                                                .setWww(yhteystiedot.www)
                                                .createOsoite()
                        )
        );
    }

    private static Optional<HakutoimistoDTO.OsoiteDTO> osoite(HakutoimistoDTO.HakutoimistonYhteystiedotDTO yhteystieto) {
        return Optional.ofNullable(yhteystieto.posti != null ? yhteystieto.posti : yhteystieto.kaynti);
    }

    private static String country(String kieli) {
        return kieli.equals(KieliUtil.ENGLANTI) ? "FINLAND" : null;
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

    private static <T> Optional<T> byKieli(String kieli, Map<String, T> m) {
        return m.keySet().stream()
                .filter(langUri -> normalizeLang(langUri).equals(kieli))
                .findFirst()
                .map(k -> m.get(k));
    }

    private static <T> Optional<T> byKieliOrSuomi(String kieli, Map<String, T> m) {
        Optional<T> o = byKieli(kieli, m);
        if (o.isPresent()) {
            return o;
        }
        return byKieli(KieliUtil.SUOMI, m);
    }

    private static String normalizeLang(String lang) {
        return KieliUtil.normalisoiKielikoodi(TarjontaUriToKoodistoUtil.cleanUri(lang));
    }
}
