package fi.vm.sade.valinta.kooste;

public class DevApp {

  public static void main(String[] args) {
    // ssl-konfiguraatio
    System.setProperty("server.ssl.key-store-type", "PKCS12");
    System.setProperty("server.ssl.key-store", "classpath:keystore.p12");
    System.setProperty("server.ssl.key-store-password", "password");
    System.setProperty("server.ssl.key-alias", "defaultkey");
    System.setProperty("server.ssl.enabled", "true");
    System.setProperty("server.port", "8443");

    System.setProperty(
        "cas.service.valintalaskentakoostepalvelu",
        "https://localhost:8443/valintalaskentakoostepalvelu");
    System.setProperty("cas-service.sendRenew", "false");
    System.setProperty("cas-service.key", "valintalaskentakoostepalvelu");

    System.setProperty("spring.profiles.active", "dev");

    System.setProperty("host.virkailija", "virkailija.hahtuvaopintopolku.fi");
    System.setProperty(
        "valintalaskentakoostepalvelu.valintaperusteet-service.baseurl",
        "https://virkailija.hahtuvaopintopolku.fi");
    System.setProperty(
        "valintalaskentakoostepalvelu.valintalaskenta-laskenta-service.baseurl",
        "https://virkailija.hahtuvaopintopolku.fi");

    System.setProperty("url-ilb", "http://alb.hahtuvaopintopolku.fi:8888");
    System.setProperty("baseurl-sijoittelu-service", "http://alb.hahtuvaopintopolku.fi:8888");
    System.setProperty(
        "kayttooikeus-service.userDetails.byUsername",
        "https://virkailija.hahtuvaopintopolku.fi/kayttooikeus-service/userDetails/$1");

    App.start();
  }
}
