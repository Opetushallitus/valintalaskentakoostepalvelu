package fi.vm.sade.valinta.kooste.external.resource.dokumentti;

import rx.Observable;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;

public interface DokumenttiAsyncResource {

    Observable<String> uudelleenNimea(String dokumenttiId, String filename);

    Observable<Response> tallenna(String id, String filename, Long expirationDate, List<String> tags, String mimeType, InputStream filedata);
}
