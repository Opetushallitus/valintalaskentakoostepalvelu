package fi.vm.sade.valinta.kooste.valintalaskentatulos;

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
        hakemusDTO.setHakemusOid("hakemus979");
        jono.setHakemukset(Arrays.asList(hakemusDTO));
        hakukohde.setValintatapajonot(Arrays.asList(jono));
        Answers answers = new Answers();
        HashMap<String, String> henkilotiedot = new HashMap<>();
        henkilotiedot.put("Kutsumanimi", "Pekka");
        henkilotiedot.put("Etunimet", "Pekka Johannes");
        henkilotiedot.put("Sukunimi", "Alamäki");
        answers.setHenkilotiedot(henkilotiedot);
        Hakemus hakemus = new Hakemus("", "", answers, new HashMap<>(), new ArrayList<>(), "hakemus979", "", "");
        InputStream inputStream = excelKomponentti.luoXls(new ArrayList<>(), "FI", "Konetekniikka", "Aalto yliopisto", "hakukohde1", Arrays.asList(hakemus), hakukohde);
        Collection<Rivi> rivit = ExcelImportUtil.importHSSFExcel(inputStream);
        assertTrue(cellTexts(rivit, "hakemus979").contains("Alamäki, Pekka"));
    }

    private List<String> cellTexts(Collection<Rivi> rivit, String oid) {
        return rivit.stream()
                .filter(containsHakemus(oid))
                .findFirst().get().getSolut().stream()
                .map(cell -> cell.toTeksti().getTeksti())
                .collect(Collectors.toList());
    }

    private Predicate<Rivi> containsHakemus(String oid) {
        return rivi -> rivi.getSolut().stream().anyMatch(solu -> solu.toTeksti().getTeksti().equals(oid));
    }
}
