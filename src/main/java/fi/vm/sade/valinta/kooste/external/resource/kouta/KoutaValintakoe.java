package fi.vm.sade.valinta.kooste.external.resource.kouta;

import fi.vm.sade.valinta.kooste.external.resource.kouta.dto.KoutaValintakoeDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.AbstractValintakoe;
import java.math.BigDecimal;

public class KoutaValintakoe extends AbstractValintakoe {
  public final BigDecimal vahimmaispisteet;

  public KoutaValintakoe(KoutaValintakoeDTO dto) {
    super(dto.getOid(), dto.getTyyppiUri());
    this.vahimmaispisteet = dto.getVahimmaispisteet();
  }
}
