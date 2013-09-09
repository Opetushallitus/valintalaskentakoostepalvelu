package fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti;

import fi.vm.sade.service.valintatiedot.ValintatietoService;
import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;
import fi.vm.sade.service.valintatiedot.schema.ValintakoeOsallistuminenTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.export.ExcelExportUtil;
import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.InputStream;
import java.util.*;

/**
 * @author Jussi Jartamo
 *         <p/>
 *         Komponentti tulosten kasaamiseen Excel-muodossa
 */
@Component("luoTuloksetXlsMuodossa")
public class ValintalaskentaTulosExcelKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(ValintalaskentaTulosExcelKomponentti.class);

    @Autowired
    private ValintatietoService valintatietoService;

    public InputStream luoTuloksetXlsMuodossa(@Simple("${property.hakukohdeOid}") String hakukohdeOid,
                                              @Simple("${property.valintakoeOid}") List<String> valintakoeOids,
                                              @Simple("${property.hakemukset}") List<SuppeaHakemus> hakemukset) {
        Map<String, String> oidToName = new HashMap<String, String>();
        for (SuppeaHakemus hakemus : hakemukset) {
            StringBuilder b = new StringBuilder();
            b.append(hakemus.getLastName()).append(", ").append(hakemus.getFirstNames());
            oidToName.put(hakemus.getOid(), b.toString());
        }
        List<HakemusOsallistuminenTyyppi> tiedotHakukohteelle = valintatietoService.haeValintatiedotHakukohteelle(
                valintakoeOids, hakukohdeOid);
        List<String> tunnisteet = getTunnisteet(tiedotHakukohteelle);
        if (tunnisteet.isEmpty()) {
            return ExcelExportUtil.exportGridAsXls(new Object[][]{
                    new Object[]{"Hakukohteelle ei löytynyt tuloksia annetuilla syötteillä!"},
                    new Object[]{"Hakukohde OID", hakukohdeOid},
                    new Object[]{"Valintakoe OID:it", Arrays.toString(valintakoeOids.toArray())}});
        } else {
            List<Object[]> rows = new ArrayList<Object[]>();
            LOG.debug("Creating rows for Excel file!");
            ArrayList<String> otsikot = new ArrayList<String>();
            otsikot.addAll(Arrays.asList("Nimi", "Hakemus", "Laskettu pvm"));
            otsikot.addAll(tunnisteet);
            rows.add(otsikot.toArray());
            for (HakemusOsallistuminenTyyppi o : tiedotHakukohteelle) {
                XMLGregorianCalendar calendar = o.getLuontiPvm();
                Date date = calendar.toGregorianCalendar().getTime();
                Map<String, ValintakoeOsallistuminenTyyppi> osallistumiset = new HashMap<String, ValintakoeOsallistuminenTyyppi>();
                for (ValintakoeOsallistuminenTyyppi v : o.getOsallistumiset()) {
                    osallistumiset.put(v.getValintakoeTunniste(), v);
                }
                ArrayList<String> rivi = new ArrayList<String>();
                rivi.addAll(Arrays.asList(oidToName.get(o.getHakemusOid()), o.getHakemusOid(),
                        ExcelExportUtil.DATE_FORMAT.format(date)));
                for (String tunniste : tunnisteet) {
                    if (osallistumiset.containsKey(tunniste)) {
                        rivi.add(osallistumiset.get(tunniste).getOsallistuminen().toString());
                    } else {
                        rivi.add("----");
                    }
                }
                rows.add(rivi.toArray());
            }

            return ExcelExportUtil.exportGridAsXls(rows.toArray(new Object[][]{}));
        }
    }

    private List<String> getTunnisteet(List<HakemusOsallistuminenTyyppi> osallistujat) {
        Set<String> tunnisteet = new HashSet<String>();
        for (HakemusOsallistuminenTyyppi osallistuja : osallistujat) {
            for (ValintakoeOsallistuminenTyyppi valintakoe : osallistuja.getOsallistumiset()) {
                if (!tunnisteet.contains(valintakoe.getValintakoeTunniste())) {
                    tunnisteet.add(valintakoe.getValintakoeTunniste());
                }
            }
        }
        return new ArrayList<String>(tunnisteet);
    }
}
