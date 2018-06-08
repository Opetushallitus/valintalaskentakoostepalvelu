package fi.vm.sade.valinta.kooste.util;

import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class AtaruHakemusWrapper implements HakemusWrapper{

    private final AtaruHakemus hakemus;
    private final Map<String,String> keyvalues;
    private final HenkiloPerustietoDto henkilo;

    public AtaruHakemusWrapper(AtaruHakemus ataruHakemus, HenkiloPerustietoDto onrHenkilo) {
        hakemus = ataruHakemus;
        keyvalues = ataruHakemus.getKeyValues();
        henkilo = onrHenkilo;
    }

    public String getUlkomainenLahiosoite() {
        return "";
    }

    public String getSukupuoli() {
        return Stream.of(Optional.ofNullable(henkilo.getSukupuoli()).orElse(
                StringUtils.EMPTY)).map(s -> {
            if (NAINEN.equals(s)) {
                return "Nainen";
            } else if (MIES.equals(s)) {
                return "Mies";
            }
            return s;
        }).findAny().get();
    }

    @Override
    public String getSukupuoliAsIs() { return henkilo.getSukupuoli(); }

    @Override
    public String getAidinkieli() { return henkilo.getAidinkieli().getKieliKoodi(); }

    @Override
    public String getKaupunkiUlkomaa() { return null; }

    @Override
    public String getUlkomainenPostinumero() { return null; }

    @Override
    public String getSuomalainenLahiosoite() { return keyvalues.get("address"); }

    @Override
    public String getSuomalainenPostinumero() { return keyvalues.get("postal-code"); }

    @Override
    public String getAsuinmaa() { return keyvalues.get("country-of-residence"); }

    @Override
    public String getKansallinenId() { return keyvalues.get("national-id-number"); }

    @Override
    public String getKansalaisuus() { return henkilo.getKansalaisuus().iterator().next().getKansalaisuusKoodi(); }

    @Override
    public String getPassinnumero() { return keyvalues.get("passport-number"); }

    @Override
    public String getKotikunta() { return keyvalues.get("home-town"); }

    @Override
    public String getPuhelinnumero() { return keyvalues.get("phone"); }

    @Override
    public String getSahkopostiOsoite() {return keyvalues.get("email"); }

    @Override
    public String getSyntymaaika() { return henkilo.getHetu(); }

    @Override
    public String getHenkilotunnus() { return henkilo.getSyntymaaika().toString(); }

    @Override
    public boolean hasHenkilotunnus() { return StringUtils.isNotEmpty(getHenkilotunnus()); }

    @Override
    public String getPersonOid() { return henkilo.getOidHenkilo(); }

    @Override
    public Integer getHakutoiveenPrioriteetti(String hakukohdeOid) {
        return hakemus.getHakutoiveet().indexOf(hakukohdeOid);
    }

    @Override
    public Boolean getToisenAsteenSuoritus() { return null; }

    @Override
    public String getToisenAsteenSuoritusmaa() { return null; }

    @Override
    public String getEtunimi() { return henkilo.getEtunimet().split("\\s+")[0]; }

    @Override
    public String getEtunimet() { return henkilo.getEtunimet();}

    @Override
    public String getSukunimi() { return henkilo.getSukunimi(); }

    @Override
    public boolean getLupaJulkaisuun() { return false; }

    @Override
    public boolean getVainSahkoinenViestinta() { return false; }

    @Override
    public boolean hasAsiointikieli() { return StringUtils.isNotEmpty(henkilo.getAsiointiKieli().getKieliKoodi()); }

    @Override
    public String getAsiointikieli() { return henkilo.getAsiointiKieli().getKieliKoodi(); }

    @Override
    public boolean getLupaSahkoiseenAsiointiin() { return false; }

    @Override
    public Collection<String> getHakutoiveOids() { return hakemus.getHakutoiveet(); }


    @Override
    public boolean isMaksuvelvollinen(String hakukohdeOid) { return false; }

    @Override
    public String getMaksuvelvollisuus(String hakukohdeOid) { return null; }
}
