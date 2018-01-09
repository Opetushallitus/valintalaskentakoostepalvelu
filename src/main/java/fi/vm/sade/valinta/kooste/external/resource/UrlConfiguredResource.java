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

    /**
     * @deprecated use {@link #getAsObservableLazily(String, Type)}
     */
    @Override
    public <T> Observable<T> getAsObservable(String path, Type type) {
        return wrappedHttpResource.getAsObservable(path, type);
    }

    /**
     * @deprecated use {@link #getAsObservableLazily(String, Type, Function)}
     */
    @Override
    public <T> Observable<T> getAsObservable(String path, Type type,
                                             Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return wrappedHttpResource.getAsObservable(path, type, paramsHeadersAndStuff);
    }

    /**
     * @deprecated use {@link #getAsObservableLazily(String, Function, Function)}
     */
    @Override
    public <T> Observable<T> getAsObservable(String path, Function<String, T> extractor,
                                             Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return wrappedHttpResource.getAsObservable(path, extractor, paramsHeadersAndStuff);
    }

    /**
     * @deprecated use {@link #postAsObservableLazily(String, Type, Entity)}
     */
    @Override
    public <A, B> Observable<B> postAsObservable(String path, Type type, Entity<A> entity) {
        return wrappedHttpResource.postAsObservable(path, type, entity);
    }

    /**
     * @deprecated use {@link #postAsObservableLazily(String, Type, Entity, Function)}
     */
    @Override
    public <A, B> Observable<B> postAsObservable(String path, Type type, Entity<A> entity,
                                                 Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return wrappedHttpResource.postAsObservable(path, type, entity, paramsHeadersAndStuff);
    }

    @Override
    public Observable<Response> getAsObservableLazily(String path) {
        return wrappedHttpResource.getAsObservableLazily(path);
    }

    @Override
    public Observable<Response> getAsObservableLazily(String path, Function<WebClient, WebClient> paramsHeadersAndStuff) {
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
    public <T> Observable<T> getAsObservableLazily(String path, Type type, Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return wrappedHttpResource.getAsObservableLazily(path, type, paramsHeadersAndStuff);
    }

    @Override
    public <T> Observable<T> getAsObservableLazily(String path, Function<String, T> extractor, Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return wrappedHttpResource.getAsObservableLazily(path, extractor, paramsHeadersAndStuff);
    }

    @Override
    public <A, B> Observable<B> postAsObservableLazily(String path, Type type, Entity<A> entity) {
        return wrappedHttpResource.postAsObservableLazily(path, type, entity);
    }

    @Override
    public <A, B> Observable<B> postAsObservableLazily(String path, Type type, Entity<A> entity, Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return wrappedHttpResource.postAsObservableLazily(path, type, entity, paramsHeadersAndStuff);
    }

    @Override
    public <A> Observable<Response> postAsObservableLazily(String path, Entity<A> entity) {
        return wrappedHttpResource.postAsObservableLazily(path, entity);
    }

    @Override
    public <A> Observable<Response> postAsObservableLazily(String path, Entity<A> entity, Function<WebClient, WebClient> paramsHeadersAndStuff) {
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
    public <A> Observable<Response> putAsObservableLazily(String path, Entity<A> entity, Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return wrappedHttpResource.putAsObservableLazily(path, entity, paramsHeadersAndStuff);
    }

    @Override
    public <A, B> Observable<B> putAsObservableLazily(String path, Type type, Entity<A> entity, Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return wrappedHttpResource.putAsObservableLazily(path, type, entity, paramsHeadersAndStuff);
    }

    @Override
    public <A> Observable<A> deleteAsObservableLazily(String path, Type type, Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return wrappedHttpResource.deleteAsObservableLazily(path, type, paramsHeadersAndStuff);
    }
}
