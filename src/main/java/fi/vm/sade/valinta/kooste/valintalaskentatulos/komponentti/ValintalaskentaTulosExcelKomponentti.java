package fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintatiedot.ValintatietoService;
import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.export.ExcelExportUtil;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Komponentti tulosten kasaamiseen Excel-muodossa
 */
@Component("luoTuloksetXlsMuodossa")
public class ValintalaskentaTulosExcelKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(ValintalaskentaTulosExcelKomponentti.class);

    @Autowired
    private ValintatietoService valintatietoService;

    public InputStream luoTuloksetXlsMuodossa(@Simple("${property.hakukohdeOid}") String hakukohdeOid,
            @Simple("${property.valintakoeOid}") List<String> valintakoeOids,
            @Simple("${property.hakemukset}") List<HakemusTyyppi> hakemukset) {
        Map<String, String> oidToName = new HashMap<String, String>();
        for (HakemusTyyppi hakemus : hakemukset) {
            StringBuilder b = new StringBuilder();
            b.append(hakemus.getHakijanSukunimi()).append(", ").append(hakemus.getHakijanEtunimi());
            oidToName.put(hakemus.getHakemusOid(), b.toString());
        }
        List<HakemusOsallistuminenTyyppi> tiedotHakukohteelle = new ArrayList<HakemusOsallistuminenTyyppi>();
        for (String valintakoeOid : valintakoeOids) {
            tiedotHakukohteelle.addAll(valintatietoService.haeValintatiedotHakukohteelle(hakukohdeOid, valintakoeOid));
        }

        List<Object[]> rows = new ArrayList<Object[]>();
        rows.add(new Object[] { "Nimi", "Hakemus", "Osallistuminen", "Laskettu pvm" });
        for (HakemusOsallistuminenTyyppi o : tiedotHakukohteelle) {
            rows.add(new Object[] { "" + oidToName.get(o.getHakemusOid()), o.getHakemusOid(),
                    "" + o.getOsallistuminen().toString(), ExcelExportUtil.DATE_FORMAT.format(o.getLuontiPvm()) });

        }

        return ExcelExportUtil.exportGridAsXls(rows.toArray(new Object[][] {}));
    }
}
