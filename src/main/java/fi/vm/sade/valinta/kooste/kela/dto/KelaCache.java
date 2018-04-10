package fi.vm.sade.valinta.kooste.kela.dto;

import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.resources.KomotoResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakuDTO;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.dto.KomotoDTO;
import fi.vm.sade.tarjonta.service.resources.dto.OidRDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.kela.komponentti.HenkilotietoSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.PaivamaaraSource;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class KelaCache implements PaivamaaraSource, HenkilotietoSource {
    private static final Logger LOG = LoggerFactory.getLogger(KelaCache.class);
    private final ConcurrentHashMap<String, HakuDTO> haut = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, HakukohdeDTO> hakukohteet = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> hakutyyppiArvo = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> haunKohdejoukkoArvo = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Date> lukuvuosi = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, HenkiloPerustietoDto> henkilotiedot =new ConcurrentHashMap<>();
    
    private final Date now;
    private final HakukohdeResource hakukohdeResource;
    private final KomotoResource komotoResource;


    public KelaCache(HakukohdeResource hakukohdeResource,
            KomotoResource komotoResource) {
        now = new Date();
        this.hakukohdeResource = hakukohdeResource;
        this.komotoResource = komotoResource;
        
    }

    @Override
    public Date lukuvuosi(HakuV1RDTO hakuDTO, String hakukohdeOid) {
        if (!lukuvuosi.containsKey(hakukohdeOid)) {
            String alkamiskausiUri = hakuDTO.getKoulutuksenAlkamiskausiUri();
            int vuosi = hakuDTO.getKoulutuksenAlkamisVuosi();
            
            if (alkamiskausiUri == null) {
                // Kyseessä jatkuva haku, haetaan alkamistiedot koulutukselta
                try {
                    List<OidRDTO> komotoOids = hakukohdeResource.getKomotosByHakukohdeOID(hakukohdeOid);

                    for (OidRDTO komotoOid : komotoOids) {
                        try {
                            KomotoDTO komoto = komotoResource.getByOID(komotoOid.getOid());
                            alkamiskausiUri = komoto.getKoulutuksenAlkamiskausi();
                            vuosi = komoto.getKoulutuksenAlkamisvuosi();
                        } catch(Exception e) {
                            LOG.error("Komoton haku tai käsittely epäonnistui. komotoOid:" + komotoOid.getOid(), e);
                            throw new RuntimeException(e);
                        }
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
                LOG.error("Viallinen arvo {}, koodilla kausi ", new Object[]{alkamiskausiUri});
            }

            
            lukuvuosi.put(hakukohdeOid, new DateTime(vuosi, kuukausi, 1, 1, 1).toDate());
        }
        return lukuvuosi.get(hakukohdeOid);
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
    public HenkiloPerustietoDto getByPersonOid(String oid) {
        return henkilotiedot.get(oid);
    }

    public void put(HakuDTO haku) {
        haut.put(haku.getOid(), haku);
    }

    public void put(HakukohdeDTO hakukohde) {
        hakukohteet.put(hakukohde.getOid(), hakukohde);
    }

    public void put(HenkiloPerustietoDto tieto) {
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
