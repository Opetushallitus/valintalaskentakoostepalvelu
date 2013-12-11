//package fi.vm.sade.valinta.kooste.kela.ftp;
//
//import java.io.IOException;
//import java.util.Properties;
//
//import org.junit.Ignore;
//import org.mockito.Mockito;
//import org.springframework.context.annotation.AnnotationConfigApplicationContext;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.ImportResource;
//import org.springframework.context.annotation.PropertySource;
//import org.springframework.core.io.ClassPathResource;
//
//import fi.vm.sade.koodisto.service.KoodiService;
//import fi.vm.sade.tarjonta.service.TarjontaPublicService;
//import fi.vm.sade.tarjonta.service.resources.HakuResource;
//import fi.vm.sade.valinta.kooste.haku.HakemusProxy;
//import fi.vm.sade.valinta.kooste.kela.komponentti.KelaExportKomponentti;
//import fi.vm.sade.valinta.kooste.kela.proxy.KelaFtpProxy;
//import fi.vm.sade.valinta.kooste.tarjonta.OrganisaatioProxy;
//
///**
// * 
// * @author Jussi Jartamo
// * 
// */
//@Ignore
//@Configuration
//@PropertySource({ "classpath:META-INF/kela.properties", "classpath:META-INF/valintalaskentakoostepalvelu.properties" })
//@ImportResource({ "classpath:tarjonta-test-context.xml", "classpath:META-INF/spring/context/kela-context.xml" })
//@ComponentScan(basePackageClasses = { KelaExportKomponentti.class })
//public class TeeKelaFTPSiirto {
//
//    @Bean
//    public HakemusProxy getHakemusProxy() {
//        return Mockito.mock(HakemusProxy.class);
//    }
//
//    @Bean
//    public OrganisaatioProxy getOrganisaatioProxy() {
//        return Mockito.mock(OrganisaatioProxy.class);
//    }
//
//    @Bean
//    public HakuResource getHakuResource() {
//        return Mockito.mock(HakuResource.class);
//    }
//
//    @Bean
//    public KoodiService getKoodiService() {
//        return Mockito.mock(KoodiService.class);
//    }
//
//    @Bean
//    public TarjontaPublicService getTarjontaPublicService() {
//        return Mockito.mock(TarjontaPublicService.class);
//    }
//
//    public static void main(String[] args) throws IOException, InterruptedException {
//        // fi.vm.sade.organisaatio.resource.OrganisaatioResource;
//        Properties props = System.getProperties();
//        props.put("socksProxyHost", "127.0.0.1");
//        props.put("socksProxyPort", "9090");
//        System.setProperties(props);
//        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TeeKelaFTPSiirto.class);
//        // HakukohdeResource h = context.getBean(HakukohdeResource.class);
//        // HakukohdeDTO h0 = h.getByOID("1.2.246.562.5.10069_03_583_2122");
//        // String h1 = new
//        // GsonBuilder().setPrettyPrinting().create().toJson(h0);
//
//        KelaFtpProxy ftpProxy = context.getBean(KelaFtpProxy.class);
//        ftpProxy.lahetaTiedosto("RO.WOT.SR.D131203.YHVA14",
//                new ClassPathResource("RO.WOT.SR.D131203.YHVA14").getInputStream());
//        Thread.currentThread().join();
//    }
// }
