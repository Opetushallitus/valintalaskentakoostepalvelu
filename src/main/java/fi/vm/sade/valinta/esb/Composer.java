package fi.vm.sade.valinta.esb;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class Composer implements Processor {

  //  private static final Logger LOGGER = LoggerFactory.getLogger(AccessRightHistoryComposer.class);

   // private final static QName _ListKayttoOikeusHistoryForHenkiloResponse_QNAME = new QName(
   //         "http://service.authentication.sade.vm.fi/types", "listKayttoOikeusHistoryForHenkiloResponse");

    @Override
    public void process(Exchange exchange) {


    }



    /**
     * HACK, our generated classes lack @rootelement
     * 
     * @param value
     * @return

    private JAXBElement<ListKayttoOikeusHistoryForHenkiloResponseType> createListKayttoOikeusHistoryForHenkiloResponse(
            ListKayttoOikeusHistoryForHenkiloResponseType value) {
        return new JAXBElement<ListKayttoOikeusHistoryForHenkiloResponseType>(
                _ListKayttoOikeusHistoryForHenkiloResponse_QNAME, ListKayttoOikeusHistoryForHenkiloResponseType.class,
                null, value);
    }
     */


}
