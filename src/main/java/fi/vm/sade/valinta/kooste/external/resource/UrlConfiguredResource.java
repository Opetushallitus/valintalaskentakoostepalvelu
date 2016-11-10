package fi.vm.sade.valinta.kooste.external.resource;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import fi.vm.sade.valinta.http.DateDeserializer;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.http.HttpResourceBuilder;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Function;

public abstract class UrlConfiguredResource implements HttpResource{
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final HttpResource wrappedHttpResource;
    private final UrlConfiguration urlConfiguration;

    public UrlConfiguredResource() {
        this(HttpResource.DEFAULT_CLIENT_TIMEOUT_MS);
    }

    public UrlConfiguredResource(long timeoutMillis) {
        this(timeoutMillis, null);
    }

    public UrlConfiguredResource(long timeoutMillis,
                                 AbstractPhaseInterceptor casInterceptor) {
        this.urlConfiguration = UrlConfiguration.getInstance();

        HttpResourceBuilder builder = new HttpResourceBuilder().gson(createGson()).timeoutMillis(timeoutMillis);
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
    public WebClient getWebClient() {
        return wrappedHttpResource.getWebClient();
    }

    @Override
    public Observable<Response> getAsObservable(String path) {
        return wrappedHttpResource.getAsObservable(path);
    }

    @Override
    public Observable<Response> getAsObservable(String path, Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return wrappedHttpResource.getAsObservable(path, paramsHeadersAndStuff);
    }

    @Override
    public Observable<String> getStringAsObservable(String path) {
        return wrappedHttpResource.getStringAsObservable(path);
    }

    @Override
    public <T> Observable<T> getAsObservable(String path, Type type) {
        return wrappedHttpResource.getAsObservable(path, type);
    }

    @Override
    public <T> Observable<T> getAsObservable(String path, Type type,
                                             Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return wrappedHttpResource.getAsObservable(path, type, paramsHeadersAndStuff);
    }

    @Override
    public <T> Observable<T> getAsObservable(String path, Function<String, T> extractor,
                                             Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return wrappedHttpResource.getAsObservable(path, extractor, paramsHeadersAndStuff);
    }

    @Override
    public <A, B> Observable<B> postAsObservable(String path, Type type, Entity<A> entity) {
        return wrappedHttpResource.postAsObservable(path, type, entity);
    }

    @Override
    public <A, B> Observable<B> postAsObservable(String path, Type type, Entity<A> entity,
                                                 Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return wrappedHttpResource.postAsObservable(path, type, entity, paramsHeadersAndStuff);
    }

    @Override
    public <A> Observable<Response> postAsObservable(String path, Entity<A> entity) {
        return wrappedHttpResource.postAsObservable(path, entity);
    }

    @Override
    public <A> Observable<Response> postAsObservable(String path, Entity<A> entity,
                                                     Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return wrappedHttpResource.postAsObservable(path, entity, paramsHeadersAndStuff);
    }

    @Override
    public <A, B> Observable<B> putAsObservable(String path, Type type, Entity<A> entity) {
        return wrappedHttpResource.putAsObservable(path, type, entity);
    }

    @Override
    public <A> Observable<Response> putAsObservable(String path, Entity<A> entity) {
        return wrappedHttpResource.putAsObservable(path, entity);
    }

    @Override
    public <A> Observable<Response> putAsObservable(String path, Entity<A> entity, Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return wrappedHttpResource.putAsObservable(path, entity, paramsHeadersAndStuff);
    }

    @Override
    public <A> Observable<A> deleteAsObservable(String path, final Type type, Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return wrappedHttpResource.deleteAsObservable(path, type, paramsHeadersAndStuff);
    }
}
