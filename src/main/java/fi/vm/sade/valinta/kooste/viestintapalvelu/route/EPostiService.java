package fi.vm.sade.valinta.kooste.viestintapalvelu.route;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.EPostiRequest;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.EPostiResponse;
import java.util.function.Consumer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

public interface EPostiService {

  void lahetaSecurelinkit(
      EPostiRequest ePostiRequest, Consumer<EPostiResponse> success, Consumer<String> failure);

  void esikatseleSecurelinkki(
      EPostiRequest ePostiRequest, DeferredResult<ResponseEntity<byte[]>> result);
}
