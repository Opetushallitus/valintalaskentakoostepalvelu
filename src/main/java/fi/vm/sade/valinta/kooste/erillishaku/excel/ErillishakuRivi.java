package fi.vm.sade.valinta.kooste.erillishaku.excel;

import com.wordnik.swagger.annotations.ApiModel;
import fi.vm.sade.authentication.model.HenkiloTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static org.apache.commons.lang.StringUtils.*;

/**
 *
 * @author Jussi Jartamo
 *
 */
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
	private final String personOid;

	private final String hakemuksenTila;
	private final String vastaanottoTila;
	private final String ilmoittautumisTila;

	private final boolean julkaistaankoTiedot;
    private final boolean poistetaankoRivi;

	public ErillishakuRivi() {
		this.hakemusOid = null;
		this.etunimi =  null;
		this.sukunimi = null;
		this.henkilotunnus = null;
		this.sahkoposti = null;
		this.personOid = null;
		this.syntymaAika = null;
		this.hakemuksenTila = null;
		this.vastaanottoTila = null;
		this.ilmoittautumisTila = null;
		this.julkaistaankoTiedot = false;
        this.poistetaankoRivi = false;
	}

	public ErillishakuRivi(String hakemusOid, String sukunimi,String etunimi, String henkilotunnus, String sahkoposti, String syntymaAika, String personOid, String hakemuksenTila, String vastaanottoTila, String ilmoittautumisTila, boolean julkaistaankoTiedot, boolean poistetaankoRivi) {
		this.hakemusOid = hakemusOid;
		this.etunimi = etunimi;
		this.sukunimi = sukunimi;
		this.henkilotunnus = henkilotunnus;
		this.sahkoposti = sahkoposti;
		this.syntymaAika = syntymaAika;
		this.personOid = personOid;
		this.hakemuksenTila = hakemuksenTila;
		this.vastaanottoTila = vastaanottoTila;
		this.ilmoittautumisTila = ilmoittautumisTila;
		this.julkaistaankoTiedot = julkaistaankoTiedot;
        this.poistetaankoRivi = poistetaankoRivi;
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

	public String getHakemuksenTila() {
		return trimToEmpty(hakemuksenTila);
	}

	public String getIlmoittautumisTila() {
		return trimToEmpty(ilmoittautumisTila);
	}

	public String getVastaanottoTila() {
		return trimToEmpty(vastaanottoTila);
	}

	private String suojaaHenkilotunnusLogeilta(String hetu) {
		if(trimToNull(hetu) == null) {
			return "***HENKILOTUNNUS***";
		} else {
			return EMPTY;
		}
	}

	@Override
	public String toString() {
		return new StringBuilder()
		.append(etunimi)
		.append(", ")
		.append(sukunimi)
		.append(", ")
		.append(sahkoposti)
		.append(", ")
		.append(hakemuksenTila)
		.append(", ")
		.append(suojaaHenkilotunnusLogeilta(henkilotunnus))
		.append(", ")
		.append(syntymaAika)
		.append(", ")
		.append(ilmoittautumisTila)
		.append(", ")
		.append(vastaanottoTila)
		.append(", ")
		.append(julkaistaankoTiedot).toString();
	}

	public HenkiloCreateDTO toHenkiloCreateDTO() {
		return new HenkiloCreateDTO(getEtunimi(), getSukunimi(), getHenkilotunnus(), parseSyntymaAika(), getPersonOid(), HenkiloTyyppi.OPPIJA);
	}

	public Date parseSyntymaAika() {
		try {
			String s = trimToNull(getSyntymaAika());
			if(s == null) {
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
		return "KESKEN".equalsIgnoreCase(hakemuksenTila);
	}

    public boolean isPoistetaankoRivi() {
		return poistetaankoRivi;
	}
}
