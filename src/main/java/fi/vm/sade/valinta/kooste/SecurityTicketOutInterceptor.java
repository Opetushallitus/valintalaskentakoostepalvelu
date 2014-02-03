package fi.vm.sade.valinta.kooste;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.SoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapPreProtocolOutInterceptor;

/**
 * 
 * @author Jussi Jartamo
 * 
 * @Deprecated Ainoastaan Cache-ongelmien poistotarkoituksiin.
 */
@Deprecated
public class SecurityTicketOutInterceptor extends
		AbstractSecurityTicketOutInterceptor<SoapMessage> implements
		SoapInterceptor {
	public SecurityTicketOutInterceptor() {
		super();
		getAfter().add(SoapPreProtocolOutInterceptor.class.getName());
	}

	@Override
	public Set<URI> getRoles() {
		return Collections.emptySet();
	}

	@Override
	public Set<QName> getUnderstoodHeaders() {
		return Collections.emptySet();
	}
}