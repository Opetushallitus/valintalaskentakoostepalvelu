package fi.vm.sade.valinta.kooste.external.resource.dokumentti;

import io.reactivex.Observable;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.ResponseEntity;

public interface DokumenttiAsyncResource {

  CompletableFuture<Void> uudelleenNimea(String dokumenttiId, String filename);

  Observable<ResponseEntity<Void>> tallenna(
      String id,
      String filename,
      Long expirationDate,
      List<String> tags,
      String mimeType,
      InputStream filedata);
}
