package fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.language.Simple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluajoResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.Hakukohde;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.Tasasijasaanto;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.Valintatapajono;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.ValintatapajonoTila;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.export.ExcelExportUtil;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Komponentti luo sijoittelun tulokset excel tiedostoksi!
 */
@Component("sijoittelunTulosXlsKomponentti")
public class SijoittelunTulosExcelKomponentti {

    @Autowired
    private SijoitteluajoResource sijoitteluajoResource;

    public InputStream luoXls(@Simple("${property.sijoitteluajoId}") Long sijoitteluajoId,
            @Simple("${property.hakukohdeOid}") String hakukohdeOid) {
        Hakukohde hakukohde = sijoitteluajoResource.getHakukohdeBySijoitteluajo(sijoitteluajoId, hakukohdeOid);
        List<Object[]> rivit = new ArrayList<Object[]>();
        for (Valintatapajono jono : hakukohde.getValintatapajonot()) {

            rivit.add(new Object[] { "Valintatapajono", jono.getOid() });
            // rivit.add(new Object[] { "", "tila", suomennaTila(jono.getTila())
            // });
            // rivit.add(new Object[] { "", "varasijatäyttö",
            // suomennaTaytto(jono.getEiVarasijatayttoa()) });
            // rivit.add(new Object[] { "", "tasasijasääntö",
            // suomennaTasasijasaanto(jono.getTasasijasaanto()) });

            rivit.add(new Object[] { "Jonosija", "Tasasijan jonosija", "Hakija", "Hakemus", "Hakutoive",
                    "Sijoittelun tila", "Vastaanottotieto" });
            for (Hakemus hakemus : jono.getHakemukset()) {
                // Jonosija Tasasijan jonosija Hakija Hakemus Hakutoive
                // Sijoittelun tila Vastaanottotieto
                StringBuilder nimi = new StringBuilder();
                nimi.append(hakemus.getSukunimi()).append(", ").append(hakemus.getEtunimi());
                rivit.add(new Object[] { hakemus.getJonosija(), hakemus.getTasasijaJonosija(), nimi.toString(),
                        hakemus.getHakemusOid(), hakemus.getPrioriteetti(), hakemus.getTila(), "--" });
            }
            rivit.add(new Object[] {});
        }
        return ExcelExportUtil.exportGridAsXls(rivit.toArray(new Object[][] {}));
    }

    private static String suomennaTila(ValintatapajonoTila tila) {
        if (tila == null) {
            return "";
        }
        return tila.toString();
    }

    private static String suomennaTasasijasaanto(Tasasijasaanto saanto) {
        if (saanto == null) {
            return "";
        }
        return saanto.toString();
    }

    private static String suomennaTaytto(boolean arvo) {
        if (arvo) {
            return "ei täyttöä";
        } else {
            return "on";
        }
    }
}
