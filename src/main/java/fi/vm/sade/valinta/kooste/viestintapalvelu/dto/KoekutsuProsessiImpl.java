package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Varoitus;
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

	public boolean isKeskeytetty() {
		return !getPoikkeukset().isEmpty();
	}
	
	@Override
	public void keskeyta(String syy) {
		if (getDokumenttiId() == null) {
			getPoikkeukset().add(
					new Poikkeus(Poikkeus.KOOSTEPALVELU, syy));
		}
	}

    @Override
    public void keskeyta(String syy, Map<String, String> virheet) {
        if (getDokumenttiId() == null) {
            getPoikkeukset().add(
                    new Poikkeus(Poikkeus.KOOSTEPALVELU, syy));

            virheet.keySet().stream().forEach(key -> {
                getVaroitukset().add(
                        new Varoitus(key, virheet.get(key))
                );
            });
        }
    }

    public void keskeyta() {
		if (getDokumenttiId() == null) {
			getPoikkeukset().add(
					new Poikkeus(Poikkeus.KOOSTEPALVELU, StringUtils.EMPTY));
		}
	}
}
