package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import com.google.common.collect.*;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.PistetietoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.OsoiteBuilder;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;

import static java.util.Optional.ofNullable;
import static junit.framework.Assert.assertEquals;

@Ignore
public class HyvaksymiskirjeetKomponenttiTest {
    @Test
    public void testLaskennanKanssaPisteetTulevatMukaan() {
        LetterBatch batch = mkTestLetterBatch(1, 2);
        assertEquals(batchResult("1 / 2", "Pisteet{nimi='Jonon nimi', oma='1', minimi='2'}"), batch.toString());
    }

    @Test
    public void testIlmanLaskentaaPisteitaEiOtetaMukaan() {
        // Ilman laskentaa saadut pisteet indikoivat negatiivisina jonosijoja
        LetterBatch batch = mkTestLetterBatch(-1, -2);
        assertEquals(batchResult("-1 / -2", ""), batch.toString());
    }

    @Test
    public void testNollaPistettaJonossaEiOtetaMukaan() {
        LetterBatch batch = mkTestLetterBatch(0, -2);
        assertEquals(batchResult("0 / -2", ""), batch.toString());
    }

    @Test
    public void testKorkeakoulunMassaAjoMukaanIPostiin1() {
        LetterBatch batch = mkTestLetterBatch(0, -2, false, false, true);
        assertEquals(batchResult("0 / -2", "", false, true), batch.toString());
    }

    @Test
    public void testKorkeakoulunMassaAjoMukaanIPostiin2() {
        LetterBatch batch = mkTestLetterBatch(0, -2, true, false, true);
        assertEquals(batchResult("0 / -2", "", false, true), batch.toString());
    }

    @Test
    public void testKorkeakoulunMassaAjoEiMukaanIPostiin() {
        LetterBatch batch = mkTestLetterBatch(0, -2, true, true, true);
        assertEquals(batchResult("0 / -2", "", true, true), batch.toString());
    }

    private String batchResult(String omatPisteet, String pisteObjekti) {
        return batchResult(omatPisteet, pisteObjekti, true, false);
    }

    private String batchResult(String omatPisteet, String pisteObjekti, boolean skipIPosti, boolean skipDokumenttipalvelu) {
        return "LetterBatch [letters=[Letter [addressLabel=null, null null, templateReplacements={palautusPvm=palautusPvm, " +
                "tulokset=[{oppilaitoksenNimi=, hyvaksytyt=1 / 1 , omatPisteet=" + omatPisteet + " , pisteet=[" + pisteObjekti + "], hylkayksenSyy=, " +
                "organisaationNimi=Hakukohteella hakukohdeOid ei ole tarjojannimeä, " +
                "sijoitukset=[Sijoitus{nimi='Jonon nimi', oma='1', hyvaksytyt='-'}], alinHyvaksyttyPistemaara=, " +
                "hakukohteenNimi=Hakukohteella hakukohdeOid ei ole hakukohteennimeä, " +
                "kaikkiHakeneet=, hylkaysperuste=, " +
                "paasyJaSoveltuvuuskoe=3,99, valinnanTulos=Hyväksytty}], palautusAika=palautusAika, koulu=Hakukohteella " +
                "hakukohdeOid ei ole tarjojannimeä, henkilotunnus=, tarjoaja=Hakukohteella tarjoajaOid ei ole " +
                "tarjojannimeä, koulutus=Hakukohteella hakukohdeOid ei ole hakukohteennimeä, hakukohde=Hakukohteella " +
                "hakukohdeOid ei ole hakukohteennimeä, hakijapalveluidenOsoite=Addressline Addressline2 Addressline3, " +
                "Postal code City, hakemusOid=hakemusOid}, personOid=hakijaOid, skipIPosti=" + skipIPosti + "]], template=, templateId=, templateReplacements={sisalto=sisalto}, " +
                "templateName=templateName, languageCode=FI, storingOid=null, organizationOid=tarjoajaOid, " +
                "applicationPeriod=hakuOid, fetchTarget=hakukohdeOid, tag=tag, skipDokumenttipalvelu=" + skipDokumenttipalvelu + "]";
    }

