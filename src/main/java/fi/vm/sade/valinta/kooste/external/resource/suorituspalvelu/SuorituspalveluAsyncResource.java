package fi.vm.sade.valinta.kooste.external.resource.suorituspalvelu;

import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.dto.HakemuksenHarkinnanvaraisuus;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SuorituspalveluAsyncResource {
  CompletableFuture<List<HakemuksenHarkinnanvaraisuus>> haeHarkinnanvaraisuudet(
      List<String> hakemusOids);
}
