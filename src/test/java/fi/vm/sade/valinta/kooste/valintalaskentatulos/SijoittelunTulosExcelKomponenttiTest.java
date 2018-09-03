package fi.vm.sade.valinta.kooste.valintalaskentatulos;

import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.*;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.excel.Solu;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.util.ExcelImportUtil;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakuappHakemusWrapper;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.SijoittelunTulosExcelKomponentti;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SijoittelunTulosExcelKomponenttiTest {
    private final String HAKEMUS1 = "hakemus1";
    private final String HAKEMUS2 = "hakemus2";
    private final String HAKEMUS3 = "hakemus3";
    private final String HAKIJARYHMA1 = "hakijaryhmä1";
    private final String HAKIJARYHMA2 = "hakijaryhmä2";

    @Mock
    private KoodistoCachedAsyncResource koodistoResource;

    @InjectMocks
    SijoittelunTulosExcelKomponentti excelKomponentti = new SijoittelunTulosExcelKomponentti();

    @Before
    public void setup() {
        when(koodistoResource.haeKoodisto(any())).thenReturn(new HashMap<>());
    }

    @Test
    public void addsNameFromAnswers() throws Throwable {
        HakukohdeDTO hakukohde = new HakukohdeDTO();
        ValintatapajonoDTO jono = new ValintatapajonoDTO();
        HakemusDTO hakemusDTO = new HakemusDTO();
        hakemusDTO.setHakemusOid(HAKEMUS1);
        jono.setHakemukset(Collections.singletonList(hakemusDTO));
        hakukohde.setValintatapajonot(Collections.singletonList(createValintatapajonot("jono1", Collections.singletonList(HAKEMUS1))));
        Answers answers = new Answers();
        HashMap<String, String> henkilotiedot = new HashMap<>();
        henkilotiedot.put("Kutsumanimi", "Pekka");
        henkilotiedot.put("Etunimet", "Pekka Johannes");
        henkilotiedot.put("Sukunimi", "Alamäki");
        answers.setHenkilotiedot(henkilotiedot);
        Hakemus hakemus = new Hakemus("", "", answers, new HashMap<>(), new ArrayList<>(), HAKEMUS1, "", "");
        HakemusWrapper wrapper = new HakuappHakemusWrapper(hakemus);
        InputStream inputStream = excelKomponentti.luoXls(new ArrayList<>(), "FI", "Konetekniikka", "Aalto yliopisto", "hakukohde1", Collections.singletonList(wrapper), emptyList(), hakukohde, getKkHaku(), emptyList());
        Collection<Rivi> rivit = ExcelImportUtil.importHSSFExcel(inputStream);
        assertTrue(cellTexts(rivit, HAKEMUS1).contains("Alamäki, Pekka Johannes"));
    }

    @Test
    public void addsEachHakemusExactlyOnce() throws Throwable {
        HakukohdeDTO hakukohde = new HakukohdeDTO();
        ValintatapajonoDTO jono1 = createValintatapajonot("jono1", asList(HAKEMUS1, HAKEMUS2));
        ValintatapajonoDTO jono2 = createValintatapajonot("jono2", asList(HAKEMUS2, HAKEMUS3));
        hakukohde.setValintatapajonot(asList(jono1, jono2));
        InputStream inputStream = excelKomponentti.luoXls(
                new ArrayList<>(),
                "FI",
                "Konetekniikka",
                "Aalto yliopisto",
                "hakukohde1",
                asList(createHakemusWrapper(HAKEMUS1), createHakemusWrapper(HAKEMUS2), createHakemusWrapper(HAKEMUS3)),
                emptyList(),
                hakukohde,
                getKkHaku(),
                emptyList()
        );
        Collection<Rivi> rivit = ExcelImportUtil.importHSSFExcel(inputStream);
        assertEquals(1, rowsContaining(HAKEMUS1, rivit).size());
        assertEquals(1, rowsContaining(HAKEMUS2, rivit).size());
        assertEquals(1, rowsContaining(HAKEMUS3, rivit).size());
    }

    @Test
    public void addsCellsForEachHakemusAndEachJono() throws Throwable {
        HakukohdeDTO hakukohde = new HakukohdeDTO();
        ValintatapajonoDTO jono1 = createValintatapajonot("jono1", Collections.singletonList(HAKEMUS1));
        ValintatapajonoDTO jono2 = createValintatapajonot("jono2", asList(HAKEMUS1, HAKEMUS3));
        ValintatapajonoDTO jono3 = createValintatapajonot("jono3", asList(HAKEMUS1, HAKEMUS2));
        hakukohde.setValintatapajonot(asList(jono1, jono2, jono3));
        InputStream inputStream = excelKomponentti.luoXls(
                new ArrayList<>(),
                "FI",
                "Konetekniikka",
                "Aalto yliopisto",
                "hakukohde1",
                asList(createHakemusWrapper(HAKEMUS1), createHakemusWrapper(HAKEMUS2), createHakemusWrapper(HAKEMUS3)),
                emptyList(),
                hakukohde,
                getKkHaku(),
                emptyList()
        );
        Collection<Rivi> rivit = ExcelImportUtil.importHSSFExcel(inputStream);
        assertEquals(60, solut(HAKEMUS1, rivit).size());
        assertEquals(48, solut(HAKEMUS2, rivit).size());
        assertEquals(48, solut(HAKEMUS3, rivit).size());
    }

    @Test
    public void addsAsiointikieliFromAnswers() throws Throwable {
        HakukohdeDTO hakukohde = new HakukohdeDTO();
        ValintatapajonoDTO jono = new ValintatapajonoDTO();
        HakemusDTO hakemusDTO = new HakemusDTO();
        hakemusDTO.setHakemusOid(HAKEMUS1);
        jono.setHakemukset(Collections.singletonList(hakemusDTO));
        hakukohde.setValintatapajonot(Collections.singletonList(createValintatapajonot("jono1", Collections.singletonList(HAKEMUS1))));
        Answers answers = new Answers();
        HashMap<String, String> lisatiedot = new HashMap<>();
        lisatiedot.put("asiointikieli", "SV");
        answers.setLisatiedot(lisatiedot);
        Hakemus hakemus = new Hakemus("", "", answers, new HashMap<>(), new ArrayList<>(), HAKEMUS1, "", "");
        HakemusWrapper wrapper = new HakuappHakemusWrapper(hakemus);
        InputStream inputStream = excelKomponentti.luoXls(new ArrayList<>(), "FI", "Konetekniikka", "Aalto yliopisto", "hakukohde1", Collections.singletonList(wrapper),emptyList(), hakukohde, getKkHaku(), emptyList());

        Collection<Rivi> rivit = ExcelImportUtil.importHSSFExcel(inputStream);
        assertTrue("Cell texts should contain text 'SV'", cellTexts(rivit, HAKEMUS1).contains("SV"));
    }

    @Test
    public void addEhdollinenValinta() throws Throwable {
        HakukohdeDTO hakukohde = new HakukohdeDTO();
        hakukohde.setValintatapajonot(Collections.singletonList(createValintatapajonot("jono1", Collections.singletonList(HAKEMUS1))));

        HakemusDTO hakemusDTO = new HakemusDTO();
        hakemusDTO.setHakemusOid(HAKEMUS1);

        ValintatapajonoDTO jono = new ValintatapajonoDTO();
        jono.setHakemukset(Collections.singletonList(hakemusDTO));

        HashMap<String, String> lisatiedot = new HashMap<>();
        lisatiedot.put("asiointikieli", "SV");

        Answers answers = new Answers();
        answers.setLisatiedot(lisatiedot);

        Hakemus hakemus = new Hakemus("", "", answers, new HashMap<>(), new ArrayList<>(), HAKEMUS1, "", "");
        HakemusWrapper wrapper = new HakuappHakemusWrapper(hakemus);

        Valintatulos valintatulos = new Valintatulos();
        valintatulos.setEhdollisestiHyvaksyttavissa(true, "", "");
        valintatulos.setEhdollisenHyvaksymisenEhtoKoodi("JOKU_EHTOKOODI", "", "");
        valintatulos.setEhdollisenHyvaksymisenEhtoFI("ehto suomi", "", "");
        valintatulos.setEhdollisenHyvaksymisenEhtoSV("ehto ruotsi", "", "");
        valintatulos.setEhdollisenHyvaksymisenEhtoEN("ehto englanti", "", "");
        valintatulos.setTila(ValintatuloksenTila.KESKEN, "", "");
        valintatulos.setValintatapajonoOid("jono1", "", "");
        valintatulos.setHakemusOid(HAKEMUS1, "", "");

        InputStream inputStream = excelKomponentti.luoXls(Collections.singletonList(valintatulos), "FI", "Konetekniikka", "Aalto yliopisto", "hakukohde1", Collections.singletonList(wrapper),emptyList(), hakukohde, getKkHaku(), emptyList());

        Collection<Rivi> rivit = ExcelImportUtil.importHSSFExcel(inputStream);
        assertTrue("Cell texts should contain text 'JOKU_EHTOKOODI'", cellTexts(rivit, HAKEMUS1).contains("JOKU_EHTOKOODI"));
        assertTrue("Cell texts should contain text 'ehto suomi'", cellTexts(rivit, HAKEMUS1).contains("ehto suomi"));
        assertTrue("Cell texts should contain text 'ehto ruotsi'", cellTexts(rivit, HAKEMUS1).contains("ehto ruotsi"));
        assertTrue("Cell texts should contain text 'ehto englanti'", cellTexts(rivit, HAKEMUS1).contains("ehto englanti"));
    }



    @Test
    public void defaultAsiointikieliWhenNoAnswer() throws Throwable {
        HakukohdeDTO hakukohde = new HakukohdeDTO();
        ValintatapajonoDTO jono = new ValintatapajonoDTO();
        HakemusDTO hakemusDTO = new HakemusDTO();
        hakemusDTO.setHakemusOid(HAKEMUS1);
        jono.setHakemukset(Collections.singletonList(hakemusDTO));
        hakukohde.setValintatapajonot(Collections.singletonList(createValintatapajonot("jono1", Collections.singletonList(HAKEMUS1))));
        Answers answers = new Answers();
        HashMap<String, String> lisatiedot = new HashMap<>();
        answers.setLisatiedot(lisatiedot);
        Hakemus hakemus = new Hakemus("", "", answers, new HashMap<>(), new ArrayList<>(), HAKEMUS1, "", "");
        HakemusWrapper wrapper = new HakuappHakemusWrapper(hakemus);
        InputStream inputStream = excelKomponentti.luoXls(new ArrayList<>(), "FI", "Konetekniikka", "Aalto yliopisto", "hakukohde1", Collections.singletonList(wrapper),emptyList(), hakukohde, getKkHaku(), emptyList());

        Collection<Rivi> rivit = ExcelImportUtil.importHSSFExcel(inputStream);
        assertTrue("Cell texts should contain text 'FI'", cellTexts(rivit, HAKEMUS1).contains("FI"));
    }

    @Test
    public void addsLupaSahkoiseenAsiointiinFromAnswers() throws Throwable {
        HakukohdeDTO hakukohde = new HakukohdeDTO();
        ValintatapajonoDTO jono = new ValintatapajonoDTO();
        HakemusDTO hakemusDTO = new HakemusDTO();
        hakemusDTO.setHakemusOid(HAKEMUS1);
        jono.setHakemukset(Collections.singletonList(hakemusDTO));
        hakukohde.setValintatapajonot(Collections.singletonList(createValintatapajonot("jono1", Collections.singletonList(HAKEMUS1))));
        Answers answers = new Answers();
        HashMap<String, String> lisatiedot = new HashMap<>();
        lisatiedot.put("lupatiedot-sahkoinen-viestinta", "true");
        answers.setLisatiedot(lisatiedot);
        Hakemus hakemus = new Hakemus("", "", answers, new HashMap<>(), new ArrayList<>(), HAKEMUS1, "", "");
        HakemusWrapper wrapper = new HakuappHakemusWrapper(hakemus);
        InputStream inputStream = excelKomponentti.luoXls(new ArrayList<>(), "FI", "Konetekniikka", "Aalto yliopisto", "hakukohde1", Collections.singletonList(wrapper),emptyList(), hakukohde, getKkHaku(), emptyList());

        Collection<Rivi> rivit = ExcelImportUtil.importHSSFExcel(inputStream);
        assertTrue("Cell texts should contain text 'Lupa sähköiseen asiointiin'",
                cellTexts(rivit, HAKEMUS1).contains("Lupa sähköiseen asiointiin"));
    }

    @Test
    public void noLupaSahkoiseenAsiointiinFromAnswers() throws Throwable {
        HakukohdeDTO hakukohde = new HakukohdeDTO();
        ValintatapajonoDTO jono = new ValintatapajonoDTO();
        HakemusDTO hakemusDTO = new HakemusDTO();
        hakemusDTO.setHakemusOid(HAKEMUS1);
        jono.setHakemukset(Collections.singletonList(hakemusDTO));
        hakukohde.setValintatapajonot(Collections.singletonList(createValintatapajonot("jono1", Collections.singletonList(HAKEMUS1))));
        Answers answers = new Answers();
        HashMap<String, String> lisatiedot = new HashMap<>();
        answers.setLisatiedot(lisatiedot);
        Hakemus hakemus = new Hakemus("", "", answers, new HashMap<>(), new ArrayList<>(), HAKEMUS1, "", "");
        HakemusWrapper wrapper = new HakuappHakemusWrapper(hakemus);
        InputStream inputStream = excelKomponentti.luoXls(new ArrayList<>(), "FI", "Konetekniikka", "Aalto yliopisto", "hakukohde1", Collections.singletonList(wrapper), emptyList(), hakukohde, getKkHaku(), emptyList());

        Collection<Rivi> rivit = ExcelImportUtil.importHSSFExcel(inputStream);
        assertFalse("Cell texts should not contain text 'Lupa sähköiseen asiointiin'",
                cellTexts(rivit, HAKEMUS1).contains("Lupa sähköiseen asiointiin"));
    }

    @Test
    public void addsHakijaryhmaFromHakukohde() throws Throwable {

        List<HakijaryhmaDTO> hakijaryhmat = new ArrayList<>();
        HakijaryhmaDTO hakijaryhma = new HakijaryhmaDTO();
        hakijaryhma.setOid(HAKIJARYHMA1);
        hakijaryhma.getHakemusOid().add(HAKEMUS1);
        hakijaryhma.setNimi("Hakijaryhmä 1");
        hakijaryhmat.add(hakijaryhma);

        HakukohdeDTO hakukohde = new HakukohdeDTO();
        hakukohde.setHakijaryhmat(hakijaryhmat);

        ValintatapajonoDTO jono = new ValintatapajonoDTO();
        HakemusDTO hakemusDTO = new HakemusDTO();
        hakemusDTO.setHakemusOid(HAKEMUS1);
        hakemusDTO.getHyvaksyttyHakijaryhmista().add(HAKIJARYHMA1);

        jono.setHakemukset(Collections.singletonList(hakemusDTO));
        hakukohde.setValintatapajonot(Collections.singletonList(createValintatapajonot("jono1", Collections.singletonList(HAKEMUS1))));
        Answers answers = new Answers();
        Hakemus hakemus = new Hakemus("", "", answers, new HashMap<>(), new ArrayList<>(), HAKEMUS1, "", "");
        HakemusWrapper wrapper = new HakuappHakemusWrapper(hakemus);
        InputStream inputStream = excelKomponentti.luoXls(new ArrayList<>(), "FI", "Konetekniikka", "Aalto yliopisto", "hakukohde1", Collections.singletonList(wrapper), emptyList(), hakukohde, getKkHaku(), emptyList());

        Collection<Rivi> rivit = ExcelImportUtil.importHSSFExcel(inputStream);
        assertTrue("Cell texts should contain text 'Hakijaryhmä 1'", cellTexts(rivit, HAKEMUS1).contains("Hakijaryhmä 1"));
    }

    @Test
    public void multipleHakijaryhmaForHakemus() throws Throwable {

        List<HakijaryhmaDTO> hakijaryhmat = new ArrayList<>();

        HakijaryhmaDTO hakijaryhma1 = new HakijaryhmaDTO();
        hakijaryhma1.setOid(HAKIJARYHMA1);
        hakijaryhma1.getHakemusOid().add(HAKEMUS1);
        hakijaryhma1.setNimi("Hakijaryhmä 1");
        hakijaryhmat.add(hakijaryhma1);

        HakijaryhmaDTO hakijaryhma2 = new HakijaryhmaDTO();
        hakijaryhma2.setOid(HAKIJARYHMA2);
        hakijaryhma2.getHakemusOid().add(HAKEMUS1);
        hakijaryhma2.setNimi("Hakijaryhmä 2");
        hakijaryhmat.add(hakijaryhma2);

        HakukohdeDTO hakukohde = new HakukohdeDTO();
        hakukohde.setHakijaryhmat(hakijaryhmat);

        ValintatapajonoDTO jono = new ValintatapajonoDTO();
        HakemusDTO hakemusDTO = new HakemusDTO();
        hakemusDTO.setHakemusOid(HAKEMUS1);

        jono.setHakemukset(Collections.singletonList(hakemusDTO));
        hakukohde.setValintatapajonot(Collections.singletonList(createValintatapajonot("jono1", Collections.singletonList(HAKEMUS1))));
        Answers answers = new Answers();
        Hakemus hakemus = new Hakemus("", "", answers, new HashMap<>(), new ArrayList<>(), HAKEMUS1, "", "");
        HakemusWrapper wrapper = new HakuappHakemusWrapper(hakemus);

        InputStream inputStream = excelKomponentti.luoXls(new ArrayList<>(), "FI", "Konetekniikka", "Aalto yliopisto", "hakukohde1", Collections.singletonList(wrapper),emptyList(), hakukohde, getKkHaku(), emptyList());

        Collection<Rivi> rivit = ExcelImportUtil.importHSSFExcel(inputStream);
        assertTrue("Cell texts should contain text 'Hakijaryhmä 1, Hakijaryhmä 2'",
                cellTexts(rivit, HAKEMUS1).contains("Hakijaryhmä 1, Hakijaryhmä 2"));
    }

    private List<String> cellTexts(Collection<Rivi> rivit, String oid) {
        return rivit.stream()
                .filter(containsText(oid))
                .findFirst().get().getSolut().stream()
                .map(cell -> cell.toTeksti().getTeksti())
                .collect(Collectors.toList());
    }

    private Collection<Rivi> rowsContaining(String text, Collection<Rivi> rivit) {
        return rivit.stream().filter(containsText(text)).collect(Collectors.toList());
    }

    private Predicate<Rivi> containsText(String oid) {
        return rivi -> rivi.getSolut().stream().anyMatch(solu -> solu.toTeksti().getTeksti().equals(oid));
    }

    private ValintatapajonoDTO createValintatapajonot(String jonoOid, List<String> hakemusOids) {
        ValintatapajonoDTO jono = new ValintatapajonoDTO();
        jono.setOid(jonoOid);
        List<HakemusDTO> hakemusDTOs = hakemusOids.stream().map(oid -> {
            HakemusDTO hakemusDTO = new HakemusDTO();
            hakemusDTO.setHakemusOid(oid);
            hakemusDTO.setTila(HakemuksenTila.HYVAKSYTTY);
            return hakemusDTO;
        }).collect(Collectors.toList());
        jono.setHakemukset(hakemusDTOs);
        return jono;
    }

    private HakemusWrapper createHakemusWrapper(String oid) {
        Hakemus hakemus = new Hakemus("", "", new Answers(), new HashMap<>(), new ArrayList<>(), oid, "", "");
        return new HakuappHakemusWrapper(hakemus);
    }

    private Collection<Solu> solut(String hakemusOid, Collection<Rivi> rivit) {
        return rowsContaining(hakemusOid, rivit).stream().flatMap(rivi -> rivi.getSolut().stream()).collect(Collectors.toList());
    }

    private HakuV1RDTO getKkHaku() {
        HakuV1RDTO haku = new HakuV1RDTO();
        haku.setKohdejoukkoUri("haunkohdejoukko_12#1");
        return haku;
    }
 }
