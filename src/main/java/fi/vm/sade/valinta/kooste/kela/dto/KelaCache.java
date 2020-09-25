package fi.vm.sade.valinta.kooste.kela.dto;

import fi.vm.sade.tarjonta.service.resources.dto.HakuDTO;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.koulutus.KoulutusV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.kela.komponentti.HenkilotietoSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.PaivamaaraSource;
import fi.vm.sade.valinta.kooste.util.CompletableFutureUtil;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KelaCache implements PaivamaaraSource, HenkilotietoSource {
  private static final Logger LOG = LoggerFactory.getLogger(KelaCache.class);
  private final ConcurrentHashMap<String, HakuDTO> haut = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, HakukohdeDTO> hakukohteet = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, String> hakutyyppiArvo = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, String> haunKohdejoukkoArvo = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Date> lukuvuosi = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, HenkiloPerustietoDto> henkilotiedot =
      new ConcurrentHashMap<>();

  private TarjontaAsyncResource tarjontaAsyncResource;
  private final Date now;

  public KelaCache(TarjontaAsyncResource tarjontaAsyncResource) {
    now = new Date();
    this.tarjontaAsyncResource = tarjontaAsyncResource;
  }

  @Override
  public Date lukuvuosi(Haku hakuDTO, String hakukohdeOid) {
    if (!lukuvuosi.containsKey(hakukohdeOid)) {
      String alkamiskausiUri = hakuDTO.koulutuksenAlkamiskausiUri;
      Integer vuosi = hakuDTO.koulutuksenAlkamisvuosi;

      if (alkamiskausiUri == null) {
        // Kyseessä jatkuva haku, haetaan alkamistiedot koulutukselta
        try {
          List<KoulutusV1RDTO> toteutukset =
              tarjontaAsyncResource
                  .haeHakukohde(hakukohdeOid)
                  .thenComposeAsync(
                      hakukohde ->
                          CompletableFutureUtil.sequence(
                              hakukohde.toteutusOids.stream()
                                  .map(tarjontaAsyncResource::haeToteutus)
                                  .collect(Collectors.toList())))
                  .get(5, TimeUnit.MINUTES);
          for (KoulutusV1RDTO toteutus : toteutukset) {
            alkamiskausiUri = toteutus.getKoulutuksenAlkamiskausi().getUri();
            vuosi = toteutus.getKoulutuksenAlkamisvuosi();
          }
        } catch (Exception e) {
          LOG.error("Ei voitu hakea lukuvuotta tarjonnalta. HakukohdeOid:" + hakukohdeOid, e);
          throw new RuntimeException(e);
        }
      }

      int kuukausi = 1;

      if (alkamiskausiUri != null && alkamiskausiUri.startsWith("kausi_s")) {
        kuukausi = 8;
      } else if (alkamiskausiUri != null && alkamiskausiUri.startsWith("kausi_k")) {
        kuukausi = 1;
      } else {
        LOG.error("Viallinen arvo {}, koodilla kausi ", alkamiskausiUri);
      }

      lukuvuosi.put(hakukohdeOid, new DateTime(vuosi, kuukausi, 1, 1, 1).toDate());
    }
    return lukuvuosi.get(hakukohdeOid);
  }

  @Override
  public Date poimintapaivamaara(Haku haku) {
    return now;
  }

  @Override
  public Date valintapaivamaara(Haku haku) {
    return now;
  }

  @Override
  public HenkiloPerustietoDto getByPersonOid(String oid) {
    return henkilotiedot.get(oid);
  }

  public void put(HakuDTO haku) {
    haut.put(haku.getOid(), haku);
  }

  public void put(HakukohdeDTO hakukohde) {
    hakukohteet.put(hakukohde.getOid(), hakukohde);
  }

  public void put(String oid, HenkiloPerustietoDto tieto) {
    henkilotiedot.put(oid, tieto);
    henkilotiedot.put(tieto.getOidHenkilo(), tieto);
  }

  public boolean containsHakutyyppi(String hakutyyppi) {
    return hakutyyppiArvo.containsKey(hakutyyppi);
  }

  public void putHakutyyppi(String hakutyyppi, String arvo) {
    hakutyyppiArvo.put(hakutyyppi, arvo);
  }

  public String getHakutyyppi(String hakutyyppi) {
    return hakutyyppiArvo.get(hakutyyppi);
  }

  public boolean containsHaunKohdejoukko(String kohdejoukko) {
    return haunKohdejoukkoArvo.containsKey(kohdejoukko);
  }

  public void putHaunKohdejoukko(String kohdejoukko, String arvo) {
    haunKohdejoukkoArvo.put(kohdejoukko, arvo);
  }

  public String getHaunKohdejoukko(String kohdejoukko) {
    return haunKohdejoukkoArvo.get(kohdejoukko);
  }
}
