package fi.vm.sade.valinta.kooste.valintatapajono.resource;

import fi.vm.sade.valinta.kooste.valintatapajono.ValintatapajonoTestTools;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;

import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.valintatapajono.dto.ValintatapajonoRivit;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoDataRiviListAdapter;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoExcel;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoRivi;
import fi.vm.sade.valinta.kooste.valintatapajono.service.ValintatapajonoTuontiConverter;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValintatapajonoDTO;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.util.Arrays;
import java.util.List;

import static fi.vm.sade.valinta.http.DateDeserializer.GSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Jussi Jartamo
 */
public class ValintatapajonoTest extends ValintatapajonoTestTools{
    final static Logger LOG = LoggerFactory.getLogger(ValintatapajonoTest.class);

    @Test
    public void testaaExcel() throws Exception {
        List<Hakemus> hakemukset = GSON.fromJson(
                classpathResourceAsString("/valintatapajono/json_tuonti_listfull.json"),
                new TypeToken<List<Hakemus>>() {}.getType()
        );
        List<ValintatietoValinnanvaiheDTO> valinnanvaiheet = GSON.fromJson(
                classpathResourceAsString("/valintatapajono/json_tuonti_laskenta_valinnanvaihe.json"),
                new TypeToken<List<ValintatietoValinnanvaiheDTO>>() {}.getType()
        );

        ValintatapajonoDataRiviListAdapter listaus = new ValintatapajonoDataRiviListAdapter();
        try {
            ValintatapajonoExcel valintatapajonoExcel = new ValintatapajonoExcel(
                    "1.2.246.562.29.173465377510",
                    "1.2.246.562.20.85029108298",
                    "14229501603804360431186491391519",
                    "",
                    "",
                    valinnanvaiheet,
                    hakemukset,
                    Arrays.asList(listaus)
            );
            valintatapajonoExcel.getExcel().tuoXlsx(new ClassPathResource("/valintatapajono/valintatapajono_yksi_hakija.xlsx").getInputStream());
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
        List<ValintatapajonoRivi> rivit = listaus.getRivit();
        assertEquals(1, rivit.size());
        assertRivi(new ValintatapajonoRivi("1.2.246.562.11.00000000181", "1", "Ilman Laskentaa",
                        ValintatapajonoExcel.HYVAKSYTTAVISSA, "55.45", null, null, null),
                   rivit.get(0));
    }

    @Test
    public void testaaValintatapajononTuontia() throws Exception {
        String valintatapajonoOid = "14229501603804360431186491391519";
        ValintatapajonoRivi rivi = new ValintatapajonoRivi("1.2.246.562.11.00000000181", "1", "Ilman laskentaa", "HYVAKSYTTAVISSA", "", "", "", "");
        ValintatapajonoRivit rivit = new ValintatapajonoRivit(Arrays.asList(rivi));
        LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(rivit));
        List<ValinnanVaiheJonoillaDTO> valintaperusteet = GSON.fromJson(
                classpathResourceAsString("/valintatapajono/json_tuonti_valinnanvaihe.json"),
                new TypeToken<List<ValinnanVaiheJonoillaDTO>>() {}.getType()
        );
        List<Hakemus> hakemukset = GSON.fromJson(
                classpathResourceAsString("/valintatapajono/json_tuonti_listfull.json"),
                new TypeToken<List<Hakemus>>() {}.getType()
        );
        List<ValintatietoValinnanvaiheDTO> valinnanvaihe = GSON.fromJson(
                classpathResourceAsString("/valintatapajono/json_tuonti_laskenta_valinnanvaihe.json"),
                new TypeToken<List<ValintatietoValinnanvaiheDTO>>() {}.getType()
        );

        ValinnanvaiheDTO generoitu_valinnanvaihe =
        ValintatapajonoTuontiConverter.konvertoi("hakuOid", "1.2.246.562.20.85029108298", valintatapajonoOid, valintaperusteet, hakemukset, valinnanvaihe, rivit.getRivit());

        String json = new GsonBuilder().setPrettyPrinting().create().toJson(generoitu_valinnanvaihe);
        LOG.error("{}", json);
        assertTrue(json.contains("\"arvo\": -1"));
        assertTrue(json.contains("\"tila\": \"HYVAKSYTTAVISSA\""));
        assertFalse(generoitu_valinnanvaihe.getValintatapajonot().isEmpty());
        for(ValintatietoValintatapajonoDTO jono : generoitu_valinnanvaihe.getValintatapajonot()) {
            assertEquals(jono.getOid(), valintatapajonoOid);
        }
    }

    @Test
    public void testaaValintatapajononTuontiaPisteitaJaMaarittelematonTila() throws Exception {
        String valintatapajonoOid = "14229501603804360431186491391519";
        ValintatapajonoRivi rivi = new ValintatapajonoRivi("1.2.246.562.11.00000000181", "", "Ilman laskentaa", "MAARITTELEMATON", "20", "", "", "");
        ValintatapajonoRivit rivit = new ValintatapajonoRivit(Arrays.asList(rivi));
        LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(rivit));
        List<ValinnanVaiheJonoillaDTO> valintaperusteet = GSON.fromJson(
                classpathResourceAsString("/valintatapajono/json_tuonti_valinnanvaihe.json"),
                new TypeToken<List<ValinnanVaiheJonoillaDTO>>() {}.getType()
        );
        List<Hakemus> hakemukset = GSON.fromJson(
                classpathResourceAsString("/valintatapajono/json_tuonti_listfull.json"),
                new TypeToken<List<Hakemus>>() {}.getType()
        );
        List<ValintatietoValinnanvaiheDTO> valinnanvaihe = GSON.fromJson(
                classpathResourceAsString("/valintatapajono/json_tuonti_laskenta_valinnanvaihe.json"),
                new TypeToken<List<ValintatietoValinnanvaiheDTO>>() {}.getType()
        );