    private LetterBatch mkTestLetterBatch(int omatPisteet, int minimipisteet) {
        return mkTestLetterBatch(omatPisteet, minimipisteet, false, false, false);
    }

    private LetterBatch mkTestLetterBatch(int omatPisteet, int minimipisteet, boolean sahkoposti, boolean sahkoinenAsiointi, boolean korkeakouluMassapostitus) {
        final String HAKUKOHDE_OID = "hakukohdeOid";
        final String HAKEMUS_OID = "hakemusOid";
        final String VALINTATAPAJONO_OID = "valintatapajonoId";
        final String TARJOAJA_OID = "tarjoajaOid";

        HakijaDTO hakija = new HakijaDTO() {{
            setEtunimi("Etunimi");
            setSukunimi("Etunimi");
            setHakemusOid(HAKEMUS_OID);
            setHakijaOid("hakijaOid");
            HakutoiveDTO h = new HakutoiveDTO() {{
                setHakukohdeOid(HAKUKOHDE_OID);
                setHakutoive(1);
                setPistetiedot(ImmutableList.of());
                setTarjoajaOid(TARJOAJA_OID);
                setKaikkiJonotSijoiteltu(true);
                HakutoiveenValintatapajonoDTO j = new HakutoiveenValintatapajonoDTO() {{
                    setValintatapajonoOid(VALINTATAPAJONO_OID);
                    setTila(HakemuksenTila.HYVAKSYTTY);
                    setHyvaksytty(1);
                    setHakeneet(1);
                    setPisteet(new BigDecimal(omatPisteet));
                    setAlinHyvaksyttyPistemaara(new BigDecimal(minimipisteet));
                    setValintatapajonoNimi("Jonon nimi");
                }};
                setHakutoiveenValintatapajonot(Lists.newArrayList(j));
                setPistetiedot(ImmutableList.of(
                        new PistetietoDTO() {{ setArvo("3,99"); }},
                        new PistetietoDTO() {{ setArvo("ei-numero"); }}
                ));
            }};
            setHakutoiveet(Sets.newTreeSet(ImmutableList.of(h)));
        }};

        Answers answers = new Answers() {{
            if(!sahkoposti) {
                setHenkilotiedot(ImmutableMap.of(
                        "Postinumero", "00000",
                        "lahiosoite", "Lähiosoite 1",
                        "asuinmaa", "Suomi"));
            } else {
                setHenkilotiedot(ImmutableMap.of(
                        "Postinumero", "00000",
                        "lahiosoite", "Lähiosoite 1",
                        "asuinmaa", "Suomi",
                        "Sähköposti", "testi@testi.fi"));
            }
            setLisatiedot(ImmutableMap.of("lupatiedot-sahkoinen-viestinta", "" + sahkoinenAsiointi));
        }};

        Osoite osoite = new OsoiteBuilder()
                .setFirstName("Etunimi")
                .setLastName("Sukunimi")
                .setAddressline("Addressline")
                .setAddressline2("Addressline2")
                .setAddressline3("Addressline3")
                .setPostalCode("Postal code")
                .setCity("City")
                .setRegion("Region")
                .setCountry("Country")
                .setCountryCode("CountryCode")
                .createOsoite();

        return HyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
                (String x) -> new HashMap<>(),
                ImmutableMap.of(TARJOAJA_OID, ofNullable(osoite)),
                ImmutableMap.of(HAKUKOHDE_OID, new MetaHakukohde(TARJOAJA_OID, new Teksti(), new Teksti())),
                ImmutableList.of(hakija),
                ImmutableList.of(new Hakemus("type", "applicationSystemId", answers, ImmutableMap.of(),
                                             ImmutableList.of(), HAKEMUS_OID, "state", "personOid")),
                null,
                "hakuOid",
                ofNullable(KieliUtil.SUOMI),
                "sisalto",
                "tag",
                "templateName",
                "palautusPvm",
                "palautusAika",
                false,
                korkeakouluMassapostitus
        );
    }
}
