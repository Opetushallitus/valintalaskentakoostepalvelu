package fi.vm.sade.valinta.kooste.external.resource.haku.dto;

/**
 * @author Jussi Jartamo
 */
public class HakemusPrototyyppi {

    public final String hakijaOid;
    public final String etunimi;
    public final String sukunimi;
    public final String henkilotunnus;
    public final String syntymaAika;

    public HakemusPrototyyppi(String hakijaOid, String etunimi, String sukunimi, String henkilotunnus, String syntymaAika) {
        this.hakijaOid = hakijaOid;
        this.etunimi = etunimi;
        this.sukunimi = sukunimi;
        this.henkilotunnus = henkilotunnus;
        this.syntymaAika = syntymaAika;
    }
}
