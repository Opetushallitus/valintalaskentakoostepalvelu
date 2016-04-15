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

public class ErillishakuDataRivi extends DataRivi {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ErillishakuDataRivi.class);
    public static final DateTimeFormatter SYNTYMAAIKA = DateTimeFormat.forPattern("dd.MM.yyyy");
    private final ErillishakuRiviKuuntelija kuuntelija;

    public ErillishakuDataRivi(ErillishakuRiviKuuntelija kuuntelija, Collection<Collection<Arvo>> s) {
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
        String vastaanottoTila = lukija.getArvoAt(9);
        String ilmoittautumisTila = lukija.getArvoAt(10);
        boolean julkaistaankoTiedot = LUPA_JULKAISUUN.equals(lukija.getArvoAt(11));

        String asiointikieli = lukija.getArvoAt(12);
        String puhelinnumero = lukija.getArvoAt(13);
        String osoite = lukija.getArvoAt(14);
        String postinumero = lukija.getArvoAt(15);
        String postitoimipaikka = lukija.getArvoAt(16);
        String asuinmaa = lukija.getArvoAt(17);
        String kansalaisuus = lukija.getArvoAt(18);
        String kotikunta = lukija.getArvoAt(19);
        String pohjakoulutusMaaToinenAste = lukija.getArvoAt(20);

        if (isNewRow(rivi, syntymaAika)) {
            kuuntelija.erillishakuRiviTapahtuma(new ErillishakuRivi(null,
                    sukunimi, etunimi, henkilotunnus, sahkoposti, syntymaAika,
                    sukupuoli, oid, aidinkieli, hakemuksenTila,
                    vastaanottoTila, ilmoittautumisTila, julkaistaankoTiedot,
                    false, asiointikieli, puhelinnumero,
                    osoite, postinumero, postitoimipaikka, asuinmaa,
                    kansalaisuus, kotikunta, pohjakoulutusMaaToinenAste));
        }
        return true;
    }

    boolean isNewRow(Rivi rivi, String syntymaAika) {
        return !rivi.isTyhja()
                && rivi.getSolut().size() >= 12 //Copy-paste easily creates extra columns for excel doc
                && !"Syntymäaika".equals(syntymaAika);
    }

    public static final Collection<String> SUKUPUOLEN_ARVOT = Arrays.asList(Sukupuoli.values()).stream().map(Object::toString).collect(Collectors.toList());
    public static final Collection<String> KIELITYYPIN_ARVOT =
            Arrays.asList(
                    ("fi|en|sv|ae|lo|sl|bm|mo|nr|kn|ga|tl|la|nv|ti|gl|to|sa|lv|hi|ke|ty|ho|cv|ts|kj|xx|vo|ro|mr|sd|ak|kv|98|fj|su|sq|" +
                            "ie|ab|ug|hr|my|hy|is|gd|ko|tg|am|bi|so|te|lg|dz|wo|az|oc|kl|kw|sk|uz|oj|ng|uk|gg|se|gu|ii|ne|ce|ee|ur|hu|mt|mg|je|zu|pa|sg|" +
                            "aa|ml|eu|bn|zh|rw|99|ha|nn|or|ta|ks|co|cr|mk|vi|io|lt|bo|ru|ik|ja|be|sc|ka|ay|he|xh|fy|dv|tn|eo|jv|sn|na|os|ln|rn|om|hz|rm|" +
                            "ss|et|bs|af|za|ve|ia|gv|st|mn|mi|fo|ri|gn|ku|es|as|ff|ig|da|av|ch|lb|tr|cy|el|li|ki|nb|lu|sm|no|tw|sw|mh|wa|tt|fr|de|km|fa|" +
                            "ht|kk|yo|ny|qu|ca|an|pt|yi|si|bg|cu|nd|ky|th|sr|ba|kr|ps|br|it|im|id|bh|iu|ar|pl|nl|ms|pi|tk|sh|cs|vk|kg").split("\\|"));
    private static final Collection<String> HAKEMUKSENTILA_ARVOT = Stream.concat(Stream.of("KESKEN"),
            Arrays.asList(HakemuksenTila.values()).stream().map(t -> t.toString())).collect(Collectors.toList());
    private static final Collection<String> VASTAANOTTOTILA_ARVOT = Arrays.asList(ValintatuloksenTila.values()).stream().map(t -> t.toString()).collect(Collectors.toList());
    private static final Collection<String> VASTAANOTTOTILA_ARVOT_KK =
            Arrays.asList(
                    // KORKEAKOULUJEN VALINTATULOKSEN TILAT
                    ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA,
                    ValintatuloksenTila.PERUNUT,
                    ValintatuloksenTila.PERUUTETTU,
                    ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI,
                    ValintatuloksenTila.KESKEN
                    //
            ).stream().map(t -> t.toString()).collect(Collectors.toList());
    private static final Collection<String> VASTAANOTTOTILA_ARVOT_TOINEN_ASTE =
            Arrays.asList(
                    // TOISEN ASTEEN VALINTATULOKSEN TILAT
                    ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI,
                    ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA,
                    ValintatuloksenTila.PERUNUT,
                    ValintatuloksenTila.KESKEN
                    //
            ).stream().map(t -> t.toString()).collect(Collectors.toList());
    private static final Collection<String> ILMOITTAUTUMISTILA_ARVOT =
            Arrays.asList(IlmoittautumisTila.values()).stream().map(t -> t.toString()).collect(Collectors.toList());
    private static final String LUPA_JULKAISUUN = "JULKAISTAVISSA";
    private static final String EI_LUPAA_JULKAISUUN = "EI JULKAISTAVISSA";
    private static final Collection<String> JULKAISU_LUPA_ARVOT =
            Arrays.asList(LUPA_JULKAISUUN, StringUtils.EMPTY, EI_LUPAA_JULKAISUUN);
    public static final Collection<String> ASIONTIKIELEN_ARVOT = Arrays.asList("fi", "sv", "en");

    public static MonivalintaArvo hakemuksenTila(String arvo) {
        return new MonivalintaArvo(arvo, HAKEMUKSENTILA_ARVOT);
    }

    public static MonivalintaArvo vastaanottoTila(Hakutyyppi hakutyyppi, String arvo) {
        if (Hakutyyppi.TOISEN_ASTEEN_OPPILAITOS.equals(hakutyyppi)) {
            return new MonivalintaArvo(arvo, VASTAANOTTOTILA_ARVOT_TOINEN_ASTE);
        } else if (Hakutyyppi.KORKEAKOULU.equals(hakutyyppi)) {
            return new MonivalintaArvo(arvo, VASTAANOTTOTILA_ARVOT_KK);
        } else {
            return new MonivalintaArvo(arvo, VASTAANOTTOTILA_ARVOT);
        }
    }

    public static MonivalintaArvo julkaisuLupa(boolean arvo) {
        return new MonivalintaArvo(arvo ? LUPA_JULKAISUUN : EI_LUPAA_JULKAISUUN, JULKAISU_LUPA_ARVOT);
    }

    public static MonivalintaArvo ilmoittautumisTila(String arvo) {
        return new MonivalintaArvo(arvo, ILMOITTAUTUMISTILA_ARVOT);
    }

    public static MonivalintaArvo asiointiKieli(String arvo) {
        return new MonivalintaArvo(arvo, ASIONTIKIELEN_ARVOT);
    }
}
