package fi.vm.sade.valinta.kooste.erillishaku.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Varoitus;
import org.apache.commons.lang.StringUtils;

import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;

public class ErillishakuProsessiDTO extends DokumenttiProsessi implements KirjeProsessi {

    public ErillishakuProsessiDTO(int vaiheet) {
        super(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, Collections.emptyList());
        setKokonaistyo(vaiheet);
    }

    @Override
    public void vaiheValmistui() {
        inkrementoiTehtyjaToita();
    }

    public boolean isValmis() {
        return getDokumenttiId() != null;
    }

    public void valmistui(String dokumentId) {
        setDokumenttiId(dokumentId);
    }

    public boolean isKeskeytetty() {
        return !getPoikkeukset().isEmpty();
    }

    public void keskeyta(Poikkeus syy) {
        if (getDokumenttiId() == null) {
            getPoikkeukset().add(syy);
        }
    }

    public void keskeyta(Collection<Poikkeus> syyt) {
        if (getDokumenttiId() == null) {
            getPoikkeukset().addAll(syyt);
        }
    }

    @Override
    public void keskeyta(String syy) {
        if (getDokumenttiId() == null) {
            getPoikkeukset().add(Poikkeus.koostepalvelupoikkeus(syy));
        }
    }

    @Override
    public void keskeyta(String syy, Map<String, String> virheet) {
        if (getDokumenttiId() == null) {
            getPoikkeukset().add(new Poikkeus(Poikkeus.KOOSTEPALVELU, syy));

            virheet.keySet().stream().forEach(key -> {
                getVaroitukset().add(new Varoitus(key, virheet.get(key)));
            });
        }
    }

    public void keskeyta() {
        if (getDokumenttiId() == null && getPoikkeukset().isEmpty()) {
            getPoikkeukset().add(new Poikkeus(Poikkeus.KOOSTEPALVELU, StringUtils.EMPTY));
        }
    }
}