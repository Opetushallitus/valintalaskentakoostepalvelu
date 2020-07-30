package fi.vm.sade.valinta.kooste.external.resource.sijoittelu;

import java.util.function.Consumer;

public interface SijoitteleAsyncResource {
  void sijoittele(
      String hakuOid, Consumer<String> successCallback, Consumer<Throwable> failureCallback);
}
