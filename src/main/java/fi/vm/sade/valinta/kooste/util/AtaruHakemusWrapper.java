package fi.vm.sade.valinta.kooste.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakukohdeDTO;
import org.apache.commons.lang.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static fi.vm.sade.valinta.kooste.util.Converter.setHakemusDTOvalintapisteet;

public class AtaruHakemusWrapper extends HakemusWrapper {

    private final AtaruHakemus hakemus;
    private final Map<String,String> keyvalues;
    private final HenkiloPerustietoDto henkilo;
    private List<String> kansalaisuudet;
    private final static String PREFERENCE_REGEX = "preference\\d-Koulutus-id-eligibility";
    private final static ImmutableMap<String, String> ELIGIBILITIES = new ImmutableMap.Builder<String, String>()
            .put("eligible", "ELIGIBLE")
            .put("uneligible", "INELIGIBLE")
            .put("unreviewed", "NOT_CHECKED")
            .put("conditionally-eligible", "CONDITIONALLY_ELIGIBLE")
            .build();

    public AtaruHakemusWrapper(AtaruHakemus ataruHakemus, HenkiloPerustietoDto onrHenkilo) {
        hakemus = Objects.requireNonNull(ataruHakemus, "Ataruhakemus oli null.");
        keyvalues = ataruHakemus.getKeyValues();
        henkilo = Objects.requireNonNull(onrHenkilo, "Henkilo ataruhakemukselle oli null.");
    }

    @Override
    public String getOid() {
        return StringUtils.trimToEmpty(hakemus.getHakemusOid());
    }

    @Override
    public String getUlkomainenLahiosoite() {
        return getIfUlkomainenOsoiteOrEmpty("address");
    }

    @Override
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
    public String getSukupuoliAsIs() { return StringUtils.trimToEmpty(henkilo.getSukupuoli()); }

    @Override
    public String getAidinkieli() {
        if (null == henkilo.getAidinkieli() || StringUtils.isBlank(henkilo.getAidinkieli().getKieliKoodi())) {
            throw new IllegalStateException(String.format("Henkilöllä %s ei ole äidinkieltä", henkilo.getOidHenkilo()));
        }
        return henkilo.getAidinkieli().getKieliKoodi();
    }

    @Override
    public String getKaupunkiUlkomaa() { return getIfUlkomainenOsoiteOrEmpty("city"); }

    @Override
    public String getUlkomainenPostinumero() { return getIfUlkomainenOsoiteOrEmpty("postal-code"); }

    @Override
    public String getUlkomainenPostitoimipaikka() { return getIfUlkomainenOsoiteOrEmpty("city"); }

    @Override
    public String getSuomalainenLahiosoite() { return getIfSuomalainenOsoiteOrEmpty("address"); }

    @Override
    public String getSuomalainenPostinumero() { return getIfSuomalainenOsoiteOrEmpty("postal-code"); }

    @Override
    public String getAsuinmaa() { return StringUtils.trimToEmpty(keyvalues.get("country-of-residence")); }

    @Override
    public String getKansallinenId() { return StringUtils.trimToEmpty(keyvalues.get("national-id-number")); }

    @Override
    public String getKansalaisuus() {
        return kansalaisuudet != null ? kansalaisuudet.iterator().next() : StringUtils.trimToEmpty(henkilo.getKansalaisuus().iterator().next().getKansalaisuusKoodi()); }

    public void setKansalaisuus(List<String> kansalaisuudet) {
        this.kansalaisuudet = kansalaisuudet;
    }

    @Override
    public String getPassinnumero() { return StringUtils.trimToEmpty(keyvalues.get("passport-number")); }

    @Override
    public String getKotikunta() { return StringUtils.trimToEmpty(keyvalues.get("home-town")); }

    @Override
    public String getPuhelinnumero() { return StringUtils.trimToEmpty(keyvalues.get("phone")); }

    public Collection<String> getPuhelinnumerot() { return Lists.newArrayList(StringUtils.trimToEmpty(keyvalues.get("phone"))); }

    @Override
    public String getSahkopostiOsoite() {return StringUtils.trimToEmpty(keyvalues.get("email")); }

    @Override
    public String getSyntymaaika() { return henkilo.getSyntymaaika().format(DateTimeFormatter.ISO_LOCAL_DATE); }

    @Override
    public String getHenkilotunnus() { return StringUtils.trimToEmpty(henkilo.getHetu()); }

    @Override
    public boolean hasHenkilotunnus() { return StringUtils.isNotEmpty(getHenkilotunnus()); }

    @Override
    public String getPersonOid() { return StringUtils.trimToEmpty(henkilo.getOidHenkilo()); }

    @Override
    public Integer getHakutoiveenPrioriteetti(String hakukohdeOid) {
        int i = hakemus.getHakutoiveet().indexOf(hakukohdeOid);
        return i < 0 ? null : i + 1;
    }

    @Override
    public Boolean getToisenAsteenSuoritus() { return null; }

