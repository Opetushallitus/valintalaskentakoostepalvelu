package fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.camel.Body;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.HakemuksenTila;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.SijoitteluajoDTO;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.ValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.util.Formatter;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.export.ExcelExportUtil;

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
    private SijoitteluResource sijoitteluajoResource;

    public InputStream luoXls(@Body String hakuOid) {
        Map<String, List<HakukohdeDTO>> hakemusHakukohde = filterHyvaksymattomatHakijatJaHakukohteet(sijoitteluajoResource
                .getSijoitteluajo(hakuOid, SijoitteluResource.LATEST));
        List<Object[]> rivit = new ArrayList<Object[]>();
        for (Entry<String, List<HakukohdeDTO>> jalkiohjattava : hakemusHakukohde.entrySet()) {
            String hakemusOid = jalkiohjattava.getKey();
            boolean otsikko = true;
            for (HakukohdeDTO hakukohde : jalkiohjattava.getValue()) {
                for (ValintatapajonoDTO jono : hakukohde.getValintatapajonot()) {
                    HakemusDTO hakemus = filterHakemus(hakemusOid, jono.getHakemukset());

                    if (otsikko) { // tehdaan otsikko
                        StringBuilder nimi = new StringBuilder();
                        nimi.append(hakemus.getSukunimi()).append(", ").append(hakemus.getEtunimi());
                        rivit.add(new Object[] { "Hakija", nimi.toString() });
                        otsikko = false;
                    }
                    rivit.add(new Object[] { "Hakukohde", hakukohde.getOid(), "Pisteet",
                            Formatter.suomennaNumero(hakemus.getPisteet()), "Valinnan tulos", hakemus.getTila() });
                }
            }
        }
        return ExcelExportUtil.exportGridAsXls(rivit.toArray(new Object[][] {}));
    }

    private static HakemusDTO filterHakemus(String hakemusOid, List<HakemusDTO> hakemukset) {
        for (HakemusDTO hakemus : hakemukset) {
            if (hakemusOid.equals(hakemus.getHakemusOid())) {
                return hakemus;
            }
        }
        return null;
    }

    private static Map<String, List<HakukohdeDTO>> filterHyvaksymattomatHakijatJaHakukohteet(SijoitteluajoDTO ajo) {
        //
        // Johonkin kohteeseen hyvaksyttyjen joukko
        //
        Set<String> hyvaksytyt = new HashSet<String>();
        //
        // Hyvaksymattomat! -- eli jalkiohjattavat!
        //
        Map<String, List<HakukohdeDTO>> jalkiohjattavat = new HashMap<String, List<HakukohdeDTO>>();
        for (HakukohdeDTO hakukohde : ajo.getHakukohteet()) {
            for (ValintatapajonoDTO jono : hakukohde.getValintatapajonot()) {
                for (HakemusDTO hakemus : jono.getHakemukset()) {
                    String hakemusOid = hakemus.getHakemusOid();
                    if (!HakemuksenTila.HYVAKSYTTY.equals(hakemus.getTila())) {
                        //
                        // Lisataan jalkiohjattaviin jos tarve
                        //
                        if (hyvaksytyt.contains(hakemusOid)) {
                            if (jalkiohjattavat.containsKey(hakemusOid)) {
                                jalkiohjattavat.get(hakemusOid).add(hakukohde);
                            } else {
                                jalkiohjattavat.put(hakemusOid, Arrays.asList(hakukohde));
                            }
                        }
                    } else {
                        //
                        // Poistetaan jalkiohjattavista jos tarve!
                        //
                        if (hyvaksytyt.add(hakemusOid)) {
                            jalkiohjattavat.remove(hakemusOid); // <- saattaa
                                                                // olla jo
                                                                // mahdollisesti
                                                                // jalkiohjattavissa
                        }
                    }
                }
            }
        }
        return jalkiohjattavat;
    }
}
