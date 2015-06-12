package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import java.util.Collection;

import fi.vm.sade.valinta.kooste.sijoittelu.dto.DelayedSijoittelu;

public interface JatkuvaSijoittelu {
    Collection<DelayedSijoittelu> haeJonossaOlevatSijoittelut();
}
