package fi.vm.sade.valinta.kooste.external.resource.seuranta;

import fi.vm.sade.valinta.seuranta.sijoittelu.dto.SijoitteluDto;
import java.util.Collection;

public interface SijoitteluSeurantaResource {

  SijoitteluDto hae(String hakuOid);

  Collection<SijoitteluDto> hae();

  SijoitteluDto merkkaaSijoittelunAjossaTila(String hakuOid, boolean tila);

  SijoitteluDto merkkaaSijoittelunAjetuksi(String hakuOid);

  void poistaSijoittelu(String hakuOid);

  void paivitaSijoittelunAloitusajankohta(String hakuOid, long aloitusajankohta, int ajotiheys);
}
