package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import java.util.Collection;

import com.google.common.collect.Lists;

public class PistesyottoDataRiviListAdapter implements PistesyottoDataRiviKuuntelija {
    private final Collection<PistesyottoRivi> rivit;

    public PistesyottoDataRiviListAdapter() {
        this.rivit = Lists.newArrayList();
    }

    @Override
    public void pistesyottoDataRiviTapahtuma(PistesyottoRivi pistesyottoRivi) {
        rivit.add(pistesyottoRivi);
    }

    public Collection<PistesyottoRivi> getRivit() {
        return rivit;
    }
}
