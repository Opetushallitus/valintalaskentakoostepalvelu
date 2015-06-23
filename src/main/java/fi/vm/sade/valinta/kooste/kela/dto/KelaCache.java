package fi.vm.sade.valinta.kooste.kela.dto;

import static fi.vm.sade.valinta.kooste.util.TarjontaUriToKoodistoUtil.toSearchCriteria;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.koodisto.service.types.common.KoodiType;
import fi.vm.sade.tarjonta.service.resources.dto.HakuDTO;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.kela.komponentti.HakemusSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.PaivamaaraSource;

public class KelaCache implements HakemusSource, PaivamaaraSource {
    private static final Logger LOG = LoggerFactory.getLogger(KelaCache.class);
    private final ConcurrentHashMap<String, HakuDTO> haut = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, HakukohdeDTO> hakukohteet = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Hakemus> hakemukset = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> hakutyyppiArvo = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> haunKohdejoukkoArvo = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Date> lukuvuosi = new ConcurrentHashMap<>();
    private final Date now;
    private final KoodiService koodiService;


    public KelaCache(KoodiService koodiService) {
        now = new Date();
        this.koodiService = koodiService;
    }

    @Override
    public Date lukuvuosi(HakuV1RDTO hakuDTO) {
        String uri = hakuDTO.getKoulutuksenAlkamiskausiUri();
        if (uri == null) {
            LOG.error("Koulutuksen alkamiskausi URI oli null!");
            throw new RuntimeException("Koulutuksen alkamiskausi URI oli null!");
        }
        if (!lukuvuosi.containsKey(uri)) {
            int vuosi = hakuDTO.getKoulutuksenAlkamisVuosi();
            int kuukausi = 1;
            List<KoodiType> koodis;
            // haku.get

            int tries = 0;

            while (true) {
                try {
                    koodis = koodiService.searchKoodis(toSearchCriteria(hakuDTO.getKoulutuksenAlkamiskausiUri()));
                    if (tries > 0) {
                        LOG.error("retry ok");
                    }
                    break;
                } catch (Exception e) {
                    if (tries == 30) {
                        LOG.error("give up");
                        throw e;
                    }
                    tries++;
                    LOG.error("koodiService ei jaksa palvella {}. Yritetään vielä uudestaan. " + tries + "/30...", e.getMessage());
                    try {
                        Thread.sleep(15000L);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }


            try {
                for (KoodiType koodi : koodis) {
                    if ("S".equals(StringUtils.upperCase(koodi.getKoodiArvo()))) {

                        kuukausi = 8;
                    } else if ("K".equals(StringUtils.upperCase(koodi
                            .getKoodiArvo()))) {
                        kuukausi = 1;
                    } else {
                        LOG.error("Viallinen arvo {}, koodilla {} ", new Object[]{koodi.getKoodiArvo(), hakuDTO.getKoulutuksenAlkamiskausiUri()});
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                LOG.error("Ei voitu hakea lukuvuotta tarjonnalta syystä {}", e.getMessage());
                throw new RuntimeException(e);
            }
            lukuvuosi.put(uri, new DateTime(vuosi, kuukausi, 1, 1, 1).toDate());
        }
        return lukuvuosi.get(uri);
    }

    @Override
    public Date poimintapaivamaara(HakuV1RDTO haku) {
        return now;
    }

    @Override
    public Date valintapaivamaara(HakuV1RDTO haku) {
        return now;
    }

    @Override
    public Hakemus getHakemusByOid(String oid) {
        return hakemukset.get(oid);
    }

    public void put(HakuDTO haku) {
        haut.put(haku.getOid(), haku);
    }

    public void put(Hakemus hakemus) {
        hakemukset.put(hakemus.getOid(), hakemus);
    }

    public void put(HakukohdeDTO hakukohde) {
        hakukohteet.put(hakukohde.getOid(), hakukohde);
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
