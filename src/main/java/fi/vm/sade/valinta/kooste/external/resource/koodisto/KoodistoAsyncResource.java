package fi.vm.sade.valinta.kooste.external.resource.koodisto;

import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import io.reactivex.Observable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface KoodistoAsyncResource {
    CompletableFuture<List<Koodi>> haeKoodisto(String koodistoUri);

    CompletableFuture<Koodi> maatjavaltiot2ToMaatjavaltiot1(String koodiUri);
}
