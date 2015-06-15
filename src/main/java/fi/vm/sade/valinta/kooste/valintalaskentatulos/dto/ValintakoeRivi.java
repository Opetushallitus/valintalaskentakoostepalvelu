package fi.vm.sade.valinta.kooste.valintalaskentatulos.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Maps;

import fi.vm.sade.valinta.kooste.hakemus.dto.Yhteystiedot;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;

public class ValintakoeRivi implements Comparable<ValintakoeRivi> {
    private final String sukunimi;
    private final String etunimet;
    private final String hakemusOid;
    private final String postitoimipaikka;
    private final String asuinmaaEnglanniksi;
    private final Osoite osoite;
    private final Yhteystiedot yhteystiedot;
    private final Date paivamaara;
    private final Map<String, String> osallistumistiedot;
    private final boolean osallistuuEdesYhteen;

    private final HakemusWrapper wrapper;

    public ValintakoeRivi(String sukunimi,
                          String etunimet,
                          String postitoimipaikka,
                          String asuinmaaEnglanniksi,
                          HakemusWrapper wrapper,
                          String hakemusOid,
                          Date paivamaara, Map<String, String> osallistumistiedot,
                          Osoite osoite, Yhteystiedot yhteystiedot,
                          boolean osallistuuEdesYhteen) {
        this.sukunimi = StringUtils.trimToEmpty(sukunimi);
        this.etunimet = StringUtils.trimToEmpty(etunimet);
        this.asuinmaaEnglanniksi = asuinmaaEnglanniksi;
        this.postitoimipaikka = postitoimipaikka;
        this.hakemusOid = hakemusOid;
        this.paivamaara = paivamaara;
        this.yhteystiedot = yhteystiedot;
        this.osoite = osoite;
        this.osallistumistiedot = osallistumistiedot;
        this.osallistuuEdesYhteen = osallistuuEdesYhteen;
        this.wrapper = wrapper;
    }

    @Override
    public int compareTo(ValintakoeRivi o) {
        int i = sukunimi.toUpperCase().compareTo(o.sukunimi.toUpperCase());
        if (i == 0) {
            return etunimet.toUpperCase().compareTo(o.etunimet.toUpperCase());
        }
        return i;
    }

    public HakemusWrapper getWrapper() {
        return wrapper;
    }

    public boolean isOsallistuuEdesYhteen() {
        return osallistuuEdesYhteen;
    }

    public String getHakemusOid() {
        return hakemusOid;
    }

    public String getEtunimet() {
        return etunimet;
    }

    public String getSukunimi() {
        return sukunimi;
    }

    public Date getPaivamaara() {
        return paivamaara;
    }

    public Map<String, String> getOsallistumistiedot() {
        return osallistumistiedot;
    }

    public String[] toArray(List<String> valintakoeOids) {
        ArrayList<String> rivi = new ArrayList<String>();
        StringBuilder b = new StringBuilder();
        b.append(sukunimi).append(", ").append(etunimet);
        String pvm;
        if (paivamaara != null) {
            pvm = ExcelExportUtil.DATE_FORMAT.format(paivamaara);
        } else {
            pvm = StringUtils.EMPTY;
        }
        rivi.addAll(Arrays.asList(
                sukunimi,
                etunimet,
                wrapper.getHenkilotunnus(),
                wrapper.getSyntymaaika(),
                wrapper.getSukupuoli(),
                wrapper.getSuomalainenLahiosoite(),
                wrapper.getSuomalainenPostinumero(),
                postitoimipaikka,
                wrapper.getUlkomainenLahiosoite(),
                wrapper.getUlkomainenPostinumero(),
                wrapper.getKaupunkiUlkomaa(),
                asuinmaaEnglanniksi,
                wrapper.getKansalaisuus(),
                wrapper.getKansallinenId(),
                wrapper.getPassinnumero(),
                yhteystiedot.getSahkoposti(),
                yhteystiedot.getPuhelinnumerotAsString(), hakemusOid, pvm));
        // boolean osallistuuEdesYhteen = false;
        for (String valintakoeOid : valintakoeOids) {
            String o = osallistumistiedot.get(valintakoeOid);
            if (o == null) {
                rivi.add("----");
            } else {
                rivi.add(o);
            }
        }
        return rivi.toArray(new String[]{});
    }

    public ValintakoeRivi merge(ValintakoeRivi v) {
        Map<String, String> m = Maps.newHashMap(osallistumistiedot);
        m.putAll(v.getOsallistumistiedot());
        return new ValintakoeRivi(sukunimi, etunimet, postitoimipaikka, asuinmaaEnglanniksi, wrapper, hakemusOid, v.paivamaara,
                m, osoite, yhteystiedot, osallistuuEdesYhteen
                || v.osallistuuEdesYhteen);
    }

}
