package fi.vm.sade.valinta.kooste.erillishaku.excel;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import fi.vm.sade.valinta.kooste.excel.arvo.MonivalintaArvo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.excel.arvo.Arvo;
import fi.vm.sade.valinta.kooste.excel.arvo.TekstiArvo;

import static fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi.emptyErillishakuRivi;

public class ErillishakuExcel {
    private final static Logger LOG = LoggerFactory.getLogger(ErillishakuExcel.class);
    private final Excel excel;

    public ErillishakuExcel(Hakutyyppi tyyppi, ErillishakuRiviKuuntelija kuuntelija) {
        this(tyyppi, "", "", "", Collections.emptyList(), kuuntelija);
    }

    public ErillishakuExcel(Hakutyyppi tyyppi, String hakuNimi, String hakukohdeNimi,
                            String tarjoajaNimi, List<ErillishakuRivi> erillishakurivit) {
        this(tyyppi, hakuNimi, hakukohdeNimi, tarjoajaNimi, erillishakurivit, rivi -> {});
    }

    public ErillishakuExcel(final Hakutyyppi tyyppi, String hakuNimi, String hakukohdeNimi,
                            String tarjoajaNimi, List<ErillishakuRivi> erillishakurivit,
                            ErillishakuRiviKuuntelija kuuntelija) {
        erillishakurivit = Lists.newArrayList(erillishakurivit);
        List<Rivi> rivit = Lists.newArrayList();
        Collection<Collection<Arvo>> esittelyt = Lists.newArrayList();
        esittelyt.add(Arrays.asList(new TekstiArvo(hakuNimi, true, false, 4)));
        esittelyt.add(Arrays.asList(new TekstiArvo(hakukohdeNimi, true, false, 4)));
        esittelyt.add(Arrays.asList(new TekstiArvo(tarjoajaNimi, true, false, 4)));
        esittelyt.add(Arrays.asList(new TekstiArvo(StringUtils.EMPTY)));
        esittelyt.add(Arrays.asList(
                new TekstiArvo("Sukunimi"),
                new TekstiArvo("Etunimi"),
                new TekstiArvo("Henkilötunnus"),
                new TekstiArvo("Sähköposti"),
                new TekstiArvo("Syntymäaika"),
                new TekstiArvo("Sukupuoli"),
                new TekstiArvo("Hakija-oid"),
                new TekstiArvo("Äidinkieli"),
                new TekstiArvo("Hakemuksentila"),
                new TekstiArvo("Vastaanottotila"),
                new TekstiArvo("Ilmoittautumistila"),
                new TekstiArvo("Julkaistavissa")));
        Collections.sort(erillishakurivit, (h1, h2) -> {
            ErillishakuRivi e1 = Optional.ofNullable(h1).orElse(emptyErillishakuRivi());
            ErillishakuRivi e2 = Optional.ofNullable(h2).orElse(emptyErillishakuRivi());
            String s1 = Optional.ofNullable(e1.getSukunimi()).orElse(StringUtils.EMPTY).toUpperCase();
            String s2 = Optional.ofNullable(e2.getSukunimi()).orElse(StringUtils.EMPTY).toUpperCase();
            int i = s1.compareTo(s2);
            if (i != 0) {
                return i;
            } else {
                String ee1 = Optional.ofNullable(e1.getEtunimi()).orElse(StringUtils.EMPTY).toUpperCase();
                String ee2 = Optional.ofNullable(e2.getEtunimi()).orElse(StringUtils.EMPTY).toUpperCase();
                return ee1.compareTo(ee2);
            }
        });
        ErillishakuDataRivi dataRivit = new ErillishakuDataRivi(
                kuuntelija,
                Stream.concat(
                        esittelyt.stream(),
                        arvoRivit(erillishakurivit).map(luoArvot(tyyppi))).collect(Collectors.toList()));

        rivit.add(dataRivit);
        this.excel = new Excel("Erillishaku", rivit);
    }

    private Stream<ErillishakuRivi> arvoRivit(List<ErillishakuRivi> erillishakurivit) {
        if (erillishakurivit.isEmpty()) {
            return ImmutableList.of(
                    new ErillishakuRivi(
                            null,
                            "Esimerkki",
                            "Rivi",
                            "123456-7890",
                            "esimerkki.rivi@example.com",
                            "01.01.1901",
                            Sukupuoli.NAINEN,
                            "",
                            "FI",
                            "HYVAKSYTTY",
                            "KESKEN",
                            "EI_TEHTY",
                            false,
                            false,
                            "FI",
                            "040123456789",
                            "Esimerkkitie 2",
                            "00100",
                            "HELSINKI",
                            "SUOMI",
                            "Suomi",
                            "HELSINKI",
                            "Pohjakoulutusmaatoinenaste")).stream();
        } else {
            return erillishakurivit.stream();
        }

    }

    private Function<ErillishakuRivi, Collection<Arvo>> luoArvot(Hakutyyppi tyyppi) {
        return rivi -> {
            Collection<Arvo> a = Lists.newArrayList();
            a.add(new TekstiArvo(rivi.getSukunimi(), true, true));
            a.add(new TekstiArvo(rivi.getEtunimi(), true, true));
            a.add(new TekstiArvo(rivi.getHenkilotunnus(), true, true));
            a.add(new TekstiArvo(rivi.getSahkoposti(), true, true));
            a.add(new TekstiArvo(rivi.getSyntymaAika(), true, true));
            a.add(new MonivalintaArvo(rivi.getSukupuoli().toString(), ErillishakuDataRivi.SUKUPUOLEN_ARVOT));
            a.add(new TekstiArvo(rivi.getPersonOid(), true, true));
            //a.add(new MonivalintaArvo(rivi.getAidinkieli(), ErillishakuDataRivi.KIELITYYPIN_ARVOT));
            // HUOM! AIDINKIELESTÄ EI VOI TEHDÄ DROPDOWNIA KOSKA EXCEL EI TUE NIIN PITKÄÄ DROPDOWNIA
            a.add(new TekstiArvo(rivi.getAidinkieli(), true, true));
            a.add(ErillishakuDataRivi.hakemuksenTila(rivi.getHakemuksenTila()));
            a.add(ErillishakuDataRivi.vastaanottoTila(tyyppi, rivi.getVastaanottoTila()));
            a.add(ErillishakuDataRivi.ilmoittautumisTila(rivi.getIlmoittautumisTila()));
            a.add(ErillishakuDataRivi.julkaisuLupa(rivi.isJulkaistaankoTiedot()));
            return a;
        };
    }

    public Excel getExcel() {
        return excel;
    }
}
