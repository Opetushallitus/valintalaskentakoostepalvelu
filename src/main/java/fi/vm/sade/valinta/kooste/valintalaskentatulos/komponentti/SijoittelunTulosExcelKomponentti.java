package fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.language.Simple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.sijoittelu.tulos.dto.HakemusDTO;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.Tasasijasaanto;
import fi.vm.sade.sijoittelu.tulos.dto.ValintatapajonoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.ValintatapajonoTila;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Komponentti luo sijoittelun tulokset excel tiedostoksi!
 */
@Component("sijoittelunTulosXlsKomponentti")
public class SijoittelunTulosExcelKomponentti {

    @Autowired
    private SijoitteluResource sijoitteluajoResource;

    public InputStream luoXls(@Simple("${property.sijoitteluajoId}") Long sijoitteluajoId,
            @Simple("${property.hakukohdeOid}") String hakukohdeOid, @Simple("${property.hakuOid}") String hakuOid) {
        HakukohdeDTO hakukohde = sijoitteluajoResource.getHakukohdeBySijoitteluajo(hakuOid, sijoitteluajoId.toString(),
                hakukohdeOid);
        // getHakukohdeBySijoitteluajo(hakuOid, sijoitteluajoId, hakukohdeOid);
        List<Object[]> rivit = new ArrayList<Object[]>();
        for (ValintatapajonoDTO jono : hakukohde.getValintatapajonot()) {

            rivit.add(new Object[] { "Valintatapajono", jono.getOid() });
            // rivit.add(new Object[] { "", "tila", suomennaTila(jono.getTila())
            // });
            // rivit.add(new Object[] { "", "varasijatäyttö",
            // suomennaTaytto(jono.getEiVarasijatayttoa()) });
            // rivit.add(new Object[] { "", "tasasijasääntö",
            // suomennaTasasijasaanto(jono.getTasasijasaanto()) });

            rivit.add(new Object[] { "Jonosija", "Tasasijan jonosija", "Hakija", "Hakemus", "Hakutoive",
                    "Sijoittelun tila", "Vastaanottotieto" });
            for (HakemusDTO hakemus : jono.getHakemukset()) {
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
