package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.PistetietoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.util.HakuappHakemusWrapper;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.OsoiteBuilder;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.Letter;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.Sijoitus;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HyvaksymiskirjeetKomponenttiTest {
    private final String TEMPLATE_NAME = "templateName";
    private final String LANGUAGE_CODE = KieliUtil.SUOMI;
    private final String ORGANIZATION_OID = "tarjoajaOid";
    private final String APPLICATION_PERIOD = "hakuOid";
    private final String FETCH_TARGET = null;
    private final String TAG = "tag";

    private final String HAKUKOHDE_OID = "hakukohdeOid";
    private final String HAKEMUS_OID = "hakemusOid";
    private final String VALINTATAPAJONO_OID = "valintatapajonoId";
    private final String VALINTATAPAJONO_NIMI = "Jonon nimi";

    private final String OSOITE_ETUNIMI = "Etunimi";
    private final String OSOITE_SUKUNIMI = "Sukunimi";
    private final String OSOITE_ADDRESSLINE = "Addressline";
    private final String OSOITE_ADDRESSLINE2 = "Addressline2";
    private final String OSOITE_ADDRESSLINE3 = "Addressline3";
    private final String OSOITE_POSTAL_CODE = "Postal code";
    private final String OSOITE_CITY = "City";
    private final String OSOITE_REGION = "Region";
    private final String OSOITE_COUNTRY = "Country";
    private final String OSOITE_COUNTRY_CODE = "Country Code";

    private final String PALAUTUS_PVM = "palautusPvm";
    private final String PALAUTUS_AIKA = "palautusAika";

    private final String PISTE_TIEDOT1 = "3,99";
    private final String PISTE_TIEDOT2 = "ei-numero";

    private final String SISALTO = "sisalto";
    private final String HAKIJA_OID = "hakijaOid";

    @Test
    public void testLaskennanKanssaPisteetTulevatMukaan() {
        LetterBatch batch = mkTestLetterBatch(1, 2);
        assertLetterBatchCorrect(batch,"1 / 2", "2", true, false);
    }

    @Test
    public void testIlmanLaskentaaPisteitaEiOtetaMukaan() {
        // Ilman laskentaa saadut pisteet indikoivat negatiivisina jonosijoja
        LetterBatch batch = mkTestLetterBatch(-1, -2);
        assertLetterBatchCorrect(batch,"−1 / −2", null, true, false);
    }

    @Test
    public void testNollaPistettaJonossaEiOtetaMukaan() {
        LetterBatch batch = mkTestLetterBatch(0, -2);
        assertLetterBatchCorrect(batch,"0 / −2", null, true, false);
    }

    @Test
    public void testKorkeakoulunMassaAjoMukaanIPostiin1() {
        LetterBatch batch = mkTestLetterBatch(0, -2, false, false, true);
        assertLetterBatchCorrect(batch,"0 / −2", null, true, true);
    }

    @Test
    public void testKorkeakoulunMassaAjoMukaanIPostiin2() {
        LetterBatch batch = mkTestLetterBatch(0, -2, true, false, true);
        assertLetterBatchCorrect(batch,"0 / −2", null, true, true);
    }

    @Test
    public void testKorkeakoulunMassaAjoEiMukaanIPostiin() {
        LetterBatch batch = mkTestLetterBatch(0, -2, true, true, true);
        assertLetterBatchCorrect(batch, "0 / −2", null, true, true);
    }

    private void assertLetterBatchCorrect(LetterBatch batch, String omatPisteet,
                                   String minimipisteet, boolean skipIPosti, boolean skipDokumenttipalvelu) {
        assertEquals(TEMPLATE_NAME, batch.getTemplateName());
        assertEquals(LANGUAGE_CODE, batch.getLanguageCode());
        assertEquals(ORGANIZATION_OID, batch.getOrganizationOid());
        assertEquals(APPLICATION_PERIOD, batch.getApplicationPeriod());
        assertEquals("FetchTarget was null, the default is used", HAKUKOHDE_OID, batch.getFetchTarget());
        assertEquals(TAG, batch.getTag());
        assertEquals(skipDokumenttipalvelu, batch.isSkipDokumenttipalvelu());

        assertEquals(1, batch.getLetters().size());
        Letter letter = batch.getLetters().get(0);

        assertEquals("Lähiosoite 1, 00000 00000", letter.getAddressLabel().toString());
        assertNull(letter.getLanguageCode());
        assertEquals(13, letter.getTemplateReplacements().size());
        assertEquals(PALAUTUS_PVM, letter.getTemplateReplacements().get("palautusPvm"));
        assertEquals(PALAUTUS_AIKA, letter.getTemplateReplacements().get("palautusAika"));
        assertTrue(letter.getTemplateReplacements().containsKey("tulokset"));
        assertEquals("Hakukohteella " + HAKUKOHDE_OID + " ei ole tarjojannimeä", letter.getTemplateReplacements().get("koulu"));
        assertEquals("", letter.getTemplateReplacements().get("henkilotunnus"));
        assertEquals(null, letter.getTemplateReplacements().get("ohjeetUudelleOpiskelijalle"));
        assertEquals("", letter.getTemplateReplacements().get("syntymaaika"));
        assertEquals("Hakukohteella " + ORGANIZATION_OID + " ei ole tarjojannimeä", letter.getTemplateReplacements().get("tarjoaja"));
        assertEquals("Hakukohteella " + HAKUKOHDE_OID + " ei ole hakukohteennimeä", letter.getTemplateReplacements().get("koulutus"));
        assertNull(letter.getTemplateReplacements().get("hakukonde"));
        assertTrue(letter.getTemplateReplacements().containsKey("hakijapalveluidenOsoite"));
        assertEquals(HAKEMUS_OID, letter.getTemplateReplacements().get("hakemusOid"));
        assertNull(letter.getLetterContent());
        assertNull(letter.getEmailAddress());
        assertEquals(HAKIJA_OID, letter.getPersonOid());
        assertTrue("Hakija OID has to be also available to template", letter.getTemplateReplacements().containsKey("hakijaOid"));
        assertEquals(HAKIJA_OID, letter.getTemplateReplacements().get("hakijaOid"));
        assertEquals(HAKEMUS_OID, letter.getApplicationOid());
        assertEquals(skipIPosti, letter.isSkipIPosti());

        List<Map<String, Object>> tulokset = (List<Map<String, Object>>) letter.getTemplateReplacements().get("tulokset");
        assertEquals(1, tulokset.size());
        Map<String, Object> tulos = tulokset.get(0);
        assertEquals(14, tulos.size());
        assertEquals("", tulos.get("oppilaitoksenNimi"));
        assertEquals("1 / 1 ", tulos.get("hyvaksytyt"));
        assertEquals(omatPisteet + " ", tulos.get("omatPisteet"));
        assertEquals("", tulos.get("hylkayksenSyy"));
        assertEquals("Hakukohteella " + HAKUKOHDE_OID + " ei ole tarjojannimeä", tulos.get("organisaationNimi"));
        assertTrue(tulos.containsKey("sijoitukset"));
        assertEquals("", tulos.get("alinHyvaksyttyPistemaara"));
        assertEquals("Hakukohteella " + HAKUKOHDE_OID + " ei ole hakukohteennimeä", tulos.get("hakukohteenNimi"));
        assertEquals("", tulos.get("kaikkiHakeneet"));
        assertNull(tulos.get("hylkaysperuste"));
        assertEquals(PISTE_TIEDOT1, tulos.get("paasyJaSoveltuvuuskoe"));
        assertEquals("Hyväksytty", tulos.get("valinnanTulos"));
        assertNull(tulos.get("peruuntumisenSyy"));
        assertEquals(Boolean.TRUE, tulos.get("hyvaksytty"));

        List<Sijoitus> sijoitukset = (List<Sijoitus>) tulos.get("sijoitukset");
        assertEquals(1, sijoitukset.size());
        Sijoitus sijoitus = sijoitukset.get(0);

        assertEquals(VALINTATAPAJONO_NIMI, sijoitus.getNimi());
        assertEquals("1", sijoitus.getOma());
        assertNull(sijoitus.getVarasija());

        assertEquals(minimipisteet, sijoitus.getPisteet().getMinimi());
        assertNull(sijoitus.getPisteet().getEnsikertMinimi());

        Osoite hakijaPalveluidenOsoite = (Osoite) letter.getTemplateReplacements().get("hakijapalveluidenOsoite");
        assertEquals(OSOITE_ETUNIMI, hakijaPalveluidenOsoite.getFirstName());
        assertEquals(OSOITE_SUKUNIMI, hakijaPalveluidenOsoite.getLastName());
        assertEquals(OSOITE_ADDRESSLINE, hakijaPalveluidenOsoite.getAddressline());
        assertEquals(OSOITE_ADDRESSLINE2, hakijaPalveluidenOsoite.getAddressline2());
        assertEquals(OSOITE_ADDRESSLINE3, hakijaPalveluidenOsoite.getAddressline3());
        assertEquals(OSOITE_POSTAL_CODE, hakijaPalveluidenOsoite.getPostalCode());
        assertEquals(OSOITE_CITY, hakijaPalveluidenOsoite.getCity());
        assertEquals(OSOITE_REGION, hakijaPalveluidenOsoite.getRegion());
        assertEquals(OSOITE_COUNTRY, hakijaPalveluidenOsoite.getCountry());
        assertEquals(OSOITE_COUNTRY_CODE, hakijaPalveluidenOsoite.getCountryCode());
        assertNull(hakijaPalveluidenOsoite.getOrganisaationimi());
        assertNull(hakijaPalveluidenOsoite.getEmail());
        assertNull(hakijaPalveluidenOsoite.getNumero());
        assertNull(hakijaPalveluidenOsoite.getWww());

        assertEquals(1, batch.getTemplateReplacements().size());
        assertEquals(SISALTO, batch.getTemplateReplacements().get("sisalto"));

    }

    private LetterBatch mkTestLetterBatch(int omatPisteet, int minimipisteet) {
        return mkTestLetterBatch(omatPisteet, minimipisteet, false, false, false);
    }

    private LetterBatch mkTestLetterBatch(int omatPisteet, int minimipisteet, boolean sahkoposti, boolean sahkoinenAsiointi, boolean korkeakouluMassapostitus) {

        HakijaDTO hakija = new HakijaDTO() {{
            setEtunimi("Etunimi");
            setSukunimi("Etunimi");
            setHakemusOid(HAKEMUS_OID);
            setHakijaOid(HAKIJA_OID);
            HakutoiveDTO h = new HakutoiveDTO() {{
                setHakukohdeOid(HAKUKOHDE_OID);
                setHakutoive(1);
                setPistetiedot(ImmutableList.of());
                setTarjoajaOid(ORGANIZATION_OID);
                setKaikkiJonotSijoiteltu(true);
                HakutoiveenValintatapajonoDTO j = new HakutoiveenValintatapajonoDTO() {{
                    setValintatapajonoOid(VALINTATAPAJONO_OID);
                    setTila(HakemuksenTila.HYVAKSYTTY);
                    setHyvaksytty(1);
                    setHakeneet(1);
                    setPisteet(new BigDecimal(omatPisteet));
                    setAlinHyvaksyttyPistemaara(new BigDecimal(minimipisteet));
                    setValintatapajonoNimi(VALINTATAPAJONO_NIMI);
                }};
                setHakutoiveenValintatapajonot(Lists.newArrayList(j));
                setPistetiedot(ImmutableList.of(
                        new PistetietoDTO() {{ setArvo(PISTE_TIEDOT1); }},
                        new PistetietoDTO() {{ setArvo(PISTE_TIEDOT2); }}
                ));
            }};
            setHakutoiveet(Sets.newTreeSet(ImmutableList.of(h)));
        }};

        Answers answers = new Answers() {{
            if(!sahkoposti) {
                setHenkilotiedot(ImmutableMap.of(
                        "Postinumero", "00000",
                        "lahiosoite", "Lähiosoite 1",
                        "asuinmaa", "FIN"));
            } else {
                setHenkilotiedot(ImmutableMap.of(
                        "Postinumero", "00000",
                        "lahiosoite", "Lähiosoite 1",
                        "asuinmaa", "FIN",
                        "Sähköposti", "testi@testi.fi"));
            }
            setLisatiedot(ImmutableMap.of("lupatiedot-sahkoinen-viestinta", "" + sahkoinenAsiointi));
        }};

        Osoite osoite = new OsoiteBuilder()
                .setFirstName(OSOITE_ETUNIMI)
                .setLastName(OSOITE_SUKUNIMI)
                .setAddressline(OSOITE_ADDRESSLINE)
                .setAddressline2(OSOITE_ADDRESSLINE2)
                .setAddressline3(OSOITE_ADDRESSLINE3)
                .setPostalCode(OSOITE_POSTAL_CODE)
                .setCity(OSOITE_CITY)
                .setRegion(OSOITE_REGION)
                .setCountry(OSOITE_COUNTRY)
                .setCountryCode(OSOITE_COUNTRY_CODE)
                .createOsoite();

        return HyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
                (String x) -> new HashMap<>(),
                ImmutableMap.of(HAKUKOHDE_OID, ofNullable(osoite)),
                ImmutableMap.of(HAKUKOHDE_OID, new MetaHakukohde(ORGANIZATION_OID, new Teksti(), new Teksti())),
                ImmutableList.of(hakija),
                ImmutableMap.of(
                        HAKEMUS_OID,
                        new HakuappHakemusWrapper(
                                new Hakemus(
                                        "type",
                                        "applicationSystemId",
                                        answers,
                                        ImmutableMap.of(),
                                        ImmutableList.of(),
                                        HAKEMUS_OID,
                                        "state",
                                        "personOid"
                                )
                        )
                ),
                FETCH_TARGET,
                APPLICATION_PERIOD,
                ofNullable(LANGUAGE_CODE),
                SISALTO,
                TAG,
                TEMPLATE_NAME,
                PALAUTUS_PVM,
                PALAUTUS_AIKA,
                korkeakouluMassapostitus
        );
    }
}
