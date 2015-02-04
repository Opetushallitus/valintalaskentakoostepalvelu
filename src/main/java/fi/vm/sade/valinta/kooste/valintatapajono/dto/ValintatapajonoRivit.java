package fi.vm.sade.valinta.kooste.valintatapajono.dto;

import com.wordnik.swagger.annotations.ApiModel;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoRivi;

import java.util.Collection;
import java.util.List;

/**
 * @author Jussi Jartamo
 */
@ApiModel
public class ValintatapajonoRivit {

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
