package fi.vm.sade.valinta.kooste.kela.dto;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import fi.vm.sade.tarjonta.service.resources.dto.HakuDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.kela.komponentti.HakemusSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.HakukohdeSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.LinjakoodiSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.OppilaitosSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.PaivamaaraSource;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Base class for haku and lisahaku
 */
public abstract class KelaAbstraktiHaku {

	public static final String SUKUNIMI = "sukunimi";
	public static final String ETUNIMET = "Etunimet";

	private final HakuDTO haku;
	private final PaivamaaraSource paivamaaraSource;

	public KelaAbstraktiHaku(HakuDTO haku, PaivamaaraSource paivamaaraSource) {
		this.haku = haku;
		this.paivamaaraSource = paivamaaraSource;
	}

	public HakuDTO getHaku() {
		return haku;
	}

	protected PaivamaaraSource getPaivamaaraSource() {
		return paivamaaraSource;
	}

	/**
	 * 
	 * @return kela hakuun liittyvat hakemus oidit
	 */
	public abstract Collection<String> getHakemusOids();

	// public Collection<String> hakemusOids();
	public abstract Collection<KelaHakijaRivi> createHakijaRivit(
			HakemusSource hakemusSource, HakukohdeSource hakukohdeSource,
			LinjakoodiSource linjakoodiSource, OppilaitosSource oppilaitosSource);

	/**
	 * @return case insensitive map
	 */
	protected Map<String, String> additionalInfo(Hakemus hakemus) {
		Map<String, String> additionalInfo = new TreeMap<String, String>(
				String.CASE_INSENSITIVE_ORDER);
		additionalInfo.putAll(hakemus.getAdditionalInfo());
		return additionalInfo;
	}

	protected Map<String, String> henkilotiedot(Hakemus hakemus) {
		Map<String, String> henkilotiedot = new TreeMap<String, String>(
				String.CASE_INSENSITIVE_ORDER);
		henkilotiedot.putAll(hakemus.getAnswers().getHenkilotiedot());
		return henkilotiedot;
	}
}
