package fi.vm.sade.valinta.kooste.external.resource.koodisto;

import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import io.reactivex.Observable;

import java.util.List;

public interface KoodistoAsyncResource {
    Observable<List<Koodi>> haeKoodisto(String koodistoUri);

    Observable<Koodi> maatjavaltiot2ToMaatjavaltiot1(String koodiUri);
}
