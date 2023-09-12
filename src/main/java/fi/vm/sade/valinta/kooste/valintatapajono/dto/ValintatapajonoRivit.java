package fi.vm.sade.valinta.kooste.valintatapajono.dto;

import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoRivi;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema
public class ValintatapajonoRivit {

  @Schema(required = true)
  private final List<ValintatapajonoRivi> rivit;

  public ValintatapajonoRivit() {
    this.rivit = null;
  }

  public ValintatapajonoRivit(List<ValintatapajonoRivi> rivit) {
    this.rivit = rivit;
  }

  public List<ValintatapajonoRivi> getRivit() {
    return rivit;
  }
}
