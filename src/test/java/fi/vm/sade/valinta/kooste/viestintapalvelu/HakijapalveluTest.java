package fi.vm.sade.valinta.kooste.viestintapalvelu;

import com.google.common.collect.ImmutableMap;
import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HakijapalveluTest {

    @Test
    public void testParsingForFinnishFromFinnish() throws Exception {
        HakutoimistoDTO hakutoimisto = hakutoimistoFi();
        assertCorrectAddress(Hakijapalvelu.osoite(hakutoimisto, KieliUtil.SUOMI), "Hakutoimisto", "Testitie 1", "Helsinki", "http://www.foo.fi");
    }

    @Test
    public void testParsingForEnglishFromFiAndEn() throws Exception {
        HakutoimistoDTO hakutoimisto = hakutoimistoFiAndEn();
        assertCorrectAddress(Hakijapalvelu.osoite(hakutoimisto, KieliUtil.ENGLANTI), "Admission services", "Testitie 1, 00100, Helsinki, Finland", StringUtils.EMPTY, "http://www.foo.fi");
    }


    @Test
    public void testParsingForEnglishFromFinnish() throws Exception {
        HakutoimistoDTO hakutoimisto = hakutoimistoFi();
        assertCorrectAddress(Hakijapalvelu.osoite(hakutoimisto, KieliUtil.ENGLANTI), "Hakutoimisto", "Testitie 1", "Helsinki", "http://www.foo.fi");
    }

    @Test
    public void testFallbackToFinnishWhenParsingForEnglishWithNoAddress() throws Exception {
        HakutoimistoDTO hakutoimisto = hakutoimistoFiAndEnWithNoAddress();
        assertCorrectAddress(Hakijapalvelu.osoite(hakutoimisto, KieliUtil.ENGLANTI), "Admission services", "Testitie 1", "Helsinki", "http://www.foo.com");
    }

    private void assertCorrectAddress(Optional<Osoite> osoiteOpt,
                                      String nimi, String katuosoite, String postitoimipaikka, String www) {
        assertTrue(osoiteOpt.isPresent());
        Osoite osoite = osoiteOpt.get();
        assertEquals(nimi, osoite.getOrganisaationimi());
        assertEquals(katuosoite, osoite.getAddressline());
        assertEquals(postitoimipaikka, osoite.getCity());
        assertEquals(www, osoite.getWww());
    }

    private HakutoimistoDTO hakutoimistoFi() {
        return new HakutoimistoDTO(
                ImmutableMap.of("kieli_fi#1", "Hakutoimisto"),
                ImmutableMap.of("kieli_fi#1", yhteystiedot("Testitie 1", false))
        );
    }

    private HakutoimistoDTO hakutoimistoFiAndEn() {
        return new HakutoimistoDTO(
                ImmutableMap.of("kieli_fi#1", "Hakutoimisto", "kieli_en#1", "Admission services"),
                ImmutableMap.of(
                        "kieli_fi#1", yhteystiedot("Testitie 1", false),
                        "kieli_en#1", yhteystiedot("Testitie 1, 00100, Helsinki, Finland", true))
        );
    }

    private HakutoimistoDTO hakutoimistoFiAndEnWithNoAddress() {
        return new HakutoimistoDTO(
                ImmutableMap.of("kieli_fi#1", "Hakutoimisto", "kieli_en#1", "Admission services"),
                ImmutableMap.of(
                        "kieli_fi#1", yhteystiedot("Testitie 1", false),
                        "kieli_en#1", new HakutoimistoDTO.HakutoimistonYhteystiedotDTO(
                                null, null, "http://www.foo.com", "bar@foo.com", "01000700"))
        );
    }

    private HakutoimistoDTO.HakutoimistonYhteystiedotDTO yhteystiedot(String katuosoite, boolean foreign) {
        return new HakutoimistoDTO.HakutoimistonYhteystiedotDTO(osoite(katuosoite, foreign), osoite(katuosoite, foreign), "http://www.foo.fi", "bar@foo.fi", "01000700");
    }

    private HakutoimistoDTO.OsoiteDTO osoite(String osoite, boolean foreign) {
        return new HakutoimistoDTO.OsoiteDTO("1.2.3", osoite, foreign ? null : "posti_00100", foreign ? null : "HELSINKI");
    }
}
