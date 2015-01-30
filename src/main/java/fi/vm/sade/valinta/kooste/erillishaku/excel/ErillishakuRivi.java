package fi.vm.sade.valinta.kooste.erillishaku.excel;

import java.util.Date;
import java.util.Optional;

import com.wordnik.swagger.annotations.ApiModel;
import static fi.vm.sade.valinta.kooste.util.HenkilotunnusTarkistusUtil.*;
import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.authentication.model.HenkiloTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 *
 * @author Jussi Jartamo
 *
 */
@ApiModel
public class ErillishakuRivi {
	private static final Logger LOG = LoggerFactory.getLogger(ErillishakuRivi.class);
	public final static org.joda.time.format.DateTimeFormatter SYNTYMAAIKAFORMAT = DateTimeFormat.forPattern("dd.MM.yyyy");

	public static final DateTimeFormatter SYNTYMAAIKA = DateTimeFormat.forPattern("dd.MM.yyyy");
	private final String etunimi;
	private final String sukunimi;
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

	public ErillishakuRivi(String sukunimi,String etunimi, String henkilotunnus, String sahkoposti, String syntymaAika, String personOid, String hakemuksenTila, String vastaanottoTila, String ilmoittautumisTila, boolean julkaistaankoTiedot, Optional<Boolean> poistetaankoRivi) {
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
        this.poistetaankoRivi = poistetaankoRivi.orElse(false);
	}

	public boolean isJulkaistaankoTiedot() {
		return julkaistaankoTiedot;
	}
	public String getEtunimi() {
		return etunimi;
	}
	public String getPersonOid() {
		return personOid;
	}
	public String getHenkilotunnus() {
		return henkilotunnus;
	}
	public String getSahkoposti() {
		return sahkoposti;
	}
	public String getSukunimi() {
		return sukunimi;
	}
	public String getSyntymaAika() {
		return syntymaAika;
	}

	public String getHakemuksenTila() {
		return hakemuksenTila;
	}
	public String getIlmoittautumisTila() {
		return ilmoittautumisTila;
	}
	public String getVastaanottoTila() {
		return vastaanottoTila;
	}

	private String suojaaHenkilotunnusLogeilta(String hetu) {
		if(StringUtils.trimToNull(hetu) == null) {
			return "***HENKILOTUNNUS***";
		} else {
			return StringUtils.EMPTY;
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
			return SYNTYMAAIKAFORMAT.parseDateTime(getSyntymaAika()).toDate();
		} catch (Exception e) {
			LOG.error("Syntymäaikaa {} ei voitu parsia muodossa dd.MM.yyyy", getSyntymaAika());
			return null;
		}
	}

    public boolean isPoistetaankoRivi() {
        return poistetaankoRivi;
    }
}
