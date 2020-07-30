package fi.vm.sade.valinta.kooste.sijoittelu.route;

import fi.vm.sade.valinta.kooste.sijoittelu.dto.Sijoittelu;

public interface SijoittelunValvonta {
  Sijoittelu haeAktiivinenSijoitteluHaulle(String hakuOid);
}
