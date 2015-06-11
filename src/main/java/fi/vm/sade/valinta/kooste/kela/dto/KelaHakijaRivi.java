package fi.vm.sade.valinta.kooste.kela.dto;

import java.util.Date;

public class KelaHakijaRivi {

    private final String hakemusOid;
    private final String hakuOid;
    private final String siirtotunnus;
    private final String henkilotunnus;
    private final String etunimi;
    private final String sukunimi;
    private final Date lukuvuosi;
    private final String tutkinnontaso;
    private final Date poimintapaivamaara;
    private final Date valintapaivamaara;
    private final String oppilaitosnumero;
    private final String organisaatio;
    private final String hakukohde;
    private final String syntymaaika; // 04.05.1965

    public KelaHakijaRivi(String hakemusOid, // <- seurattavuuden vuoksi hakemusOid mukaan
                          String siirtotunnus, String etunimi, String sukunimi,
                          String henkilotunnus, Date lukuvuosi, Date poimintapaivamaara,
                          Date valintapaivamaara, String oppilaitosnumero, String organisaatio, String haku, // seurattavuuden vuoksi
                          String hakukohde,
                          String syntymaaika, String tutkinnontaso) {
        this.hakemusOid = hakemusOid;
        this.hakuOid = haku;
        this.siirtotunnus = siirtotunnus;
        this.etunimi = etunimi;
        this.sukunimi = sukunimi;
        this.henkilotunnus = henkilotunnus;
        this.lukuvuosi = lukuvuosi;
        this.poimintapaivamaara = poimintapaivamaara;
        this.valintapaivamaara = valintapaivamaara;
        this.oppilaitosnumero = oppilaitosnumero;
        this.organisaatio = organisaatio;
        this.hakukohde = hakukohde;
        this.syntymaaika = syntymaaika;
        this.tutkinnontaso = tutkinnontaso;
    }

    public String getHakuOid() {
        return hakuOid;
    }

    public String getHakemusOid() {
        return hakemusOid;
    }

    public String getSiirtotunnus() {
        return siirtotunnus;
    }

    public String getSyntymaaika() {
        return syntymaaika;
    }

    public boolean hasHenkilotunnus() {
        return henkilotunnus != null;
    }

    public Date getValintapaivamaara() {
        return valintapaivamaara;
    }

    public String getHenkilotunnus() {
        return henkilotunnus;
    }

    public String getEtunimi() {
        return etunimi;
    }

    public String getSukunimi() {
        return sukunimi;
    }

    public Date getPoimintapaivamaara() {
        return poimintapaivamaara;
    }

    public Date getLukuvuosi() {
        return lukuvuosi;
    }

    public String getOppilaitosnumero() {
        return oppilaitosnumero;
    }

    public String getOrganisaatio() {
        return organisaatio;
    }

    public String getHakukohde() {
        return hakukohde;
    }

    public String getTutkinnontaso() {
        return tutkinnontaso;
    }
}
