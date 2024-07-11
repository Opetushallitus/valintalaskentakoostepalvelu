package fi.vm.sade.valinta.kooste.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.io.IOUtils;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.asynchttpclient.uri.Uri;
import org.springframework.core.io.ClassPathResource;

public class KoostepalveluTestingHttpUtil {
  public static Response createMockResponse(
      String fileInClasspath, int statusCode, String contentType, String... args) throws Exception {
    Response response = mock(Response.class);
    when(response.getStatusCode()).thenReturn(statusCode);

    DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();
    httpHeaders.add("Content-Type", Collections.singletonList(contentType));
    when(response.getHeaders()).thenReturn(httpHeaders);

    when(response.getUri())
        .thenReturn(new Uri("scheme", "userInfo", "host", 0, "path", "query", "fragment"));
    String responseJson = classpathResourceAsString(fileInClasspath).formatted(args);
    when(response.getResponseBody()).thenReturn(responseJson);
    return response;
  }

  public static Response createMockJsonResponse(
      String fileInClasspath, int statusCode, String... args) throws Exception {
    return createMockResponse(fileInClasspath, statusCode, "application/json", args);
  }

  public static String classpathResourceAsString(String path) throws Exception {
    return IOUtils.toString(new ClassPathResource(path).getInputStream());
  }

  public static ListenableFuture<Response> listenableFuture(Response resp) {
    ListenableFuture<Response> listenableFuture = mock(ListenableFuture.class);
    when(listenableFuture.toCompletableFuture())
        .thenReturn(CompletableFuture.completedFuture(resp));
    return listenableFuture;
  }
}
