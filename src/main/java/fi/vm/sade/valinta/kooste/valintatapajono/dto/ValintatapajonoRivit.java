package fi.vm.sade.valinta.kooste.valintatapajono.dto;

import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoRivi;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;

@ApiModel
public class ValintatapajonoRivit {

  @ApiModelProperty(required = true)
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
