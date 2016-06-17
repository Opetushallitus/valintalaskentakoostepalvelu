package fi.vm.sade.valinta.kooste.erillishaku.excel;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import fi.vm.sade.authentication.model.HenkiloTyyppi;
import fi.vm.sade.valinta.http.DateDeserializer;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.util.HenkilotunnusTarkistusUtil;
import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static org.apache.commons.lang.StringUtils.*;

@ApiModel
public class ErillishakuRivi {
    private static final Logger LOG = LoggerFactory.getLogger(ErillishakuRivi.class);
    public final static DateTimeFormatter SYNTYMAAIKAFORMAT = DateTimeFormat.forPattern("dd.MM.yyyy");

    private final String etunimi;
    private final String sukunimi;
    private final String hakemusOid;

    private final String henkilotunnus;
    private final String sahkoposti;
    private final String syntymaAika;
    @JsonDeserialize(using = SukupuoliDeserializer.class)
    private final Sukupuoli sukupuoli;
    @ApiModelProperty(required = true)
    private final String aidinkieli;
    private final String personOid;
    @ApiModelProperty(required = true)
    private final String hakemuksenTila;
    private final boolean ehdollisestiHyvaksyttavissa;
    private final Date hyvaksymiskirjeLahetetty;
    private final String vastaanottoTila;
    private final String ilmoittautumisTila;
    @ApiModelProperty(required = true)
    private final boolean julkaistaankoTiedot;
    private final boolean poistetaankoRivi;

    private final String asiointikieli;
    private final String puhelinnumero;
    private final String osoite;
    private final String postinumero;
    private final String postitoimipaikka;
    private final String asuinmaa;
    private final String kansalaisuus;
    private final String kotikunta;
    private final String pohjakoulutusMaaToinenAste;

    public static ErillishakuRivi emptyErillishakuRivi() {
        return new ErillishakuRivi(null, null, null, null, null, null, Sukupuoli.EI_SUKUPUOLTA, null, null, null, false,
                null, null, null, false, false, null, null, null, null, null, null, null, null, null);
    }

    public ErillishakuRivi() {
        this(null, null, null, null, null, null, Sukupuoli.EI_SUKUPUOLTA, null, null, null, false,
                null, null, null, false, false, null, null, null, null, null, null, null, null, null);
    }

    public ErillishakuRivi(String hakemusOid, String sukunimi, String etunimi, String henkilotunnus, String sahkoposti,
                           String syntymaAika, String sukupuoli, String personOid, String aidinkieli,
                           String hakemuksenTila, boolean ehdollisestiHyvaksyttavissa, Date hyvaksymiskirjeLahetetty, String vastaanottoTila, String ilmoittautumisTila,
                           boolean julkaistaankoTiedot, boolean poistetaankoRivi, String asiointikieli,
                           String puhelinnumero, String osoite, String postinumero, String postitoimipaikka,
                           String asuinmaa, String kansalaisuus, String kotikunta, String pohjakoulutusMaaToinenAste) {
        this(hakemusOid, sukunimi, etunimi, henkilotunnus, sahkoposti, syntymaAika,
                Sukupuoli.fromString(sukupuoli), personOid, aidinkieli,
                hakemuksenTila, ehdollisestiHyvaksyttavissa, hyvaksymiskirjeLahetetty, vastaanottoTila, ilmoittautumisTila,
                julkaistaankoTiedot, poistetaankoRivi, asiointikieli, puhelinnumero,
                osoite, postinumero, postitoimipaikka, asuinmaa, kansalaisuus, kotikunta, pohjakoulutusMaaToinenAste);
    }

    public ErillishakuRivi(String hakemusOid, String sukunimi, String etunimi, String henkilotunnus, String sahkoposti,
                           String syntymaAika, Sukupuoli sukupuoli, String personOid, String aidinkieli,
                           String hakemuksenTila, boolean ehdollisestiHyvaksyttavissa, Date hyvaksymiskirjeLahetetty, String vastaanottoTila, String ilmoittautumisTila,
                           boolean julkaistaankoTiedot, boolean poistetaankoRivi, String asiointikieli,
                           String puhelinnumero, String osoite, String postinumero, String postitoimipaikka,
                           String asuinmaa, String kansalaisuus, String kotikunta, String pohjakoulutusMaaToinenAste) {
        this.hakemusOid = hakemusOid;
        this.etunimi = etunimi;
        this.sukunimi = sukunimi;
        this.henkilotunnus = henkilotunnus;
        this.sahkoposti = sahkoposti;
        this.syntymaAika = syntymaAika;
        this.sukupuoli = sukupuoli;
        this.aidinkieli = aidinkieli;
        this.personOid = personOid;
        this.hakemuksenTila = hakemuksenTila;
        this.ehdollisestiHyvaksyttavissa = ehdollisestiHyvaksyttavissa;
        this.hyvaksymiskirjeLahetetty = hyvaksymiskirjeLahetetty;
        this.vastaanottoTila = vastaanottoTila;
        this.ilmoittautumisTila = ilmoittautumisTila;
        this.julkaistaankoTiedot = julkaistaankoTiedot;
        this.poistetaankoRivi = poistetaankoRivi;
        this.asiointikieli = asiointikieli;
        this.puhelinnumero = puhelinnumero;
        this.osoite = osoite;
        this.postinumero = postinumero;
        this.postitoimipaikka = postitoimipaikka;
        this.asuinmaa = asuinmaa;
        this.kansalaisuus = kansalaisuus;
        this.kotikunta = kotikunta;
        this.pohjakoulutusMaaToinenAste = pohjakoulutusMaaToinenAste;
    }

