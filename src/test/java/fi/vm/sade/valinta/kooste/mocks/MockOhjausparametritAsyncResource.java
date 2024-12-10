package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import java.util.concurrent.CompletableFuture;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("mockresources")
@Service("OhjausparametritAsyncResource")
public class MockOhjausparametritAsyncResource implements OhjausparametritAsyncResource {
  @Override
  public CompletableFuture<ParametritDTO> haeHaunOhjausparametrit(String hakuOid) {
    return CompletableFuture.completedFuture(new ParametritDTO());
  }
}
