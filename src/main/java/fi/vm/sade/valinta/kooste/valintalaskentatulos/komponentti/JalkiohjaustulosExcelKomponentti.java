package fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti;

import fi.vm.sade.sijoittelu.tulos.dto.PistetietoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import org.apache.camel.Body;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Komponentti tulosten kasaamiseen Excel-muodossa
 */
@Component("jalkiohjaustulosXlsKomponentti")
public class JalkiohjaustulosExcelKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(JalkiohjaustulosExcelKomponentti.class);

    @Autowired
    private SijoitteluResource sijoitteluResource;

    public InputStream luoXls(@Body String hakuOid) {
        final List<HakijaDTO> hyvaksymattomatHakijat = sijoitteluResource.ilmankoulutuspaikkaa(hakuOid,
                SijoitteluResource.LATEST);
        List<Object[]> rivit = new ArrayList<Object[]>();
        for (HakijaDTO jalkiohjattava : hyvaksymattomatHakijat) {
            boolean otsikko = true;
            for (HakutoiveDTO hakutoive : jalkiohjattava.getHakutoiveet()) {
                for (HakutoiveenValintatapajonoDTO jono : hakutoive.getHakutoiveenValintatapajonot()) {

                    // HakemusDTO hakemus = filterHakemus(hakemusOid,
                    // jono.getHakemukset());

                    if (otsikko) { // tehdaan otsikko
                        StringBuilder nimi = new StringBuilder();
                        nimi.append(jalkiohjattava.getSukunimi()).append(", ").append(jalkiohjattava.getEtunimi());
                        rivit.add(new Object[] { "Hakija", nimi.toString() });
                        otsikko = false;
                    }
                    StringBuilder pisteet = new StringBuilder();
                    for (PistetietoDTO pistetieto : hakutoive.getPistetiedot()) {
                        pisteet.append(pistetieto.getArvo()).append(" ");
                    }
                    rivit.add(new Object[] { "Hakukohde", hakutoive.getHakukohdeOid(), "Valintatapajono",
                            jono.getValintatapajonoOid(), "Pisteet", pisteet.toString().trim(), "Valinnan tulos",
                            jono.getTila() });
                }
            }
        }
        return ExcelExportUtil.exportGridAsXls(rivit.toArray(new Object[][] {}));
    }

}
