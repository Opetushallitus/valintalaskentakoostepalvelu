package fi.vm.sade.valinta.kooste.valintalaskentatulos.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Maps;

import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ValintakoeRivi implements Comparable<ValintakoeRivi> {

	private final String sukunimi;
	private final String etunimet;
	private final String hakemusOid;
	private final Date paivamaara;
	private final Map<String, String> osallistumistiedot;
	private final boolean osallistuuEdesYhteen;

	public ValintakoeRivi(String sukunimi, String etunimet, String hakemusOid,
			Date paivamaara, Map<String, String> osallistumistiedot,
			boolean osallistuuEdesYhteen) {
		this.sukunimi = StringUtils.trimToEmpty(sukunimi);
		this.etunimet = StringUtils.trimToEmpty(etunimet);
		this.hakemusOid = hakemusOid;
		this.paivamaara = paivamaara;
		this.osallistumistiedot = osallistumistiedot;
		this.osallistuuEdesYhteen = osallistuuEdesYhteen;
	}

	@Override
	public int compareTo(ValintakoeRivi o) {
		int i = sukunimi.toUpperCase().compareTo(o.sukunimi.toUpperCase());
		if (i == 0) {
			return etunimet.toUpperCase().compareTo(o.etunimet.toUpperCase());
		}
		return i;
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
		rivi.addAll(Arrays.asList(b.toString(), hakemusOid,
				ExcelExportUtil.DATE_FORMAT.format(paivamaara)));
		// boolean osallistuuEdesYhteen = false;
		for (String valintakoeOid : valintakoeOids) {
			String o = osallistumistiedot.get(valintakoeOid);
			if (o == null) {
				rivi.add("----");
			} else {
				rivi.add(o);
			}
		}
		return rivi.toArray(new String[] {});
	}

	public ValintakoeRivi merge(ValintakoeRivi v) {
		Map<String, String> m = Maps.newHashMap(v.getOsallistumistiedot());
		for (Entry<String, String> e : osallistumistiedot.entrySet()) {
			m.put(e.getKey(), e.getValue());
		}
		return new ValintakoeRivi(sukunimi, etunimet, hakemusOid, v.paivamaara,
				m, osallistuuEdesYhteen || v.osallistuuEdesYhteen);
	}

}
