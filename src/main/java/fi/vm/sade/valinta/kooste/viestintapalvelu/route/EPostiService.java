package fi.vm.sade.valinta.kooste.viestintapalvelu.route;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.EPostiRequest;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.EPostiResponse;

import java.util.function.Consumer;

public interface EPostiService {

    void lahetaSecurelinkit(EPostiRequest ePostiRequest, Consumer<EPostiResponse> success, Consumer<String> failure);

}
