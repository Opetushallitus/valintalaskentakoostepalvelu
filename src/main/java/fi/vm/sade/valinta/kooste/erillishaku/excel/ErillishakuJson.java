package fi.vm.sade.valinta.kooste.erillishaku.excel;

import io.swagger.annotations.ApiModel;

import java.util.Collections;
import java.util.List;

@ApiModel
public class ErillishakuJson {
    private final List<ErillishakuRivi> rivit;

    public ErillishakuJson() {
        this.rivit = Collections.emptyList();
    }

    public ErillishakuJson(List<ErillishakuRivi> rivit) {
        this.rivit = rivit;
    }

    public List<ErillishakuRivi> getRivit() {
        return rivit;
    }
}