    @Override
    public String getToisenAsteenSuoritusmaa() { return null; }

    @Override
    public String getEtunimi() { return StringUtils.trimToEmpty(henkilo.getEtunimet()).split("\\s+")[0]; }

    @Override
    public String getKutsumanimi() { return StringUtils.trimToEmpty(henkilo.getKutsumanimi()); }

    @Override
    public String getEtunimet() { return StringUtils.trimToEmpty(henkilo.getEtunimet());}

    @Override
    public String getSukunimi() { return StringUtils.trimToEmpty(henkilo.getSukunimi()); }

    @Override
    public boolean getLupaJulkaisuun() {
        if (keyvalues.containsKey("valintatuloksen-julkaisulupa")) {
            if (keyvalues.get("valintatuloksen-julkaisulupa").equals("Kyllä")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean getVainSahkoinenViestinta() { return false; }

    @Override
    public boolean getLupaTulosEmail() {
        return "Kyllä".equals(StringUtils.trimToEmpty(keyvalues.get("sahkoisen-asioinnin-lupa")));
    }

    @Override
    public boolean hasAsiointikieli() {
        try {
            this.getAsiointikieli();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getAsiointikieli() {
        return hakemus.getAsiointikieli();
    }

    @Override
    public boolean getLupaSahkoiseenAsiointiin() {
        if (keyvalues.containsKey("sahkoisen-asioinnin-lupa")) {
            if (keyvalues.get("sahkoisen-asioinnin-lupa").equals("Kyllä")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<String> getHakutoiveOids() {
        return new HashSet<>(hakemus.getHakutoiveet());
    }

    @Override
    public boolean isMaksuvelvollinen(String hakukohdeOid) {
        if (hakemus.getMaksunTila().containsKey(hakukohdeOid)) {
                if (hakemus.getMaksunTila().get(hakukohdeOid).equals("obligated")){
                    return true;
            }
        }
        return false;
    }

    @Override
    public String getMaksuvelvollisuus(String hakukohdeOid) { return null; }

    @Override
    public boolean ulkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt() {
        return false;
    }

    @Override
    public String getHakuoid() { return hakemus.getHakuOid(); }

    @Override
    public String getState() { return null; }

    @Override
    public int hashCode() {
        return Optional.ofNullable(getOid()).orElse("").hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (this == obj) {
            return true;
        } else if (obj instanceof AtaruHakemusWrapper) {
            return this.getOid().equals(((AtaruHakemusWrapper) obj).getOid());
        } else {
            return false;
        }
    }

    private static void setPreferenceValue(String value, AvainArvoDTO aa) {
        if (ELIGIBILITIES.containsKey(value)) {
            aa.setArvo(ELIGIBILITIES.get(value));
        } else {
            throw new IllegalArgumentException(String.format("Could not parse hakemus preference value: %s", value));
        }
    }

    @Override
    public HakemusDTO toHakemusDto(Valintapisteet valintapisteet, Map<String, List<String>> hakukohdeRyhmasForHakukohdes) {
        HakemusDTO hakemusDto = new HakemusDTO();
        hakemusDto.setHakemusoid(getOid());
        hakemusDto.setHakijaOid(getPersonOid());
        hakemusDto.setHakuoid(getHakuoid());

        if (hakemus.getKeyValues() != null) {
            hakemus.getKeyValues().forEach((key, value) -> {
                if (!"language".equals(key)) { // FIXME Hakemuspalvelun ei tulisi palauttaa ONR dataa
                    AvainArvoDTO aa = new AvainArvoDTO();
                    aa.setAvain(key);
                    if (key.matches(PREFERENCE_REGEX)) {
                        setPreferenceValue(value, aa);
                    } else {
                        aa.setArvo(value);
                    }
                    hakemusDto.getAvaimet().add(aa);
                }
            });
        }

        hakemusDto.getAvaimet().add(new AvainArvoDTO("language", getAidinkieli()));

        IntStream.range(0, hakemus.getHakutoiveet().size())
                .forEach(i -> {
                    HakukohdeDTO hk = new HakukohdeDTO();
                    String oid = hakemus.getHakutoiveet().get(i);
                    hk.setOid(oid);
                    hk.setPrioriteetti(i + 1);
                    hk.setHakukohdeRyhmatOids(hakukohdeRyhmasForHakukohdes.get(oid));
                    hk.setHarkinnanvaraisuus(false);
                    hakemusDto.getHakukohteet().add(hk);
                });

        setHakemusDTOvalintapisteet(valintapisteet, hakemusDto);

        return hakemusDto;
    }

    private String getIfSuomalainenOsoiteOrEmpty(String key) {
        if(keyvalues.get("home-town") == null) return "";
        return StringUtils.trimToEmpty(keyvalues.get(key));
    }

    private String getIfUlkomainenOsoiteOrEmpty(String key) {
        if(keyvalues.get("home-town") != null) return "";
        return StringUtils.trimToEmpty(keyvalues.get(key));
    }
}
