package fi.vm.sade.valinta.kooste.external.resource.dokumentti;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import rx.Observable;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

public interface DokumenttiAsyncResource {
    Peruutettava tallenna(String id, String filename, Long expirationDate, List<String> tags, String mimeType, InputStream filedata,
                          Consumer<Response> responseCallback, Consumer<Throwable> failureCallback);

    Observable<String> uudelleenNimea(String dokumenttiId, String filename);

    Observable<Response> tallenna(String id, String filename, Long expirationDate, List<String> tags, String mimeType, InputStream filedata);
}
