package fi.vm.sade.valinta.esb;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;

public class MulticastStrategy implements AggregationStrategy {

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
       /*
        AggregationDTO aggregationDTO = null;

        if (oldExchange == null) {
            aggregationDTO = new AggregationDTO();
            newExchange.getIn().setHeader("aggregationDTO", aggregationDTO);
        } else {
            aggregationDTO = (AggregationDTO) oldExchange.getIn().getHeader("aggregationDTO");
            newExchange.getIn().setHeader("aggregationDTO", aggregationDTO);
        }

        // This blows up otherwise.
        if(newExchange.getIn().getBody() instanceof CxfPayload) {
            return newExchange;
        }

        String operation = (String) newExchange.getIn().getHeader("operationname");
        String body = newExchange.getIn().getBody(String.class);

        if ("listMyonnettyKayttoOikeus".equals(operation)) {
            ListMyonnettyKayttoOikeusResponseType resp = unmarshal(ListMyonnettyKayttoOikeusResponseType.class, body);
            aggregationDTO.getMyonnettyKayttoOikeus().addAll(resp.getMyonnettyKayttoOikeus());
        } else if ("listMyonnettyKayttoOikeusRyhmas".equals(operation)) {
            ListMyonnettyKayttoOikeusRyhmasResponseType resp = unmarshal(ListMyonnettyKayttoOikeusRyhmasResponseType.class,
                    body);
            aggregationDTO.getMyonnettyKayttoOikeusRyhmas().addAll(resp.getMyonnettyKayttoOikeusRyhma());
        } else if ("listMyonnettyKayttoOikeusRyhmaTapahtumas".equals(operation)) {
            ListMyonnettyKayttoOikeusRyhmaTapahtumasResponseType resp = unmarshal(
                    ListMyonnettyKayttoOikeusRyhmaTapahtumasResponseType.class, body);
            aggregationDTO.getMyonnettyKayttoOikeusRyhmaTapahtumas().addAll(
                    resp.getMyonnettyKayttoOikeusRyhmaTapahtuma());
        } else if ("listKayttoOikeus".equals(operation)) {
            ListKayttoOikeusResponseType resp = unmarshal(ListKayttoOikeusResponseType.class, body);
            aggregationDTO.getKayttoOikeusTypes().addAll(resp.getKayttoOikeus());
        } else if ("listKayttoOikeusRyhmas".equals(operation)) {
            ListKayttoOikeusRyhmasResponseType resp = unmarshal(ListKayttoOikeusRyhmasResponseType.class, body);
            aggregationDTO.getKayttoOikeusRyhmaTypes().addAll(resp.getKayttoOikeusRyhma());
        }
         */
        return newExchange;
    }
   /*
    private <T> T unmarshal(Class<T> root, String body) {
        try {
            JAXBContext jc = JAXBContext.newInstance(root);
            Unmarshaller m = jc.createUnmarshaller();
            StreamSource s = new StreamSource(new StringReader(body));
            JAXBElement<T> resp = m.unmarshal(s, root);
            return resp.getValue();
        } catch (JAXBException e) {
            LOGGER.error("Error when trying to unmarshal String [" + body + "] to class [" + root + "]");
            throw new RuntimeException("Error when trying to unmarshal String [" + body + "] to class [" + root + "]",
                    e);
        }
    }
    */
    // private Exchange processListKayttoOikeusRyhmas(Exchange newExchange) {
    // try {
    // AggregationDTO aggregationDTO = (AggregationDTO)
    // newExchange.getIn().getHeader("aggregationDTO");
    //
    // String inputXml = (String) newExchange.getIn().getBody();
    // InputSource inputXmlSource = new InputSource(new StringReader(inputXml));
    //
    // XPathFactory factory = XPathFactory.newInstance();
    // XPath xpath = factory.newXPath();
    //
    // XPathExpression expr1 = xpath.compile("//kayttoOikeusRyhma");
    // XPathExpression expr2 = xpath.compile("./id/text()");
    // XPathExpression expr3 = xpath.compile("./name/text()");
    // XPathExpression expr4 = xpath.compile("./description");
    // XPathExpression expr5 = xpath.compile("./lang/text()");
    // XPathExpression expr6 = xpath.compile("./text/text()");
    //
    // Object result = expr1.evaluate(inputXmlSource, XPathConstants.NODESET);
    // NodeList nodes = (NodeList) result;
    //
    // for (int i = 0; i < nodes.getLength(); i++) {
    //
    // String id = ((Node) expr2.evaluate(nodes.item(i),
    // XPathConstants.NODE)).getNodeValue();
    // String name = ((Node) expr3.evaluate(nodes.item(i),
    // XPathConstants.NODE)).getNodeValue();
    //
    // KayttoOikeusRyhmaType dto = new KayttoOikeusRyhmaType();
    // dto.setId(Long.parseLong(id));
    // dto.setName(name);
    // // dto.setTila(KayttoOikeudenTilaType.valueOf(tila));
    // // dto.setVoimassaAlkuPvm(value);
    // // dto.setVoimassaLoppuPvm(value);
    // aggregationDTO.getKayttoOikeusRyhmaTypes().add(dto);
    // }
    //
    // return newExchange;
    // } catch (XPathExpressionException e) {
    // throw new RuntimeException("XPATH failed", e);
    // }
    // }

    // private Exchange processListKayttooikeus(Exchange newExchange) {
    // try {
    //
    // AggregationDTO aggregationDTO = (AggregationDTO)
    // newExchange.getIn().getHeader("aggregationDTO");
    //
    // String inputXml = (String) newExchange.getIn().getBody();
    // InputSource inputXmlSource = new InputSource(new StringReader(inputXml));
    //
    // XPathFactory factory = XPathFactory.newInstance();
    // XPath xpath = factory.newXPath();
    //
    // XPathExpression expr1 = xpath.compile("//kayttoOikeus");
    // XPathExpression expr2 = xpath.compile("./id/text()");
    // // XPathExpression expr3 = xpath.compile("./tila/text()");
    // // XPathExpression expr4 =
    // // xpath.compile("./voimassaAlkuPvm/text()");
    // // XPathExpression expr5 =
    // // xpath.compile("./voimassaLoppuPvm/text()");
    // Object result = expr1.evaluate(inputXmlSource, XPathConstants.NODESET);
    // NodeList nodes = (NodeList) result;
    //
    // for (int i = 0; i < nodes.getLength(); i++) {
    //
    // String id = ((Node) expr2.evaluate(nodes.item(i),
    // XPathConstants.NODE)).getNodeValue();
    // KayttoOikeusType dto = new KayttoOikeusType();
    // dto.setId(Long.parseLong(id));
    // aggregationDTO.getKayttoOikeusTypes().add(dto);
    // }
    //
    // return newExchange;
    // } catch (XPathExpressionException e) {
    // throw new RuntimeException("XPATH failed", e);
    // }
    //
    // }

    // private Exchange processMyonnettyKayttoOikeusDTO(Exchange newExchange) {
    // try {
    // AggregationDTO aggregationDTO = (AggregationDTO)
    // newExchange.getIn().getHeader("aggregationDTO");
    //
    // String inputXml = (String) newExchange.getIn().getBody();
    // InputSource inputXmlSource = new InputSource(new StringReader(inputXml));
    //
    // XPathFactory factory = XPathFactory.newInstance();
    // XPath xpath = factory.newXPath();
    //
    // XPathExpression expr1 = xpath.compile("//MyonnettyKayttoOikeusType");
    // XPathExpression expr2 = xpath.compile("./kayttoOikeusId/text()");
    // XPathExpression expr3 = xpath.compile("./tila/text()");
    // XPathExpression expr4 = xpath.compile("./voimassaAlkuPvm/text()");
    // XPathExpression expr5 = xpath.compile("./voimassaLoppuPvm/text()");
    // Object result = expr1.evaluate(inputXmlSource, XPathConstants.NODESET);
    // NodeList nodes = (NodeList) result;
    //
    // for (int i = 0; i < nodes.getLength(); i++) {
    //
    // String kayttoOikeusId = ((Node) expr2.evaluate(nodes.item(i),
    // XPathConstants.NODE)).getNodeValue();
    // String tila = ((Node) expr3.evaluate(nodes.item(i),
    // XPathConstants.NODE)).getNodeValue();
    // String voimassaAlkuPvm = ((Node) expr4.evaluate(nodes.item(i),
    // XPathConstants.NODE)).getNodeValue();
    // String voimassaLoppuPvm = ((Node) expr5.evaluate(nodes.item(i),
    // XPathConstants.NODE)).getNodeValue();
    //
    // MyonnettyKayttoOikeusType dto = new MyonnettyKayttoOikeusType();
    // dto.setKayttoOikeusId(Long.parseLong(kayttoOikeusId));
    // dto.setTila(KayttoOikeudenTilaType.valueOf(tila));
    // // dto.setVoimassaAlkuPvm(value);
    // // dto.setVoimassaLoppuPvm(value);
    // aggregationDTO.getMyonnettyKayttoOikeus().add(dto);
    // }
    //
    // return newExchange;
    // } catch (XPathExpressionException e) {
    // throw new RuntimeException("XPATH failed", e);
    // }
    // }

    // private Exchange processMyonnettyKayttoOikeusRyhmasDTO(Exchange
    // newExchange) {
    // try {
    // AggregationDTO aggregationDTO = (AggregationDTO)
    // newExchange.getIn().getHeader("aggregationDTO");
    //
    // String inputXml = (String) newExchange.getIn().getBody();
    // InputSource inputXmlSource = new InputSource(new StringReader(inputXml));
    //
    // XPathFactory factory = XPathFactory.newInstance();
    // XPath xpath = factory.newXPath();
    //
    // XPathExpression expr1 = xpath.compile("//MyonnettyKayttoOikeusRyhmaType");
    // XPathExpression expr2 = xpath.compile("./kayttoOikeusRyhmaId/text()");
    // XPathExpression expr3 = xpath.compile("./tila/text()");
    // XPathExpression expr4 = xpath.compile("./voimassaAlkuPvm/text()");
    // XPathExpression expr5 = xpath.compile("./voimassaLoppuPvm/text()");
    // Object result = expr1.evaluate(inputXmlSource, XPathConstants.NODESET);
    // NodeList nodes = (NodeList) result;
    //
    // for (int i = 0; i < nodes.getLength(); i++) {
    //
    // String kayttoOikeusId = ((Node) expr2.evaluate(nodes.item(i),
    // XPathConstants.NODE)).getNodeValue();
    // String tila = ((Node) expr3.evaluate(nodes.item(i),
    // XPathConstants.NODE)).getNodeValue();
    // String voimassaAlkuPvm = ((Node) expr4.evaluate(nodes.item(i),
    // XPathConstants.NODE)).getNodeValue();
    // String voimassaLoppuPvm = ((Node) expr5.evaluate(nodes.item(i),
    // XPathConstants.NODE)).getNodeValue();
    //
    // MyonnettyKayttoOikeusRyhmaType dto = new MyonnettyKayttoOikeusRyhmaType();
    // dto.setKayttoOikeusRyhmaId(Long.parseLong(kayttoOikeusId));
    // dto.setTila(KayttoOikeudenTilaType.valueOf(tila));
    // // dto.setVoimassaAlkuPvm(value);
    // // dto.setVoimassaLoppuPvm(value);
    // aggregationDTO.getMyonnettyKayttoOikeusRyhmas().add(dto);
    // }
    //
    // return newExchange;
    // } catch (XPathExpressionException e) {
    // throw new RuntimeException("XPATH failed", e);
    // }
    // }

}