        ValinnanvaiheDTO generoitu_valinnanvaihe =
                ValintatapajonoTuontiConverter.konvertoi("hakuOid", "1.2.246.562.20.85029108298", valintatapajonoOid, valintaperusteet, hakemukset, valinnanvaihe, rivit.getRivit());

        String json = new GsonBuilder().setPrettyPrinting().create().toJson(generoitu_valinnanvaihe);
        LOG.error("{}", json);
        assertTrue(json.contains("\"arvo\": 20"));
        assertTrue(json.contains("\"tila\": \"HYVAKSYTTAVISSA\""));
        assertFalse(generoitu_valinnanvaihe.getValintatapajonot().isEmpty());
        for(ValintatietoValintatapajonoDTO jono : generoitu_valinnanvaihe.getValintatapajonot()) {
            assertEquals(jono.getOid(), valintatapajonoOid);
        }
    }

    @Test
    public void testaaValintatapajononTuontiaEiPisteitaJaMaarittelematonTila() throws Exception {
        String valintatapajonoOid = "14229501603804360431186491391519";
        ValintatapajonoRivi rivi = new ValintatapajonoRivi("1.2.246.562.11.00000000181", "", "Ilman laskentaa", "MAARITTELEMATON", "", "", "", "");
        ValintatapajonoRivit rivit = new ValintatapajonoRivit(Arrays.asList(rivi));
        LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(rivit));
        List<ValinnanVaiheJonoillaDTO> valintaperusteet = GSON.fromJson(
                classpathResourceAsString("/valintatapajono/json_tuonti_valinnanvaihe.json"),
                new TypeToken<List<ValinnanVaiheJonoillaDTO>>() {}.getType()
        );
        List<Hakemus> hakemukset = GSON.fromJson(
                classpathResourceAsString("/valintatapajono/json_tuonti_listfull.json"),
                new TypeToken<List<Hakemus>>() {}.getType()
        );
        List<ValintatietoValinnanvaiheDTO> valinnanvaihe = GSON.fromJson(
                classpathResourceAsString("/valintatapajono/json_tuonti_laskenta_valinnanvaihe.json"),
                new TypeToken<List<ValintatietoValinnanvaiheDTO>>() {}.getType()
        );

        ValinnanvaiheDTO generoitu_valinnanvaihe =
                ValintatapajonoTuontiConverter.konvertoi("hakuOid", "1.2.246.562.20.85029108298", valintatapajonoOid, valintaperusteet, hakemukset, valinnanvaihe, rivit.getRivit());

        String json = new GsonBuilder().setPrettyPrinting().create().toJson(generoitu_valinnanvaihe);
        LOG.error("{}", json);
        assertFalse(json.contains("\"tila\""));
        assertFalse(generoitu_valinnanvaihe.getValintatapajonot().isEmpty());
        for(ValintatietoValintatapajonoDTO jono : generoitu_valinnanvaihe.getValintatapajonot()) {
            assertEquals(jono.getOid(), valintatapajonoOid);
        }
    }

    @Test
    public void testaaValintatapajononTuonninVirhe() throws Exception {
        String valintatapajonoOid = "14229501603804360431186491391519";
        ValintatapajonoRivi rivi = new ValintatapajonoRivi("1.2.246.562.11.00000000181", "1", "Ilman laskentaa", "HYVAKSYTTAVISSA", "20", "", "", "");
        ValintatapajonoRivit rivit = new ValintatapajonoRivit(Arrays.asList(rivi));
        LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(rivit));
        List<ValinnanVaiheJonoillaDTO> valintaperusteet = GSON.fromJson(
                classpathResourceAsString("/valintatapajono/json_tuonti_valinnanvaihe.json"),
                new TypeToken<List<ValinnanVaiheJonoillaDTO>>() {}.getType()
        );
        List<Hakemus> hakemukset = GSON.fromJson(
                classpathResourceAsString("/valintatapajono/json_tuonti_listfull.json"),
                new TypeToken<List<Hakemus>>() {}.getType()
        );
        List<ValintatietoValinnanvaiheDTO> valinnanvaihe = GSON.fromJson(
                classpathResourceAsString("/valintatapajono/json_tuonti_laskenta_valinnanvaihe.json"),
                new TypeToken<List<ValintatietoValinnanvaiheDTO>>() {}.getType()
        );

        boolean exceptionThrown = false;
        try {
            ValinnanvaiheDTO generoitu_valinnanvaihe =
                    ValintatapajonoTuontiConverter.konvertoi("hakuOid", "1.2.246.562.20.85029108298", valintatapajonoOid, valintaperusteet, hakemukset, valinnanvaihe, rivit.getRivit());
        } catch (RuntimeException re) {
            assertEquals("Samassa valintatapajonossa ei voida käyttää sekä jonosijoja että kokonaispisteitä.", re.getMessage());
            exceptionThrown = true;
        }
        assertTrue("Poikkeus puuttuu.", exceptionThrown);
    }

    private static String classpathResourceAsString(String path) throws Exception {
        return IOUtils.toString(new ClassPathResource(path).getInputStream());
    }
}
