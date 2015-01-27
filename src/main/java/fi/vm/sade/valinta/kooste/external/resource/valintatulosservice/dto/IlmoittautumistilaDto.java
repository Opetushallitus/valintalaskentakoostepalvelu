package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto;

import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;

/**
 * @author Jussi Jartamo
 */
public class IlmoittautumistilaDto {

    private IlmoittautumisTila ilmoittautumisTila;

    public IlmoittautumisTila getIlmoittautumisTila() {
        return ilmoittautumisTila;
    }

    public void setIlmoittautumisTila(IlmoittautumisTila ilmoittautumisTila) {
        this.ilmoittautumisTila = ilmoittautumisTila;
    }

}
