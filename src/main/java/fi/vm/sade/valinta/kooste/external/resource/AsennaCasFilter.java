package fi.vm.sade.valinta.kooste.external.resource;

import java.util.List;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.message.Message;
import org.springframework.context.ApplicationContext;

import com.google.common.collect.Lists;

import fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class AsennaCasFilter {
	public static JAXRSClientFactoryBean asennaCasFilter(
			String webCasUrl,
			String targetService,
			String appClientUsername,
			String appClientPassword,
			JAXRSClientFactoryBean bean,
			ApplicationContext context) {
		if("default".equalsIgnoreCase(System.getProperty("spring.profiles.active", "default"))) {
			List<Interceptor<? extends Message>> interceptors = Lists
					.newArrayList();
			CasApplicationAsAUserInterceptor cas = new CasApplicationAsAUserInterceptor();
			cas.setWebCasUrl(webCasUrl);
			cas.setTargetService(targetService);
			cas.setAppClientUsername(appClientUsername);
			cas.setAppClientPassword(appClientPassword);
			interceptors.add(cas);
			bean.setOutInterceptors(interceptors);
		}
		return bean;
	}
}
