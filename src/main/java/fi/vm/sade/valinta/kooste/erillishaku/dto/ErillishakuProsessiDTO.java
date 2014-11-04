package fi.vm.sade.valinta.kooste.erillishaku.dto;

import java.util.Collections;

import org.apache.commons.lang.StringUtils;

import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ErillishakuProsessiDTO extends DokumenttiProsessi implements
		KirjeProsessi {

	public ErillishakuProsessiDTO(int vaiheet) {
		super(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY,
				Collections.emptyList());
		setKokonaistyo(vaiheet);
	}

	@Override
	public void vaiheValmistui() {
		inkrementoiTehtyjaToita();
	}

	public void valmistui(String dokumentId) {
		setDokumenttiId(dokumentId);
	}

	public boolean isKeskeytetty() {
		return !getPoikkeukset().isEmpty();
	}

	public void keskeyta() {
		if (getDokumenttiId() == null) {
			getPoikkeukset().add(
					new Poikkeus(Poikkeus.KOOSTEPALVELU, StringUtils.EMPTY));
		}
	}
}