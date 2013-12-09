package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import org.apache.camel.Body;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.valintaperusteet.schema.AvainArvoTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.HakukohdeImportTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.HakukohdekoodiTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.HakukohteenValintakoeTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.MonikielinenTekstiTyyppi;
import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeValintaperusteetDTO;
import fi.vm.sade.tarjonta.service.resources.dto.ValintakoeRDTO;

/**
 * User: wuoti Date: 20.5.2013 Time: 10.46
 */
@Component("suoritaHakukohdeImportKomponentti")
@PreAuthorize("isAuthenticated()")
public class SuoritaHakukohdeImportKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(SuoritaHakukohdeImportKomponentti.class);

    @Autowired
    private HakukohdeResource hakukohdeResource;

    public HakukohdeImportTyyppi suoritaHakukohdeImport(@Body// @Property(OPH.HAKUKOHDEOID)
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

        AvainArvoTyyppi avainArvo = new AvainArvoTyyppi();

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

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("paasykoe_tunniste");
        avainArvo.setArvo(data.getPaasykoeTunniste() != null ? data.getPaasykoeTunniste() : importTyyppi
                .getHakukohdeNimi().get(0).getText()
                + "_paasykoe");
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("lisanaytto_tunniste");
        avainArvo.setArvo(data.getLisanayttoTunniste() != null ? data.getLisanayttoTunniste() : importTyyppi
                .getHakukohdeNimi().get(0).getText()
                + "_lisanaytto");
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("lisapiste_tunniste");
        avainArvo.setArvo(data.getLisapisteTunniste() != null ? data.getLisapisteTunniste() : importTyyppi
                .getHakukohdeNimi().get(0).getText()
                + "_lisapiste");
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoTyyppi();
        avainArvo.setAvain("kielikoe_tunniste");
        avainArvo.setArvo(data.getKielikoeTunniste() != null ? data.getKielikoeTunniste() : importTyyppi
                .getHakukohdeNimi().get(0).getText()
                + "_kielikoe");
        importTyyppi.getValintaperuste().add(avainArvo);

        for (String avain : data.getPainokertoimet().keySet()) {
            avainArvo = new AvainArvoTyyppi();
            avainArvo.setAvain(avain);
            avainArvo.setArvo(data.getPainokertoimet().get(avain));
            importTyyppi.getValintaperuste().add(avainArvo);
        }

        return importTyyppi;
    }

}
