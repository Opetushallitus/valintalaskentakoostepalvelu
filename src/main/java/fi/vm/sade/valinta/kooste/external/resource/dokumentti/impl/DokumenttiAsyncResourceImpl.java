package fi.vm.sade.valinta.kooste.external.resource.dokumentti.impl;

import fi.vm.sade.valinta.dokumenttipalvelu.Dokumenttipalvelu;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import io.reactivex.Observable;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class DokumenttiAsyncResourceImpl implements DokumenttiAsyncResource {
  private final Dokumenttipalvelu dokumenttipalvelu;

  @Autowired
  public DokumenttiAsyncResourceImpl(final Dokumenttipalvelu dokumenttipalvelu) {
    this.dokumenttipalvelu = dokumenttipalvelu;
  }

  @Override
  public CompletableFuture<Void> uudelleenNimea(String dokumenttiId, String filename) {
    return dokumenttipalvelu
        .findAsync(Collections.singleton(dokumenttiId))
        .thenComposeAsync(
            objects -> {
              if (objects.isEmpty()) {
                return CompletableFuture.failedFuture(
                    new RuntimeException(
                        "Dokumenttia ei löytynyt dokumenttiId:llä " + dokumenttiId));
              }
              return dokumenttipalvelu.renameAsync(
                  objects.stream().findFirst().get().key, filename);
            });
  }

  @Override
  public Observable<ResponseEntity<Void>> tallenna(
      String id,
      String filename,
      Long expirationDate,
      List<String> tags,
      String mimeType,
      InputStream filedata) {
    return Observable.fromFuture(
            dokumenttipalvelu.saveAsync(
                id, filename, new Date(expirationDate), tags, mimeType, filedata))
        .map(response -> ResponseEntity.ok().build());
  }
}
