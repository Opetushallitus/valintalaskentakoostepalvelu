package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import fi.vm.sade.service.valintaperusteet.schema.*;
import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeValintaperusteetDTO;
import fi.vm.sade.tarjonta.service.resources.dto.ValintakoeRDTO;
import fi.vm.sade.valinta.kooste.exception.KoodistoException;
import fi.vm.sade.valinta.kooste.external.resource.haku.KoodistoJsonRESTResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.KoodistoUrheilija;
import org.apache.camel.Body;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * User: wuoti Date: 20.5.2013 Time: 10.46
 */
//@Component("suoritaHakukohdeImportKomponentti")
@PreAuthorize("isAuthenticated()")
public class SuoritaHakukohdeImportKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(SuoritaHakukohdeImportKomponentti.class);

    @Autowired
    private HakukohdeResource hakukohdeResource;

    @Autowired
    private KoodistoJsonRESTResource koodistoJsonRESTResource;

    private String koodistoResourceUrl;

    private final String KOODISTO_HAKUKOHDE_URHEILIJAHAKU_SALLITTU = "urheilijankoulutus_1";

    public HakukohdeImportTyyppi suoritaHakukohdeImport(@Body //@Property(OPH.HAKUKOHDEOID)
                                                                String hakukohdeOid) {
        HakukohdeValintaperusteetDTO data = hakukohdeResource.getHakukohdeValintaperusteet(hakukohdeOid);
        HakukohdeImportTyyppi importTyyppi = new HakukohdeImportTyyppi();

        importTyyppi.setTarjoajaOid(data.getTarjoajaOid());

        if (data.getTarjoajaNimi() != null) {
            for (String s : data.getTarjoajaNimi().keySet()) {
                MonikielinenTekstiTyyppi m = new MonikielinenTekstiTyyppi();
                m.setLang(s);
                m.setText(data.getTarjoajaNimi().get(s));
                importTyyppi.getTarjoajaNimi().add(m);
            }
        }

        if (data.getHakukohdeNimi() != null) {
            for (String s : data.getHakukohdeNimi().keySet()) {
                MonikielinenTekstiTyyppi m = new MonikielinenTekstiTyyppi();
                m.setLang(s);
                m.setText(data.getHakukohdeNimi().get(s));
                importTyyppi.getHakukohdeNimi().add(m);
            }
        }

        if (data.getHakuKausi() != null) {
            for (String s : data.getHakuKausi().keySet()) {
                MonikielinenTekstiTyyppi m = new MonikielinenTekstiTyyppi();
                m.setLang(s);
                m.setText(data.getHakuKausi().get(s));
                importTyyppi.getHakuKausi().add(m);
            }
        }

        importTyyppi.setHakuVuosi(new Integer(data.getHakuVuosi()).toString());

        HakukohdekoodiTyyppi hkt = new HakukohdekoodiTyyppi();
        hkt.setKoodiUri(data.getHakukohdeNimiUri());
        importTyyppi.setHakukohdekoodi(hkt);

        importTyyppi.setHakukohdeOid(data.getOid());
        importTyyppi.setHakuOid(data.getHakuOid());
        importTyyppi.setValinnanAloituspaikat(data.getValintojenAloituspaikatLkm());
        importTyyppi.setTila(data.getTila());
        if (data.getValintakokeet() != null) {
            LOG.debug("Valintakokeita löytyi {}!", data.getValintakokeet().size());
            for (ValintakoeRDTO valinakoe : data.getValintakokeet()) {
                HakukohteenValintakoeTyyppi v = new HakukohteenValintakoeTyyppi();
                v.setOid(valinakoe.getOid());
                v.setTyyppiUri(valinakoe.getTyyppiUri());
                importTyyppi.getValintakoe().add(v);
            }
        }

        String hakukohdeKoodiTunniste = data.getOid().replaceAll("\\.", "_");

        AvainArvoTyyppi avainArvo = new AvainArvoTyyppi();

        avainArvo.setAvain("hakukohde_oid");
        avainArvo.setArvo(data.getOid());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("paasykoe_min");
        avainArvo.setArvo(data.getPaasykoeMin().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("paasykoe_max");
        avainArvo.setArvo(data.getPaasykoeMax().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("paasykoe_hylkays_min");
        avainArvo.setArvo(data.getPaasykoeHylkaysMin().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("paasykoe_hylkays_max");
        avainArvo.setArvo(data.getPaasykoeHylkaysMax().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("lisanaytto_min");
        avainArvo.setArvo(data.getLisanayttoMin().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("lisanaytto_max");
        avainArvo.setArvo(data.getLisanayttoMax().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("lisanaytto_hylkays_min");
        avainArvo.setArvo(data.getLisanayttoHylkaysMin().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("lisanaytto_hylkays_max");
        avainArvo.setArvo(data.getLisanayttoHylkaysMax().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("paasykoe_ja_lisanaytto_hylkays_min");
        avainArvo.setArvo(data.getHylkaysMin().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("paasykoe_ja_lisanaytto_hylkays_max");
        avainArvo.setArvo(data.getHylkaysMax().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("painotettu_keskiarvo_hylkays_min");
        avainArvo.setArvo(data.getPainotettuKeskiarvoHylkaysMin().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("painotettu_keskiarvo_hylkays_max");
        avainArvo.setArvo(data.getPainotettuKeskiarvoHylkaysMax().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        String nimiUri = data.getHakukohdeNimiUri().split("#")[0];

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("paasykoe_tunniste");
        avainArvo.setArvo(data.getPaasykoeTunniste() != null ? data.getPaasykoeTunniste() : hakukohdeKoodiTunniste
                + "_paasykoe");
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("lisanaytto_tunniste");
        avainArvo.setArvo(data.getLisanayttoTunniste() != null ? data.getLisanayttoTunniste() : hakukohdeKoodiTunniste
                + "_lisanaytto");
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("lisapiste_tunniste");
        avainArvo.setArvo(data.getLisapisteTunniste() != null ? data.getLisapisteTunniste() : hakukohdeKoodiTunniste
                + "_lisapiste");
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("urheilija_lisapiste_tunniste");
        avainArvo.setArvo(data.getUrheilijaLisapisteTunniste() != null ? data.getUrheilijaLisapisteTunniste() : hakukohdeKoodiTunniste
                + "_urheilija_lisapiste");
        importTyyppi.getValintaperuste().add(avainArvo);

        String opetuskieli = null;
        if (data.getOpetuskielet().size() > 0) {
            avainArvo = new AvainArvoTyyppi();
            opetuskieli = data.getOpetuskielet().get(0);
            avainArvo.setAvain("opetuskieli");
            avainArvo.setArvo(opetuskieli);
            importTyyppi.getValintaperuste().add(avainArvo);
        }


        // Kielikoetunnisteen selvittäminen
        String kielikoetunniste = null;
        if (StringUtils.isNotBlank(data.getKielikoeTunniste())) {
            kielikoetunniste = data.getKielikoeTunniste();
        } else if (StringUtils.isNotBlank(opetuskieli)) {
            kielikoetunniste = "kielikoe_" + opetuskieli;
        } else {
            kielikoetunniste = hakukohdeKoodiTunniste + "_kielikoe";
        }

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("kielikoe_tunniste");
        avainArvo.setArvo(kielikoetunniste);
        importTyyppi.getValintaperuste().add(avainArvo);

        for (String avain : data.getPainokertoimet().keySet()) {
            avainArvo = new AvainArvoTyyppi();
            avainArvo.setAvain(avain);
            avainArvo.setArvo(data.getPainokertoimet().get(avain));
            importTyyppi.getValintaperuste().add(avainArvo);
        }

        int versioNumero = Integer.parseInt(data.getHakukohdeNimiUri().split("#")[1]);
        LOG.debug("Haetaan alakoodit versiolla {}", new Object[]{versioNumero});
        List<KoodistoUrheilija> urheilijaList = null;
        try {
            urheilijaList = koodistoJsonRESTResource.getAlakoodis(nimiUri,versioNumero);
        } catch (Exception e) {
            LOG.error("Alakoodien haku koodistosta hakukohteelle "+nimiUri+" päättyi virheeseen");
            e.printStackTrace();
        }
        if (urheilijaList == null || urheilijaList.isEmpty()) {
            throw new KoodistoException("Koodisto ei palauttanut yhtään koodia hakukohteelle " + nimiUri);
        }
        LOG.debug("Haettiin {} kpl koodeja", urheilijaList.size());

        boolean urheilijaHaku = false;

        for (KoodistoUrheilija urheilija : urheilijaList) {
            LOG.debug("hakukohde: {} - alakoodi: {}", new Object[]{nimiUri, urheilija.getKoodiUri()});
            if(urheilija.getKoodiUri().equals(KOODISTO_HAKUKOHDE_URHEILIJAHAKU_SALLITTU)) {
                urheilijaHaku = true;
            }
        }

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("urheilija_haku_sallittu");
        avainArvo.setArvo(String.valueOf(urheilijaHaku));
        importTyyppi.getValintaperuste().add(avainArvo);

        return importTyyppi;
    }

}
