package fi.vm.sade.valinta.kooste.hakemus.dto;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Lists;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;

public class Yhteystiedot {

    public static final String SAHKOPOSTI = "sähköposti";
    public static final String MATKAPUHELINNUMERO = "matkapuhelinnumero";
    private String sahkoposti;
    private Collection<String> puhelinnumerot;

    private Yhteystiedot() {
        this.sahkoposti = null;
        this.puhelinnumerot = null;
    }

    private Yhteystiedot(String sahkoposti, Collection<String> puhelinnumerot) {
        this.sahkoposti = sahkoposti;
        this.puhelinnumerot = puhelinnumerot;
    }

    public Collection<String> getPuhelinnumerot() {
        return puhelinnumerot;
    }

    public String getPuhelinnumerotAsString() {
        if (puhelinnumerot == null || puhelinnumerot.isEmpty()) {
            return StringUtils.EMPTY;
        } else {
            StringBuilder b = new StringBuilder();
            for (String puhelinnumero : puhelinnumerot) {
                if (puhelinnumero == null || StringUtils.isEmpty(puhelinnumero)) {

                } else {
                    b.append(puhelinnumero).append(" ");
                }
            }
            return b.toString().trim();
        }
    }

    public String getSahkoposti() {
        return StringUtils.trimToEmpty(sahkoposti);
    }

    public static Yhteystiedot yhteystiedotHakemukselta(Hakemus hakemus) {
        if (hakemus != null) {
            TreeMap<String, String> henkilotiedot = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            henkilotiedot.putAll(hakemus.getAnswers().getHenkilotiedot());
            Collection<String> nums = Lists.newArrayList();
            for (Entry<String, String> e : henkilotiedot.tailMap(Yhteystiedot.MATKAPUHELINNUMERO, true).entrySet()) {
                if (e.getKey().startsWith(Yhteystiedot.MATKAPUHELINNUMERO)) {
                    nums.add(e.getValue());
                } else {
                    break;
                }
            }
            return new Yhteystiedot(henkilotiedot.get(SAHKOPOSTI), nums);
        }
        return new Yhteystiedot();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        if (sahkoposti != null) {
            b.append(sahkoposti).append(" ");
        }
        for (String puhelinnumero : puhelinnumerot) {
            b.append(puhelinnumero).append(" ");
        }
        return b.toString().trim();
    }
}