    public String getHakemusOid() {
        return hakemusOid;
    }

    public boolean isJulkaistaankoTiedot() {
        return julkaistaankoTiedot;
    }

    public String getEtunimi() {
        return trimToEmpty(etunimi);
    }

    public String getPersonOid() {
        return trimToEmpty(personOid);
    }

    public String getHenkilotunnus() {
        return trimToEmpty(henkilotunnus);
    }

    public String getSahkoposti() {
        return trimToEmpty(sahkoposti);
    }

    public String getSukunimi() {
        return trimToEmpty(sukunimi);
    }

    public String getSyntymaAika() {
        return trimToEmpty(syntymaAika);
    }

    public Sukupuoli getSukupuoli() {
        if (!StringUtils.isBlank(henkilotunnus)) {
            // palauta hetun sukupuoli
            return HenkilotunnusTarkistusUtil.palautaSukupuoli(henkilotunnus);
        }
        return sukupuoli;
    }

    public String getAidinkieli() {
        return aidinkieli;
    }

    public String getHakemuksenTila() {
        return trimToEmpty(hakemuksenTila);
    }

    public boolean getEhdollisestiHyvaksyttavissa() {
        return ehdollisestiHyvaksyttavissa;
    }

    public Date getHyvaksymiskirjeLahetetty() {
        return hyvaksymiskirjeLahetetty;
    }

    public String getIlmoittautumisTila() {
        return trimToEmpty(ilmoittautumisTila);
    }

    public String getVastaanottoTila() {
        return trimToEmpty(vastaanottoTila);
    }

    private String suojaaHenkilotunnusLogeilta(String hetu) {
        if (trimToNull(hetu) == null) {
            return "***HENKILOTUNNUS***";
        } else {
            return EMPTY;
        }
    }

    public String getAsiointikieli() {
        return asiointikieli;
    }

    public String getPuhelinnumero() {
        return puhelinnumero;
    }

    public String getOsoite() {
        return osoite;
    }

    public String getPostinumero() {
        return postinumero;
    }

    public String getPostitoimipaikka() {
        return postitoimipaikka;
    }

    public String getAsuinmaa() {
        return asuinmaa;
    }

    public String getKansalaisuus() {
        return kansalaisuus;
    }

    public String getKotikunta() {
        return kotikunta;
    }

    public String getPohjakoulutusMaaToinenAste() {
        return pohjakoulutusMaaToinenAste;
    }

    @Override
    public String toString() {
        return etunimi + ", " +
                sukunimi + ", " +
                sahkoposti + ", " +
                hakemuksenTila + ", " +
                ErillishakuDataRivi.getTotuusarvoString(ehdollisestiHyvaksyttavissa) + ", " +
                suojaaHenkilotunnusLogeilta(henkilotunnus) + ", " +
                syntymaAika + ", " +
                sukupuoli + ", " +
                aidinkieli + ", " +
                ilmoittautumisTila + ", " +
                ehdollisestiHyvaksyttavissa + ", " +
                hyvaksymiskirjeLahetetty + ", " +
                vastaanottoTila + ", " +
                julkaistaankoTiedot + ", " +
                asiointikieli + ", " +
                puhelinnumero + ", " +
                osoite + ", " +
                postinumero + ", " +
                postitoimipaikka + ", " +
                asuinmaa + ", " +
                kansalaisuus + ", " +
                kotikunta + ", " +
                pohjakoulutusMaaToinenAste;
    }

    public ErillishakuRivi withAidinkieli(String aidinkieli) {
        return new ErillishakuRivi(hakemusOid, sukunimi, etunimi, henkilotunnus, sahkoposti, syntymaAika, sukupuoli, personOid, aidinkieli,
                hakemuksenTila, ehdollisestiHyvaksyttavissa, hyvaksymiskirjeLahetetty, vastaanottoTila, ilmoittautumisTila, julkaistaankoTiedot, poistetaankoRivi, asiointikieli, puhelinnumero,
                osoite, postinumero, postitoimipaikka, asuinmaa, kansalaisuus, kotikunta, pohjakoulutusMaaToinenAste);
    }

    public HenkiloCreateDTO toHenkiloCreateDTO(String kansalaisuus) {
        return new HenkiloCreateDTO(getAidinkieli(), getSukupuoli().name(), getEtunimi(), getSukunimi(),
                getHenkilotunnus(), parseSyntymaAika(), getPersonOid(), HenkiloTyyppi.OPPIJA, getAsiointikieli(), kansalaisuus);
    }

    public Date parseSyntymaAika() {
        try {
            String s = trimToNull(getSyntymaAika());
            if (s == null) {
                return null;
            } else {
                return SYNTYMAAIKAFORMAT.parseDateTime(s).toDate();
            }
        } catch (Exception e) {
            LOG.error("Syntymäaikaa {} ei voitu parsia muodossa dd.MM.yyyy", getSyntymaAika());
            return null;
        }
    }

    public boolean isKesken() {
        return "KESKEN".equalsIgnoreCase(hakemuksenTila) || StringUtils.isBlank(hakemuksenTila);
    }

    public boolean isPoistetaankoRivi() {
        return poistetaankoRivi;
    }
}
