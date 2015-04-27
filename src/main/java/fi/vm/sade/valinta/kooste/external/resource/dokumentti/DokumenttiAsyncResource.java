package fi.vm.sade.valinta.kooste.external.resource.dokumentti;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Jussi Jartamo
 */
public interface DokumenttiAsyncResource {

    Peruutettava tallenna(String id, String filename, Long expirationDate, List<String> tags, String mimeType, InputStream filedata,
                  Consumer<Response> responseCallback, Consumer<Throwable> failureCallback);
}
