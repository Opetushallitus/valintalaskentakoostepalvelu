package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class PistesyottoRivi {

	private final String oid;
	private final String nimi;
	private final String hetu;
	private final Collection<PistesyottoArvo> arvot;

	public PistesyottoRivi(String oid, String nimi, String hetu,
			Collection<PistesyottoArvo> arvot) {
		this.oid = oid;
		this.nimi = nimi;
		this.hetu = hetu;
		this.arvot = arvot;
	}

	public Map<String, String> asAdditionalData() {
		Map<String, String> data = Maps.newHashMap();
		for (PistesyottoArvo arvo : arvot) {
            if(!StringUtils.isBlank(arvo.getArvo()) && !StringUtils.isBlank(arvo.getTila())) {
                if(!arvo.getTila().equals(PistesyottoExcel.VAKIO_EI_OSALLISTUNUT)) {
			        data.put(arvo.getTunniste(), arvo.getArvo());
                }
			    data.put(arvo.getOsallistuminenTunniste(), arvo.getTila());
            } else if(StringUtils.isBlank(arvo.getArvo()) && arvo.getTila().equals(PistesyottoExcel.VAKIO_EI_OSALLISTUNUT)) {
                data.put(arvo.getOsallistuminenTunniste(), arvo.getTila());
            }
		}
		return data;
	}

	public boolean isValidi() {
		for (PistesyottoArvo a : arvot) {
			if (!a.isValidi()) {
				return false;
			}
		}
		return true;
	}

	public Collection<PistesyottoArvo> getArvot() {
		return arvot;
	}

	public String getNimi() {
		return nimi;
	}

	public String getOid() {
		return oid;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[oid=").append(oid).append(",nimi=").append(nimi).append(",hetu=").append(hetu).append(",");
		for(PistesyottoArvo arvo: arvot) {
			sb.append("\r\n\t[tunniste=").append(arvo.getTunniste()).append(",arvo=").append(arvo.getArvo())
			.append(",tila=").append(arvo.getTila())
			.append(",osallistumisenTunniste=").append(arvo.getOsallistuminenTunniste()).append("]");
		}
		sb.append("]");
		return sb.toString();
	}
}
