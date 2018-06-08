package fi.vm.sade.valinta.kooste.util;

import java.util.Collection;

public interface HakemusWrapper {
    String NAINEN = "2";
    String MIES = "1";

    String getUlkomainenLahiosoite();

    String getSukupuoli();

    String getSukupuoliAsIs();

    String getAidinkieli();

    String getKaupunkiUlkomaa();

    String getUlkomainenPostinumero();

    String getSuomalainenLahiosoite();

    String getSuomalainenPostinumero();

    String getAsuinmaa();

    String getKansallinenId();

    String getKansalaisuus();

    String getPassinnumero();

    String getKotikunta();

    String getPuhelinnumero();

    boolean isMaksuvelvollinen(String hakukohdeOid);

    String getSahkopostiOsoite();

    String getSyntymaaika();

    String getHenkilotunnus();

    boolean hasHenkilotunnus();

    String getPersonOid();

    Integer getHakutoiveenPrioriteetti(String hakukohdeOid);

    Boolean getToisenAsteenSuoritus();

    String getToisenAsteenSuoritusmaa();

    String getEtunimi();

    String getEtunimet();

    String getSukunimi();

    boolean getLupaJulkaisuun();

    boolean getVainSahkoinenViestinta();

    boolean hasAsiointikieli();

    String getAsiointikieli();

    boolean getLupaSahkoiseenAsiointiin();

    Collection<String> getHakutoiveOids();

    String getMaksuvelvollisuus(String hakukohdeOid);
}
