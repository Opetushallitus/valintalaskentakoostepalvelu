package fi.vm.sade.valinta.kooste.sijoitteluntulos.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;

public class SijoittelunTulosProsessi extends DokumenttiProsessi {
    private final AtomicInteger valmis = new AtomicInteger(-1);
    private final Collection<Valmis> valmiit;
    private final Optional<String> asiointikieli;

    public SijoittelunTulosProsessi(Optional<String> asiointikieli, String resurssi, String toiminto, String hakuOid, List<String> tags) {
        super(resurssi, toiminto, hakuOid, tags);
        this.asiointikieli = asiointikieli;
        valmiit = Collections.<Valmis>synchronizedList(Lists.<Valmis>newArrayList());
    }

    public Optional<String> getAsiointikieli() {
        return asiointikieli;
    }

    public int inkrementoi() {
        inkrementoiTehtyjaToita();
        return valmis.decrementAndGet();
    }

    @Override
    public void setKokonaistyo(int arvo) {
        valmis.set(arvo);
        super.setKokonaistyo(arvo);
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public Collection<Valmis> getValmiit() {
        return valmiit;
    }
}
