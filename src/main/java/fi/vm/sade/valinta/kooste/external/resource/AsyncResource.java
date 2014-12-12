package fi.vm.sade.valinta.kooste.external.resource;

import java.util.List;

import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public abstract class AsyncResource {
	protected final Logger LOG = LoggerFactory.getLogger(getClass());
	protected final WebClient webClient;
	protected final String address;

	public AsyncResource(String address, long timeoutMillis) {
		this(address, getJaxrsClientFactoryBean(address).createWebClient(), timeoutMillis);
	}

	public AsyncResource(String address, WebClient webClient, long timeoutMillis) {
		this.webClient = webClient;
		this.address = address;
		ClientConfiguration c = WebClient.getConfig(webClient);
		c.getHttpConduit().getClient().setReceiveTimeout(timeoutMillis);
	}

	protected static JAXRSClientFactoryBean getJaxrsClientFactoryBean(final String address) {
		JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
		bean.setAddress(address);
		bean.setThreadSafe(true);
		List<Object> providers = Lists.newArrayList();
		providers.add(new com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider());
		providers.add(new fi.vm.sade.valinta.kooste.ObjectMapperProvider());
		bean.setProviders(providers);
		return bean;
	}
}
