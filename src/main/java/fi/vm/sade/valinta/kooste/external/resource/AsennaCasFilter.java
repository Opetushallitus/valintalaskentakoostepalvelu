package fi.vm.sade.valinta.kooste.external.resource;

import java.util.Arrays;
import java.util.List;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.message.Message;
import org.springframework.context.ApplicationContext;

import com.google.common.collect.Lists;

import fi.vm.sade.authentication.cas.CasApplicationAsAUserInterceptor;
import fi.vm.sade.authentication.cas.CasFriendlyCxfInterceptor;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class AsennaCasFilter {

	public static void asennaCasFilter(
			String webCasUrl,
			String targetService,
			String appClientUsername,
			String appClientPassword,
			JAXRSClientFactoryBean bean,
			ApplicationContext context) {
		CasFriendlyCxfInterceptor<?> cas = context.getBean(CasFriendlyCxfInterceptor.class);
		
		cas.setAppClientUsername(appClientUsername);
		cas.setAppClientPassword(appClientPassword);
		bean.setOutInterceptors(Arrays.asList(cas));
		bean.setInInterceptors(Arrays.asList(cas));
	}
}
