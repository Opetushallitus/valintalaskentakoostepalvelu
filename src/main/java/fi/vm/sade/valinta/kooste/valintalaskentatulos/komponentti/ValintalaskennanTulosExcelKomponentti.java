package fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.HakukohdeResource;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import fi.vm.sade.valintalaskenta.domain.dto.JonosijaDTO;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.ValintatapajonoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.valinta.JarjestyskriteerituloksenTila;
import org.apache.camel.Header;
import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jussi Jartamo
 *         <p/>
 *         Komponentti tulosten kasaamiseen Excel-muodossa
 */
@Component("luoValintalaskennanTuloksetXlsMuodossa")
public class ValintalaskennanTulosExcelKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(ValintalaskennanTulosExcelKomponentti.class);

    @Autowired
    private HakukohdeResource hakukohdeResource;

    @Value("${valintalaskentakoostepalvelu.valintalaskenta.rest.url}")
    private String valintalaskentaResourceUrl;
    @Value("${valintalaskentakoostepalvelu.valintaperusteet.rest.url}")
    private String valintaperusteetUrl;

    public InputStream luoXls(@Header("tarjoajanNimi") String tarjoajanNimi,
                              @Header("hakukohteenNimi") String hakukohteenNimi,
                              @Property(OPH.HAKUKOHDEOID) String hakukohdeOid) throws Exception {
        LOG.debug("Yhteys {} HakukohdeResource.hakukohde({})", new Object[]{
                valintalaskentaResourceUrl, hakukohdeOid});

        List<ValintatietoValinnanvaiheDTO> valinnanVaiheet = valinnanVaiheetSorted(hakukohdeOid);

        // -jonosija
        // -nimi
        // sukunimi, etunimi
        // -järjestyskriteerit[0].arvo
        // -prioriteetti
        // -tuloksen tila
        List<Object[]> rivit = new ArrayList<>();

        rivit.add(new Object[]{tarjoajanNimi});
        rivit.add(new Object[]{hakukohteenNimi});
        rivit.add(new Object[]{});

        for (ValinnanvaiheDTO vaihe : valinnanVaiheet) {

            rivit.add(new Object[]{vaihe.getNimi(), "Valinnanvaiheen OID ", vaihe.getValinnanvaiheoid()});
            rivit.add(new Object[]{"Päivämäärä:", ExcelExportUtil.DATE_FORMAT.format(vaihe.getCreatedAt())});

            if (!vaihe.getValintatapajonot().isEmpty()) {
                rivit.add(new Object[]{vaihe.getValintatapajonot().get(0).getNimi()});
                rivit.add(new Object[]{"Valintatapajonon numero", vaihe.getJarjestysnumero()});
                rivit.add(new Object[]{"Jonosija", "Hakija", "Pisteet", "Hakutoive", "Valintatieto"});

                for (fi.vm.sade.valintalaskenta.domain.dto.ValintatapajonoDTO jono : vaihe.getValintatapajonot()) {

                    for (JonosijaDTO sija : jonotSorted(jono)) {
                        StringBuilder hakija = new StringBuilder();
                        hakija.append(sija.getSukunimi()).append(", ").append(sija.getEtunimi());
                        String yhteispisteet = "--";
                        try {
                            yhteispisteet = sija.getJarjestyskriteerit().first().getArvo().toString();
                        } catch (Exception e) {
                            LOG.error("Hakemukselle {}, nimi {} ei löytynyt yhteispisteitä!", new Object[]{sija.getHakemusOid(), hakija.toString()});
                        }
                        rivit.add(new Object[]{sija.getJonosija(), hakija.toString(), yhteispisteet, sija.getPrioriteetti(), suomennaTila(sija.getTuloksenTila())});
                    }
                }
                rivit.add(new Object[]{});
            }
        }
        return ExcelExportUtil.exportGridAsXls(rivit.toArray(new Object[][]{}));
    }

    private List<JonosijaDTO> jonotSorted(ValintatapajonoDTO jono) {
        return jono.getJonosijat().stream().sorted((o1, o2) -> {
            String o1sukunimi = o1.getSukunimi();
            if (o2 == null || o1sukunimi == null) {
                return 0;
            }
            int c = o1sukunimi.compareTo(o2.getSukunimi());
            if (c == 0) {
                String o1etunimi = o1.getEtunimi();
                if (o1etunimi == null) {
                    return 0;
                }
                return o1etunimi.compareTo(o2.getEtunimi());
            } else {
                return c;
            }
        }).collect(Collectors.toList());
    }

    private List<ValintatietoValinnanvaiheDTO> valinnanVaiheetSorted(@Property(OPH.HAKUKOHDEOID) String hakukohdeOid) {
        return hakukohdeResource.hakukohde(hakukohdeOid)
                .stream()
                .sorted((o1, o2) -> {
                    if (o1 == null || o2 == null || o1.getCreatedAt() == null) {
                        LOG.error("Sijoittelulta palautuu null valinnanvaiheita!");
                        return 0;
                    }
                    return -1 * o1.getCreatedAt().compareTo(o2.getCreatedAt());
                })
                .collect(Collectors.toList());
    }

    private static String suomennaTila(JarjestyskriteerituloksenTila tila) {
        if (tila == null) {
            return "--";
        } else {
            return tila.toString();
        }
    }
}
