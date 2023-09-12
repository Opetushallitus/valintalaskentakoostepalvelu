# Valintalaskentakoostepalvelu

## Testien ajaminen

Projektissa käytetään tällä hetkellä Javan versiota 17. Riippuvuuksina käytetään kuitenkin kirjastoja jotka eivät ole Java-17 -yhteensopivia, koska ne käsittelevät reflektiolla JDK:n luokkia. Testien
ajamiseksi (ja sovelluksen käynnistämiseksi) JVM:lle pitää antaa seuraavat parametrit:

`--add-opens java.base/java.util=ALL-UNNAMED`

Mavenin osalta kyseiset parametrit on jo lisätty, mutta kun koodia ajetaan IDE:n kautta, niin kyseiset parametrit on lisättävä esim.
IntelliJ:n Run Configuraatioihin (VM-parametreina).

## Ajaminen lokaalisti hahtuvan palveluita vasten

Joissakin tapauksissa on tarpeellista ajaa ympäristöä lokaalisti jonkin jaetun kehitysympäristön datalla, esim. virheiden
selvittämistä varten. Tämä onnistuu seuraavilla ohjeilla.

1. Kopioi valintalaskentapalvelun hahtuva-kontilta (ecs-exec) common.properties-tiedoston polkuun `/src/test/resources/application-dev.properties`. Tämä tiedosto
on ignoroitu etteivät salasanat valu repoon (kannattaa varmistaa).


2. Koska käytetään hahtuva-ympäristön palveluita, tarvitaan ssh-porttiohjaus:

         Lisää hosts-tiedostoon seuraava rivi:

         `127.0.0.1       alb.hahtuvaopintopolku.fi`

         Porttiohjaus käynnistetään seuraavalla komennolla (vaatii todennäköisesti VPN:n)

         `ssh -L 8888:alb.hahtuvaopintopolku.fi:80 <ssh-tunnus>@bastion.hahtuvaopintopolku.fi`


3. Lisää tarvittavat JVM-parametrit, mene Run -> Edit Configurations -> Valitse DevApp.java -> Modify Options -> Add VM Options
   Ja lisää:

   `--add-opens java.base/java.util=ALL-UNNAMED`


5. Käynnistä Ideassa ```DevApp.java``` (right-click -> Run), ja avaa selaimessa esim. allaoleva osoite (uudelleenohjaa aluksi hahtuva-autentikointiin):

         `https://localhost:8443/valintalaskentakoostepalvelu/resources/parametrit/hakukohderyhmat/1.2.246.562.20.00000000000000015541`

6. Valintalaskenta voidaan käynnistää esim. seuraavalla komennolla (korvaa sessioid, validin session saa selaimesta esim. kun tekee kohdan 5. kutsun):

         `curl --insecure -H "Cookie: JSESSIONID=<validi sessioid>" -H "Content-Type: application/json" -X POST --data "[\"1.2.246.562.20.00000000000000015541\"]" "https://localhost:8443/valintalaskentakoostepalvelu/resources/valintalaskentakerralla/haku/1.2.246.562.29.00000000000000012989/tyyppi/HAKUKOHDE/whitelist/true?erillishaku=false&haunnimi=Korkeakoulujen+yhteishaku+syksy+2022&nimi=Haaga-Helia+ammattikorkeakoulu,+Porvoon+kampus:+Restonomi+(AMK),+matkailu-+ja+tapahtuma-ala,+monimuotototeutus,+Porvoo&valinnanvaihe=0&valintakoelaskenta=false" -v`

7. Sijoittelulle vastaava komento on:

         `curl --insecure -H "Cookie: JSESSIONID=<validi sessioid>" -H "Content-Type: application/json" -X POST --data "\{\}" https://localhost:8443/valintalaskentakoostepalvelu/resources/koostesijoittelu/aktivoi\?hakuOid=1.2.246.562.29.00000000000000021303 -v`


### Swagger endpoint

Swagger löytyy osoitteesta [https://localhost:8443/valintalaskentakoostepalvelu/swagger-ui/index.html](https://localhost:8443/valintalaskentakoostepalvelu/swagger-ui/index.html).
