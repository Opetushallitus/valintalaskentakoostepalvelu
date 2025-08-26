package fi.vm.sade.valinta.kooste;

public class DevApp {

  private static final String ENVIRONMENT = "hahtuva";

  public static void main(String[] args) {
    // ssl-konfiguraatio
    System.setProperty("server.ssl.key-store-type", "PKCS12");
    System.setProperty("server.ssl.key-store", "classpath:keystore.p12");
    System.setProperty("server.ssl.key-store-password", "password");
    System.setProperty("server.ssl.key-alias", "defaultkey");
    System.setProperty("server.ssl.enabled", "true");
    System.setProperty("server.port", "8443");

    // System.setProperty(
    //    "cas.service.valintalaskentakoostepalvelu",
    //    String.format(
    //        "https://virkailija.%sopintopolku.fi/valintalaskentakoostepalvelu", ENVIRONMENT));
    System.setProperty(
        "cas.service.valintalaskentakoostepalvelu",
        String.format("https://localhost:8443/valintalaskentakoostepalvelu", ENVIRONMENT));
    System.setProperty("cas-service.sendRenew", "false");
    System.setProperty("cas-service.key", "valintalaskentakoostepalvelu");

    System.setProperty("spring.profiles.active", "dev");

    System.setProperty(
        "host.virkailija", String.format("virkailija.%sopintopolku.fi", ENVIRONMENT));
    System.setProperty(
        "valintalaskentakoostepalvelu.valintaperusteet-service.baseurl",
        String.format("https://virkailija.%sopintopolku.fi", ENVIRONMENT));
    System.setProperty(
        "valintalaskentakoostepalvelu.valintalaskenta-laskenta-service.baseurl",
        String.format("https://virkailija.%sopintopolku.fi", ENVIRONMENT));

    System.setProperty("url-ilb", String.format("http://alb.%sopintopolku.fi:8888", ENVIRONMENT));
    System.setProperty(
        "baseurl-sijoittelu-service",
        String.format("http://alb.%sopintopolku.fi:8888", ENVIRONMENT));
    System.setProperty(
        "baseurl-viestintapalvelu", String.format("http://alb.%sopintopolku.fi:8888", ENVIRONMENT));
    System.setProperty(
        "kayttooikeus-service.userDetails.byUsername",
        String.format(
            "http://alb.%sopintopolku.fi:8888/kayttooikeus-service/userDetails/$1", ENVIRONMENT));

    System.setProperty("aws.region", "eu-west-1");
    System.setProperty("aws.bucket.name", "opintopolku-local-dokumenttipalvelu");

    System.setProperty("server.servlet.session.timeout", "60m");

    System.setProperty(
        "valintalaskentakoostepalvelu.postgresql.url",
        "jdbc:postgresql://localhost:5433/valintalaskentakoostepalvelu");
    System.setProperty("valintalaskentakoostepalvelu.postgresql.driver", "");

    TempDockerDB.start();
    App.start();
  }
}
