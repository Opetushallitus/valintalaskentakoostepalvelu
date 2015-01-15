package fi.vm.sade.valinta.kooste.erillishaku.excel;

import com.wordnik.swagger.annotations.ApiModel;

import java.util.List;

@ApiModel
public class ErillishakuJson {

    public final List<ErillishakuRivi> rivit;

    public ErillishakuJson(List<ErillishakuRivi> rivit) {
        this.rivit = rivit;
    }
}
