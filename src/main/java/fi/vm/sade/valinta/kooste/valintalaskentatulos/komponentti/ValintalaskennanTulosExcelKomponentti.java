package fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.external.resource.laskenta.HakukohdeResource;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.dto.JarjestyskriteerituloksenTila;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.dto.JonosijaDTO;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.dto.ValinnanvaiheDTO;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.dto.ValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.export.ExcelExportUtil;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Komponentti tulosten kasaamiseen Excel-muodossa
 */
@Component("luoValintalaskennanTuloksetXlsMuodossa")
public class ValintalaskennanTulosExcelKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(ValintalaskennanTulosExcelKomponentti.class);

    @Autowired
    private HakukohdeResource hakukohdeResource;
    @Value("${valintalaskentakoostepalvelu.valintalaskenta.rest.url}")
    private String valintalaskentaResourceUrl;

    public InputStream luoXls(@Simple("${property.hakukohdeOid}") String hakukohdeOid) {
        LOG.debug("Yhteys {} HakukohdeResource.hakukohde({})",
                new Object[] { valintalaskentaResourceUrl, hakukohdeOid });
        List<ValinnanvaiheDTO> valinnanVaiheet = hakukohdeResource.hakukohde(hakukohdeOid);
        // -jonosija
        // -nimi
        // sukunimi, etunimi
        // -järjestyskriteerit[0].arvo
        // -prioriteetti
        // -tuloksen tila
        List<Object[]> rivit = new ArrayList<Object[]>();

        // Valinnanvaihe OID: 13770670692632664634655521339392
        // Päivämäärä: 08.09.2013 15:23:40
        // Valintatapajono: Varsinaisen valinnanvaiheen valintatapajono (v.)
        for (ValinnanvaiheDTO vaihe : valinnanVaiheet) {
            rivit.add(new Object[] { "Valinnanvaihe OID:", vaihe.getValinnanvaiheoid() });
            rivit.add(new Object[] { "Päivämäärä:", ExcelExportUtil.DATE_FORMAT.format(vaihe.getCreatedAt()) });
            rivit.add(new Object[] { "Valintatapajono:", vaihe.getJarjestysnumero() });
            rivit.add(new Object[] {});
            rivit.add(new Object[] { "Jonosija", "Hakija", "Yhteispisteet", "Hakutoive", "Valintatieto" });
            for (ValintatapajonoDTO jono : vaihe.getValintatapajono()) {
                for (JonosijaDTO sija : jono.getJonosijat()) {
                    StringBuilder hakija = new StringBuilder();
                    hakija.append(sija.getSukunimi()).append(", ").append(sija.getEtunimi());
                    String yhteispisteet = "--";
                    try {
                        yhteispisteet = sija.getJarjestyskriteerit().firstEntry().getValue().getArvo().toString();
                    } catch (Exception e) {
                        LOG.error("Hakemukselle {}, nimi {} ei löytynyt yhteispisteitä!",
                                new Object[] { sija.getHakemusOid(), hakija.toString() });
                    }
                    rivit.add(new Object[] { sija.getJonosija(), hakija.toString(), yhteispisteet,
                            sija.getPrioriteetti(), suomennaTila(sija.getTuloksenTila()) });
                }
            }
        }
        return ExcelExportUtil.exportGridAsXls(rivit.toArray(new Object[][] {}));
    }

    private static String suomennaTila(JarjestyskriteerituloksenTila tila) {
        if (tila == null) {
            return "--";
        } else {
            return tila.toString();
        }
    }
}
