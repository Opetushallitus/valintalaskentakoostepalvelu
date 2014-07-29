package fi.vm.sade.valinta.kooste;

import org.apache.cxf.message.Message;

/**
 * 
 * @author Jussi Jartamo
 * 
 * @Deprecated Ainoastaan Cache-ongelmien poistotarkoituksiin.
 */
@Deprecated
public class SecurityTicketOutInterceptor extends
		AbstractSecurityTicketOutInterceptor<Message> {
	public SecurityTicketOutInterceptor() {
	}
}