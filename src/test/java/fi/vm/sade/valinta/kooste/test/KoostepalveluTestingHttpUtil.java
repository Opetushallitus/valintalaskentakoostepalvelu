package fi.vm.sade.valinta.kooste.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;

public class KoostepalveluTestingHttpUtil {
  public static HttpResponse<InputStream> createMockResponse(
      String fileInClasspath, int statusCode, String contentType) throws Exception {
    HttpResponse<InputStream> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(statusCode);

    Map<String, List<String>> responseHeaders = new HashMap<>();
    responseHeaders.put("Content-Type", Collections.singletonList(contentType));
    when(response.headers()).thenReturn(HttpHeaders.of(responseHeaders, (s, s2) -> true));

    String responseJson = classpathResourceAsString(fileInClasspath);
    when(response.body()).thenReturn(new ByteArrayInputStream(responseJson.getBytes(UTF_8)));
    return response;
  }

  public static HttpResponse<InputStream> createMockJsonResponse(String fileInClasspath)
      throws Exception {
    return createMockResponse(fileInClasspath, 200, "application/json");
  }

  public static String classpathResourceAsString(String path) throws Exception {
    return IOUtils.toString(new ClassPathResource(path).getInputStream());
  }
}
