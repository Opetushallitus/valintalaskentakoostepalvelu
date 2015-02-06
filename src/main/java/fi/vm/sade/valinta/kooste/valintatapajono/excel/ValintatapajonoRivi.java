package fi.vm.sade.valinta.kooste.valintatapajono.excel;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import fi.vm.sade.valinta.kooste.valintatapajono.dto.Kuvaus;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Maps;

import static fi.vm.sade.valinta.kooste.util.KieliUtil.*;
import fi.vm.sade.valintalaskenta.domain.valinta.JarjestyskriteerituloksenTila;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;

/**
 * @author Jussi Jartamo
 */
@ApiModel
public class ValintatapajonoRivi {
	@ApiModelProperty(required = true)
	private final String oid;
	@ApiModelProperty(required = true)
	private final String nimi;
	@ApiModelProperty(required = true, allowableValues = "MAARITTELEMATON,HYVAKSYTTAVISSA,HYLATTY,HYVAKSYTTY_HARKINNANVARAISESTI")
	private final String tila;
	@ApiModelProperty(required = true)
	private final String jonosija;
	private final Map<String,String> kuvaus;

	@ApiModelProperty(hidden = false)
	private transient Integer jonosijaNumerona;
	@ApiModelProperty(hidden = false)
	private transient JarjestyskriteerituloksenTila tilaEnumeraationa;
	/*
	@ApiModelProperty(hidden = false)
	private final boolean validi;
	@ApiModelProperty(hidden = false)
	private final JarjestyskriteerituloksenTila kriteeriTila;
	@ApiModelProperty(hidden = false)
	private final String virhe;
	*/

	public ValintatapajonoRivi() {
		this.oid = null;
		this.nimi = null;
		this.tila = null;
		this.kuvaus = null;
		this.jonosija = null;
	}

	public ValintatapajonoRivi(String oid, String jonosija, String nimi,
			String tila, String fi, String sv, String en) {

		this.oid = oid;
		this.jonosija = jonosija; //new BigDecimal(jonosija).intValue();
		this.nimi = nimi;
		this.tila = tila;
		this.kuvaus =
				Arrays.asList(new Kuvaus(SUOMI,fi), new Kuvaus(RUOTSI,sv), new Kuvaus(ENGLANTI,en)).stream()
						.filter(k -> StringUtils.trimToNull(k.getTeksti()) != null)
						.collect(Collectors.toMap(
						k -> k.getKieli(), k -> k.getTeksti()
				));
		/*
		StringBuilder defaultVirhe = new StringBuilder();
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
		this.tila = tila;
		boolean errors = false;
		JarjestyskriteerituloksenTila defaultTila = JarjestyskriteerituloksenTila.HYLATTY;
		if (ValintatapajonoExcel.HYLATTY.equals(tila) || ValintatapajonoExcel.VAKIO_HYLATTY.equals(tila)) {
			defaultTila = JarjestyskriteerituloksenTila.HYLATTY;
		} else if (ValintatapajonoExcel.HYVAKSYTTAVISSA.equals(tila) || ValintatapajonoExcel.VAKIO_HYVAKSYTTAVISSA.equals(tila)) {
			defaultTila = JarjestyskriteerituloksenTila.HYVAKSYTTAVISSA;
		} else if (ValintatapajonoExcel.HYVAKSYTTY_HARKINNANVARAISESTI.equals(tila) || ValintatapajonoExcel.VAKIO_HYVAKSYTTY_HARKINNANVARAISESTI.equals(tila)) {
			defaultTila = JarjestyskriteerituloksenTila.HYVAKSYTTY_HARKINNANVARAISESTI;
		} else {
			defaultVirhe.append("Tuntematon tila ").append(tila).append(".");
			errors = true;
		}

		int defaultJonosija = 0;
		try {
			if (StringUtils.isBlank(jonosija)) {
				if (JarjestyskriteerituloksenTila.HYVAKSYTTAVISSA
						.equals(defaultTila)) {
					errors = true;
					defaultVirhe
							.append(" Tyhjä jonosija vaikka hakija on hyväksyttävissä.");
				}
			} else {
				defaultJonosija = new BigDecimal(jonosija).toBigInteger()
						.intValue();
				if (!JarjestyskriteerituloksenTila.HYLATTY.equals(defaultTila)) {
					defaultTila = JarjestyskriteerituloksenTila.HYVAKSYTTAVISSA;
				}
			}
		} catch (Exception e) {
			// e.printStackTrace();
			errors = true;
			defaultVirhe.append(" Jonosijaa ").append(jonosija)
					.append(" ei voi muuttaa numeroksi.");

		}
		this.kriteeriTila = defaultTila;
		this.jonosija = defaultJonosija;
		this.validi = !errors
				|| defaultTila
						.equals(JarjestyskriteerituloksenTila.MAARITTELEMATON);
		this.virhe = defaultVirhe.toString().trim();
		*/
	}

	@ApiModelProperty(hidden = false)
	public boolean isValidi() {
		return asTila() != JarjestyskriteerituloksenTila.MAARITTELEMATON;
	}

	public JarjestyskriteerituloksenTila asTila() {
		if(tilaEnumeraationa == null) {
			if (tila == null) {
				tilaEnumeraationa = JarjestyskriteerituloksenTila.MAARITTELEMATON;
			} else
			if (ValintatapajonoExcel.VAIHTOEHDOT_KONVERSIO.containsKey(tila)) {
				tilaEnumeraationa = JarjestyskriteerituloksenTila.valueOf(tila);
			} else
			if (ValintatapajonoExcel.VAIHTOEHDOT_TAKAISINPAIN_KONVERSIO.containsKey(tila)) {
				tilaEnumeraationa = JarjestyskriteerituloksenTila.valueOf(ValintatapajonoExcel.VAIHTOEHDOT_TAKAISINPAIN_KONVERSIO.get(tila));
			}
		}
		return tilaEnumeraationa;
	}
	public int asJonosija() {
		if(jonosijaNumerona == null) {
			if(StringUtils.trimToNull(jonosija) == null) {
				jonosijaNumerona = 0;
			} else {
				try {
					jonosijaNumerona = new BigDecimal(jonosija).intValue();
				} catch (Exception e) {
					jonosijaNumerona = 0;
				}
			}
		}
		return jonosijaNumerona;
	}
	public String getTila() {
		return tila;
	}

	public String getJonosija() {
		return jonosija;
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
