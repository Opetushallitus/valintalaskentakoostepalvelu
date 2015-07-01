package fi.vm.sade.valinta.kooste.valintalaskentatulos;

import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.sijoittelu.tulos.dto.HakemusDTO;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.ValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.util.ExcelImportUtil;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SijoittelunTulosExcelKomponenttiTest {
    private final String HAKEMUS1 = "hakemus1";
    private final String HAKEMUS2 = "hakemus2";
    private final String HAKEMUS3 = "hakemus3";

    @Mock
    private KoodistoCachedAsyncResource koodistoResource;

    @InjectMocks
    SijoittelunTulosExcelKomponentti excelKomponentti = new SijoittelunTulosExcelKomponentti();

    @Before
    public void setup() {
        HashMap<String, Koodi> koodisto = new HashMap<>();
        koodisto.put("00100", new Koodi());
        when(koodistoResource.haeKoodisto(any())).thenReturn(koodisto);
    }

    @Test
    public void createsExcel() throws Throwable {
        HakukohdeDTO hakukohde = new HakukohdeDTO();
        ValintatapajonoDTO jono = new ValintatapajonoDTO();
        HakemusDTO hakemusDTO = new HakemusDTO();
        hakemusDTO.setHakemusOid(HAKEMUS1);
        jono.setHakemukset(Arrays.asList(hakemusDTO));
        hakukohde.setValintatapajonot(Arrays.asList(createValintatapajonot("jono1", Arrays.asList(HAKEMUS1))));
        Answers answers = new Answers();
        HashMap<String, String> henkilotiedot = new HashMap<>();
        henkilotiedot.put("Kutsumanimi", "Pekka");
        henkilotiedot.put("Etunimet", "Pekka Johannes");
        henkilotiedot.put("Sukunimi", "Alamäki");
        answers.setHenkilotiedot(henkilotiedot);
        Hakemus hakemus = new Hakemus("", "", answers, new HashMap<>(), new ArrayList<>(), HAKEMUS1, "", "");
        InputStream inputStream = excelKomponentti.luoXls(new ArrayList<>(), "FI", "Konetekniikka", "Aalto yliopisto", "hakukohde1", Arrays.asList(hakemus), hakukohde);
        Collection<Rivi> rivit = ExcelImportUtil.importHSSFExcel(inputStream);
        assertTrue(cellTexts(rivit, HAKEMUS1).contains("Alamäki, Pekka"));
    }

    @Test
    public void addsEachHakemusOnlyOnce() throws Throwable {
        HakukohdeDTO hakukohde = new HakukohdeDTO();
        ValintatapajonoDTO jono1 = createValintatapajonot("jono1", Arrays.asList(HAKEMUS1, HAKEMUS2));
        ValintatapajonoDTO jono2 = createValintatapajonot("jono2", Arrays.asList(HAKEMUS1, HAKEMUS2, HAKEMUS3));
        Hakemus hakemus1 = new Hakemus("", "", new Answers(), new HashMap<>(), new ArrayList<>(), HAKEMUS1, "", "");
        Hakemus hakemus2 = new Hakemus("", "", new Answers(), new HashMap<>(), new ArrayList<>(), HAKEMUS2, "", "");
        Hakemus hakemus3 = new Hakemus("", "", new Answers(), new HashMap<>(), new ArrayList<>(), HAKEMUS3, "", "");
        hakukohde.setValintatapajonot(Arrays.asList(jono1, jono2));
        InputStream inputStream = excelKomponentti.luoXls(new ArrayList<>(), "FI", "Konetekniikka", "Aalto yliopisto", "hakukohde1", Arrays.asList(hakemus1, hakemus2, hakemus3), hakukohde);
        Collection<Rivi> rivit = ExcelImportUtil.importHSSFExcel(inputStream);
        assertEquals(1, rowsContaining(HAKEMUS1, rivit).size());
        assertEquals(1, rowsContaining(HAKEMUS2, rivit).size());
        assertEquals(1, rowsContaining(HAKEMUS3, rivit).size());
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
}
