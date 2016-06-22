package fi.vm.sade.valinta.kooste.erillishaku.excel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.excel.arvo.Arvo;
import fi.vm.sade.valinta.kooste.excel.arvo.BooleanArvo;
import fi.vm.sade.valinta.kooste.excel.arvo.MonivalintaArvo;
import fi.vm.sade.valinta.kooste.excel.arvo.TekstiArvo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi.emptyErillishakuRivi;

public class ErillishakuExcel {
    private final static Logger LOG = LoggerFactory.getLogger(ErillishakuExcel.class);
    public static final String HEADER_HYVAKSYMISKIRJE_LAHETETTY = "Hyväksymiskirje lähetetty";
    private final Excel excel;

    public ErillishakuExcel(Hakutyyppi tyyppi, ErillishakuRiviKuuntelija kuuntelija) {
        this(tyyppi, "", "", "", Collections.emptyList(), kuuntelija);
    }

    public ErillishakuExcel(Hakutyyppi tyyppi, String hakuNimi, String hakukohdeNimi,
                            String tarjoajaNimi, List<ErillishakuRivi> erillishakurivit) {
        this(tyyppi, hakuNimi, hakukohdeNimi, tarjoajaNimi, erillishakurivit, rivi -> {});
    }

    ErillishakuExcel(final Hakutyyppi tyyppi, String hakuNimi, String hakukohdeNimi,
                     String tarjoajaNimi, List<ErillishakuRivi> erillishakurivit,
                     ErillishakuRiviKuuntelija kuuntelija) {
        erillishakurivit = Lists.newArrayList(erillishakurivit);
        List<Rivi> rivit = Lists.newArrayList();
        Collection<Collection<Arvo>> esittelyt = Lists.newArrayList();
        esittelyt.add(Collections.singletonList(new TekstiArvo(hakuNimi, true, false, 4)));
        esittelyt.add(Collections.singletonList(new TekstiArvo(hakukohdeNimi, true, false, 4)));
        esittelyt.add(Collections.singletonList(new TekstiArvo(tarjoajaNimi, true, false, 4)));
        esittelyt.add(Collections.singletonList(new TekstiArvo(StringUtils.EMPTY)));

        ImmutableList.Builder builder = ImmutableList.builder();
        builder.add(new TekstiArvo("Sukunimi"));
        builder.add(new TekstiArvo("Etunimi"));
        builder.add(new TekstiArvo("Henkilötunnus"));
        builder.add(new TekstiArvo("Sähköposti"));
        builder.add(new TekstiArvo("Syntymäaika"));
        builder.add(new TekstiArvo("Sukupuoli"));
        builder.add(new TekstiArvo("Hakija-oid"));
        builder.add(new TekstiArvo("Äidinkieli"));
        builder.add(new TekstiArvo("Hakemuksentila"));
        if (tyyppi == Hakutyyppi.KORKEAKOULU) {
            // 'Ehdollinen valinta' -sarake halutaan näyttää vain jos kyseessä KK-haku
            builder.add(new TekstiArvo("Ehdollinen valinta"));
        }
        builder.add(new TekstiArvo(HEADER_HYVAKSYMISKIRJE_LAHETETTY));
        builder.add(new TekstiArvo("Vastaanottotila"));
        builder.add(new TekstiArvo("Ilmoittautumistila"));
        builder.add(new TekstiArvo("Julkaistavissa"));
        builder.add(new TekstiArvo("Asiointikieli"));
        builder.add(new TekstiArvo("Puhelinnumero"));
        builder.add(new TekstiArvo("Osoite"));
        builder.add(new TekstiArvo("Postinumero"));
        builder.add(new TekstiArvo("Postitoimipaikka"));
        builder.add(new TekstiArvo("Asuinmaa"));
        builder.add(new TekstiArvo("Kansalaisuus"));
        builder.add(new TekstiArvo("Kotikunta"));
        builder.add(new TekstiArvo("Pohjakoulutuksen maa (toinen aste)"));
        esittelyt.add(builder.build());

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
                            Sukupuoli.NAINEN.name(),
                            "",
                            "FI",
                            "HYVAKSYTTY",
                            false,
                            new Date(),
                            "KESKEN",
                            "EI_TEHTY",
                            false,
                            false,
                            "FI",
                            "040123456789",
                            "Esimerkkitie 2",
                            "00100",
                            "HELSINKI",
                            "FIN",
                            "FIN",
                            "HELSINKI",
                            "FIN")).stream();
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
            // HUOM! AIDINKIELESTÄ EI VOI TEHDÄ DROPDOWNIA KOSKA EXCEL EI TUE NIIN PITKÄÄ DROPDOWNIA
            a.add(new TekstiArvo(rivi.getAidinkieli(), true, true));
            a.add(ErillishakuDataRivi.hakemuksenTila(tyyppi, rivi.getHakemuksenTila()));
            if (tyyppi == Hakutyyppi.KORKEAKOULU) {
                // Ehdollinen valinta - sarake halutaan näyttää vain jos kyseessä KK-haku
                a.add(new BooleanArvo(rivi.getEhdollisestiHyvaksyttavissa(), ErillishakuDataRivi.TOTUUSARVO, ErillishakuDataRivi.TOSI, ErillishakuDataRivi.EPATOSI, ErillishakuDataRivi.EPATOSI));
            }
            a.add(new TekstiArvo(rivi.getHyvaksymiskirjeLahetetty() == null ? "" : ErillishakuDataRivi.LAHETETTYFORMAT.print(rivi.getHyvaksymiskirjeLahetetty().getTime())));
            a.add(ErillishakuDataRivi.vastaanottoTila(tyyppi, rivi.getVastaanottoTila()));
            a.add(ErillishakuDataRivi.ilmoittautumisTila(rivi.getIlmoittautumisTila()));
            a.add(ErillishakuDataRivi.julkaisuLupa(rivi.isJulkaistaankoTiedot()));
            a.add(ErillishakuDataRivi.asiointiKieli(rivi.getAsiointikieli()));
            a.add(new TekstiArvo(rivi.getPuhelinnumero(), true, true));
            a.add(new TekstiArvo(rivi.getOsoite(), true, true));
            a.add(new TekstiArvo(rivi.getPostinumero(), true, true));
            a.add(new TekstiArvo(rivi.getPostitoimipaikka(), true, true));
            a.add(new TekstiArvo(rivi.getAsuinmaa(), true, true));
            a.add(new TekstiArvo(rivi.getKansalaisuus(), true, true));
            a.add(new TekstiArvo(rivi.getKotikunta(), true, true));
            a.add(new TekstiArvo(rivi.getPohjakoulutusMaaToinenAste(), true, true));
            return a;
        };
    }

    public Excel getExcel() {
        return excel;
    }
}
