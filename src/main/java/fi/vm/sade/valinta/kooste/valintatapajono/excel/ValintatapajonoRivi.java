package fi.vm.sade.valinta.kooste.valintatapajono.excel;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Maps;

import fi.vm.sade.valinta.kooste.util.KieliUtil;

public class ValintatapajonoRivi {
	private final String oid;
	private final String nimi;
	private final boolean validi;
	private final Map<String, String> kuvaus;

	public ValintatapajonoRivi(String oid, String jonosija, String nimi,
	//
			String tila, String fi, String sv, String en) {
		this.oid = oid;
		this.nimi = nimi;
		this.kuvaus = Maps.newHashMap();
		if (!StringUtils.isBlank(fi)) {
			kuvaus.put(KieliUtil.SUOMI, fi);
		}
		if (!StringUtils.isBlank(sv)) {
			kuvaus.put(KieliUtil.RUOTSI, sv);
		}
		if (!StringUtils.isBlank(en)) {
			kuvaus.put(KieliUtil.ENGLANTI, en);
		}

		this.validi = true;
	}

	public boolean isValidi() {
		return validi;
	}

	public Map<String, String> getKuvaus() {
		return kuvaus;
	}

	public String getNimi() {
		return nimi;
	}

	public String getOid() {
		return oid;
	}
}
