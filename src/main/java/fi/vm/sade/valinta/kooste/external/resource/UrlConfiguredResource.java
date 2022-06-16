package fi.vm.sade.valinta.kooste.external.resource;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import fi.vm.sade.valinta.sharedutils.http.DateDeserializer;
import fi.vm.sade.valinta.sharedutils.http.HttpResource;
import fi.vm.sade.valinta.sharedutils.http.HttpResourceBuilder;
import io.reactivex.Observable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Function;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class UrlConfiguredResource implements HttpResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(UrlConfiguredResource.class);
  public static final String VALINTALASKENTAKOOSTEPALVELU_CALLER_ID = "1.2.246.562.10.00000000001.valintalaskentakoostepalvelu";
  public final HttpResource wrappedHttpResource;
  private final UrlConfiguration urlConfiguration;

  public UrlConfiguredResource() {
    this(HttpResource.DEFAULT_CLIENT_TIMEOUT_MS);
  }

  public UrlConfiguredResource(long timeoutMillis) {
    this(timeoutMillis, null);
  }

  public UrlConfiguredResource(long timeoutMillis, AbstractPhaseInterceptor casInterceptor) {
    this.urlConfiguration = UrlConfiguration.getInstance();

    HttpResourceBuilder builder = new HttpResourceBuilder(VALINTALASKENTAKOOSTEPALVELU_CALLER_ID).gson(createGson())
        .timeoutMillis(timeoutMillis);

    if (casInterceptor != null) {
      builder.jaxrsClientFactoryBean(installCasFilter(casInterceptor, HttpResource.getJaxrsClientFactoryBean()));
    }
    this.wrappedHttpResource = builder.build();
  }

  protected Gson createGson() {
    return DateDeserializer.gsonBuilder().create();
  }

  protected String getUrl(String key, Object... params) {
    return urlConfiguration.url(key, params);
  }

  private JAXRSClientFactoryBean installCasFilter(AbstractPhaseInterceptor casInterceptor,
      JAXRSClientFactoryBean bean) {
    if ("default".equalsIgnoreCase(System.getProperty("spring.profiles.active", "default"))) {
      List<Interceptor<? extends Message>> interceptors = Lists.newArrayList();
      interceptors.add(casInterceptor);
      bean.setOutInterceptors(interceptors);
      bean.setInInterceptors(interceptors);
    }
    return bean;
  }

  @Override
  public Gson gson() {
    return wrappedHttpResource.gson();
  }

  @Override
  public Observable<Response> getAsObservableLazily(String path) {
    return wrappedHttpResource.getAsObservableLazily(path);
  }

  @Override
  public Observable<Response> getAsObservableLazily(String path,
      Function<WebClient, WebClient> paramsHeadersAndStuff) {
    return wrappedHttpResource.getAsObservableLazily(path, paramsHeadersAndStuff);
  }

  @Override
  public Observable<String> getStringAsObservableLazily(String path) {
    return wrappedHttpResource.getStringAsObservableLazily(path);
  }

  @Override
  public <T> Observable<T> getAsObservableLazily(String path, Type type) {
    return wrappedHttpResource.getAsObservableLazily(path, type);
  }

  @Override
  public <T, A> Observable<T> getAsObservableLazily(String path, Type type, Entity<A> entity) {
    return wrappedHttpResource.getAsObservableLazily(path, type, entity);
  }

  @Override
  public <T> Observable<T> getAsObservableLazily(String path, Type type,
      Function<WebClient, WebClient> paramsHeadersAndStuff) {
    return wrappedHttpResource.getAsObservableLazily(path, type, paramsHeadersAndStuff);
  }

  @Override
  public <T> Observable<T> getAsObservableLazily(String path, Function<String, T> extractor,
      Function<WebClient, WebClient> paramsHeadersAndStuff) {
    return wrappedHttpResource.getAsObservableLazily(path, extractor, paramsHeadersAndStuff);
  }

  @Override
  public <T> Observable<T> getAsObservableLazilyWithInputStream(String path, Type type,
      Function<WebClient, WebClient> paramsHeadersAndStuff) {
    return wrappedHttpResource.getAsObservableLazilyWithInputStream(path, type, paramsHeadersAndStuff);
  }

  @Override
  public <A, B> Observable<B> postAsObservableLazily(String path, Type type, Entity<A> entity) {
    return wrappedHttpResource.postAsObservableLazily(path, type, entity);
  }

  @Override
  public <A, B> Observable<B> postAsObservableLazily(String path, Type type, Entity<A> entity,
      Function<WebClient, WebClient> paramsHeadersAndStuff) {
    return wrappedHttpResource.postAsObservableLazily(path, type, entity, paramsHeadersAndStuff);
  }

  @Override
  public <A> Observable<Response> postAsObservableLazily(String path, Entity<A> entity) {
    return wrappedHttpResource.postAsObservableLazily(path, entity);
  }

  @Override
  public <A> Observable<Response> postAsObservableLazily(String path, Entity<A> entity,
      Function<WebClient, WebClient> paramsHeadersAndStuff) {
    return wrappedHttpResource.postAsObservableLazily(path, entity, paramsHeadersAndStuff);
  }

  @Override
  public <A, B> Observable<B> putAsObservableLazily(String path, Type type, Entity<A> entity) {
    return wrappedHttpResource.putAsObservableLazily(path, type, entity);
  }

  @Override
  public <A> Observable<Response> putAsObservableLazily(String path, Entity<A> entity) {
    return wrappedHttpResource.putAsObservableLazily(path, entity);
  }

  @Override
  public <A> Observable<Response> putAsObservableLazily(String path, Entity<A> entity,
      Function<WebClient, WebClient> paramsHeadersAndStuff) {
    return wrappedHttpResource.putAsObservableLazily(path, entity, paramsHeadersAndStuff);
  }

  @Override
  public <A, B> Observable<B> putAsObservableLazily(String path, Type type, Entity<A> entity,
      Function<WebClient, WebClient> paramsHeadersAndStuff) {
    return wrappedHttpResource.putAsObservableLazily(path, type, entity, paramsHeadersAndStuff);
  }

  @Override
  public <A> Observable<A> deleteAsObservableLazily(String path, Type type,
      Function<WebClient, WebClient> paramsHeadersAndStuff) {
    return wrappedHttpResource.deleteAsObservableLazily(path, type, paramsHeadersAndStuff);
  }
}
