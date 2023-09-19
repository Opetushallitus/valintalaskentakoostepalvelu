package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import io.reactivex.Observable;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Profile("mockresources")
@Service
public class MockDokumenttiAsyncResource implements DokumenttiAsyncResource {
  private static Map<String, InputStream> docs = new ConcurrentHashMap<>();

  @Override
  public CompletableFuture<Void> uudelleenNimea(String dokumenttiId, String filename) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public synchronized Observable<ResponseEntity<Object>> tallenna(
      String id,
      String filename,
      Long expirationDate,
      List<String> tags,
      String mimeType,
      InputStream filedata) {
    docs.put(id, filedata);
    return Observable.just(ResponseEntity.ok(filedata));
  }

  public static synchronized InputStream getStoredDocument(String id) {
    if (!docs.containsKey(id)) {
      throw new IllegalStateException("Doc " + id + " not found");
    }
    return docs.get(id);
  }
}
