package fi.vm.sade.valinta.kooste.external.resource.dokumentti;

import io.reactivex.Observable;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.ws.rs.core.Response;

public interface DokumenttiAsyncResource {

  CompletableFuture<Void> uudelleenNimea(String dokumenttiId, String filename);

  Observable<Response> tallenna(String id, String filename, Long expirationDate, List<String> tags, String mimeType,
      InputStream filedata);

  CompletableFuture<HttpResponse<InputStream>> lataa(String documentId);

  CompletableFuture<Void> tyhjenna();
}
