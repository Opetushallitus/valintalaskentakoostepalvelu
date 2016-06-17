package fi.vm.sade.valinta.kooste.erillishaku.excel;

import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.tulos.dto.ValintatuloksenTila;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.excel.DataRivi;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.excel.SoluLukija;
import fi.vm.sade.valinta.kooste.excel.arvo.Arvo;
import fi.vm.sade.valinta.kooste.excel.arvo.MonivalintaArvo;
import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi.KORKEAKOULU;
import static fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi.TOISEN_ASTEEN_OPPILAITOS;

public class ErillishakuDataRivi extends DataRivi {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ErillishakuDataRivi.class);
    private final ErillishakuRiviKuuntelija kuuntelija;

    ErillishakuDataRivi(ErillishakuRiviKuuntelija kuuntelija, Collection<Collection<Arvo>> s) {
        super(s);
        this.kuuntelija = kuuntelija;
    }

    @Override
    public boolean validoi(Rivi rivi) {
        SoluLukija lukija = new SoluLukija(rivi.getSolut());
        String sukunimi = lukija.getArvoAt(0);
        String etunimi = lukija.getArvoAt(1);
        String henkilotunnus = lukija.getArvoAt(2);
        String sahkoposti = lukija.getArvoAt(3);
        String syntymaAika = lukija.getArvoAt(4);
        Sukupuoli sukupuoli = Sukupuoli.fromString(lukija.getArvoAt(5));
        String oid = lukija.getArvoAt(6);
        String aidinkieli = lukija.getArvoAt(7);

        String hakemuksenTila = lukija.getArvoAt(8);
        boolean ehdollisestiHyvaksytty = TOSI.equals(lukija.getArvoAt(9));
        String vastaanottoTila = lukija.getArvoAt(10);
        String ilmoittautumisTila = lukija.getArvoAt(11);
        boolean julkaistaankoTiedot = LUPA_JULKAISUUN.equals(lukija.getArvoAt(12));

        String asiointikieli = lukija.getArvoAt(13);
        String puhelinnumero = lukija.getArvoAt(14);
        String osoite = lukija.getArvoAt(15);
        String postinumero = lukija.getArvoAt(16);
        String postitoimipaikka = lukija.getArvoAt(17);
        String asuinmaa = lukija.getArvoAt(18);
        String kansalaisuus = lukija.getArvoAt(19);
        String kotikunta = lukija.getArvoAt(20);
        String pohjakoulutusMaaToinenAste = lukija.getArvoAt(21);

        if (isNewRow(rivi, syntymaAika)) {
            kuuntelija.erillishakuRiviTapahtuma(new ErillishakuRivi(null,
                    sukunimi, etunimi, henkilotunnus, sahkoposti, syntymaAika,
                    sukupuoli, oid, aidinkieli, hakemuksenTila, ehdollisestiHyvaksytty,
                    vastaanottoTila, ilmoittautumisTila, julkaistaankoTiedot,
                    false, asiointikieli, puhelinnumero,
                    osoite, postinumero, postitoimipaikka, asuinmaa,
                    kansalaisuus, kotikunta, pohjakoulutusMaaToinenAste));
        }
        return true;
    }

    private boolean isNewRow(Rivi rivi, String syntymaAika) {
        return !rivi.isTyhja()
                && rivi.getSolut().size() >= 13 //Copy-paste easily creates extra columns for excel doc
                && !"Syntymäaika".equals(syntymaAika);
    }

    final static String TOSI = "Kyllä";
    final static String EPATOSI = "Ei";
    final static Collection<String> TOTUUSARVO = Arrays.asList(EPATOSI, TOSI);

    static String getTotuusarvoString(boolean b){
        return b ? TOSI : EPATOSI;
    }

    static final Collection<String> SUKUPUOLEN_ARVOT = Arrays.asList(Sukupuoli.values()).stream().map(Object::toString).collect(Collectors.toList());
    private static final Collection<String> HAKEMUKSENTILA_ARVOT = Stream.concat(Stream.of("KESKEN"),
            Arrays.asList(HakemuksenTila.values()).stream().map(Enum::toString)).collect(Collectors.toList());
    private static final Collection<String> HAKEMUKSENTILA_ARVOT_TOINEN_ASTE = Stream.concat(Stream.of("KESKEN"),
            Arrays.asList(HakemuksenTila.values()).stream().map(Enum::toString)).collect(Collectors.toList());
    private static final Collection<String> HAKEMUKSENTILA_ARVOT_KK = Stream.concat(Stream.of("KESKEN"),
            Arrays.asList(HakemuksenTila.values()).stream().filter(t -> !HakemuksenTila.HARKINNANVARAISESTI_HYVAKSYTTY.equals(t)).map(Enum::toString)).collect(Collectors.toList());
    private static final Collection<String> VASTAANOTTOTILA_ARVOT = Arrays.asList(ValintatuloksenTila.values()).stream().map(Enum::toString).collect(Collectors.toList());
    private static final Collection<String> VASTAANOTTOTILA_ARVOT_KK =
            Arrays.asList(
                    // KORKEAKOULUJEN VALINTATULOKSEN TILAT
                    ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA,
                    ValintatuloksenTila.PERUNUT,
                    ValintatuloksenTila.PERUUTETTU,
                    ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI,
                    ValintatuloksenTila.KESKEN
                    //
            ).stream().map(Enum::toString).collect(Collectors.toList());
    private static final Collection<String> VASTAANOTTOTILA_ARVOT_TOINEN_ASTE =
            Arrays.asList(
                    // TOISEN ASTEEN VALINTATULOKSEN TILAT
                    ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI,
                    ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA,
                    ValintatuloksenTila.PERUNUT,
                    ValintatuloksenTila.KESKEN
                    //
            ).stream().map(Enum::toString).collect(Collectors.toList());
    private static final Collection<String> ILMOITTAUTUMISTILA_ARVOT =
            Arrays.asList(IlmoittautumisTila.values()).stream().map(Enum::toString).collect(Collectors.toList());
    private static final String LUPA_JULKAISUUN = "JULKAISTAVISSA";
    private static final String EI_LUPAA_JULKAISUUN = "EI JULKAISTAVISSA";
    private static final Collection<String> JULKAISU_LUPA_ARVOT =
            Arrays.asList(LUPA_JULKAISUUN, StringUtils.EMPTY, EI_LUPAA_JULKAISUUN);
    public static final Collection<String> ASIONTIKIELEN_ARVOT = Arrays.asList("fi", "sv", "en");

    public static MonivalintaArvo hakemuksenTila(Hakutyyppi hakutyyppi, String arvo) {
        return getMonivalintaArvo(hakutyyppi, arvo, HAKEMUKSENTILA_ARVOT_TOINEN_ASTE, HAKEMUKSENTILA_ARVOT_KK, HAKEMUKSENTILA_ARVOT);
    }

    static MonivalintaArvo vastaanottoTila(Hakutyyppi hakutyyppi, String arvo) {
        return getMonivalintaArvo(hakutyyppi, arvo, VASTAANOTTOTILA_ARVOT_TOINEN_ASTE, VASTAANOTTOTILA_ARVOT_KK, VASTAANOTTOTILA_ARVOT);
    }

    private static MonivalintaArvo getMonivalintaArvo(Hakutyyppi hakutyyppi, String arvo, Collection<String> toinenAste, Collection<String> kk, Collection<String> other) {
        if (TOISEN_ASTEEN_OPPILAITOS.equals(hakutyyppi))
            return new MonivalintaArvo(arvo, toinenAste);
        if (KORKEAKOULU.equals(hakutyyppi))
            return new MonivalintaArvo(arvo, kk);
        return new MonivalintaArvo(arvo, other);
    }

    static MonivalintaArvo julkaisuLupa(boolean arvo) {
        return new MonivalintaArvo(arvo ? LUPA_JULKAISUUN : EI_LUPAA_JULKAISUUN, JULKAISU_LUPA_ARVOT);
    }

    public static MonivalintaArvo ilmoittautumisTila(String arvo) {
        return new MonivalintaArvo(arvo, ILMOITTAUTUMISTILA_ARVOT);
    }

    static MonivalintaArvo asiointiKieli(String arvo) {
        return new MonivalintaArvo(arvo, ASIONTIKIELEN_ARVOT);
    }
}
