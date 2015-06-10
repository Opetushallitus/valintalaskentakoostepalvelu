package fi.vm.sade.valinta.kooste.external.resource.haku.dto;

import java.util.Date;

import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuDataRivi;

public class HakemusPrototyyppi {

    public final String hakijaOid;
    public final String etunimi;
    public final String sukunimi;
    public final String henkilotunnus;
    public final String sahkoposti;
    public final String syntymaAika;
    public final String sukupuoli;
    public final String aidinkieli;

    public HakemusPrototyyppi(String sukupuoli, String aidinkieli, String hakijaOid, String etunimi, String sukunimi, String henkilotunnus, String sahkoposti, String syntymaAika) {
        this.sukupuoli = sukupuoli;
        this.aidinkieli = aidinkieli;
        this.hakijaOid = hakijaOid;
        this.etunimi = etunimi;
        this.sukunimi = sukunimi;
        this.henkilotunnus = henkilotunnus;
        this.sahkoposti = sahkoposti;
        this.syntymaAika = syntymaAika;
    }

    public HakemusPrototyyppi(final String sukupuoli, final String aidinkieli, final String hakijaOid, final String etunimi, final String sukunimi, final String henkilotunnus, final String sahkoposti, final Date syntymaAika) {
        this(sukupuoli, aidinkieli, hakijaOid, etunimi, sukunimi, henkilotunnus, sahkoposti, parseDate(syntymaAika));

    }

    private static String parseDate(final Date syntymaAika) {
        if (syntymaAika == null) return null;
        return ErillishakuDataRivi.SYNTYMAAIKA.print(syntymaAika.getTime());
    }
}
