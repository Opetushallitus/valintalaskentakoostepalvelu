package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.lang.StringUtils;

import fi.vm.sade.valinta.kooste.valvomo.dto.Oid;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class KoekutsuProsessiImpl extends DokumenttiProsessi implements
		KirjeProsessi {

	public KoekutsuProsessiImpl(int vaiheet) {
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

	public void keskeyta() {
		if (getDokumenttiId() == null) {
			getPoikkeukset().add(
					new Poikkeus(Poikkeus.KOOSTEPALVELU, StringUtils.EMPTY));
		}
	}
}
