package fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HakemusHakija {

    private Hakemus hakemus;
    private Oppija oppija;

    public Oppija getOppija() {
        return oppija;
    }

    public void setHakemus(Hakemus hakemus) {
        this.hakemus = hakemus;
    }

    public void setOppija(Oppija oppija) {
        this.oppija = oppija;
    }

    public Hakemus getHakemus() {
        return hakemus;
    }
}
