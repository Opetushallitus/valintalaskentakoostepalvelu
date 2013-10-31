package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.schema.HakukohdeImportTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.HakukohdekoodiTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.HakukohteenValintakoeTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.MonikielinenTekstiTyyppi;
import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;
import fi.vm.sade.tarjonta.service.resources.dto.ValintakoeRDTO;
import org.apache.camel.Body;
import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import static fi.vm.sade.valinta.kooste.security.SecurityPreprocessor.SECURITY_CONTEXT_HEADER;

/**
 * User: wuoti Date: 20.5.2013 Time: 10.46
 */
@Component("suoritaHakukohdeImportKomponentti")
@PreAuthorize("isAuthenticated()")
public class SuoritaHakukohdeImportKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(SuoritaHakukohdeImportKomponentti.class);

    @Autowired
    private ValintaperusteService valintaperusteService;

    @Autowired
    private HakukohdeResource hakukohdeResource;

    public void suoritaHakukohdeImport(@Property(SECURITY_CONTEXT_HEADER) Authentication auth, @Body String hakukohdeOid) {
        assert (auth != null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        HakukohdeNimiRDTO hakukohdeNimi = hakukohdeResource.getHakukohdeNimi(hakukohdeOid);
        HakukohdeDTO hakukohdeData = hakukohdeResource.getByOID(hakukohdeOid);
        HakukohdeImportTyyppi importTyyppi = new HakukohdeImportTyyppi();

        importTyyppi.setTarjoajaOid(hakukohdeNimi.getTarjoajaOid());

        for (String s : hakukohdeNimi.getTarjoajaNimi().keySet()) {
            MonikielinenTekstiTyyppi m = new MonikielinenTekstiTyyppi();
            m.setLang(s);
            m.setText(hakukohdeNimi.getTarjoajaNimi().get(s));
            importTyyppi.getTarjoajaNimi().add(m);
        }
        for (String s : hakukohdeNimi.getHakukohdeNimi().keySet()) {
            MonikielinenTekstiTyyppi m = new MonikielinenTekstiTyyppi();
            m.setLang(s);
            m.setText(hakukohdeNimi.getHakukohdeNimi().get(s));
            importTyyppi.getHakukohdeNimi().add(m);
        }

        for (String s : hakukohdeNimi.getHakuKausi().keySet()) {
            MonikielinenTekstiTyyppi m = new MonikielinenTekstiTyyppi();
            m.setLang(s);
            m.setText(hakukohdeNimi.getHakuKausi().get(s));
            importTyyppi.getHakuKausi().add(m);
        }

        importTyyppi.setHakuVuosi(new Integer(hakukohdeNimi.getHakuVuosi()).toString());

        HakukohdekoodiTyyppi hkt = new HakukohdekoodiTyyppi();
        hkt.setKoodiUri(hakukohdeData.getHakukohdeNimiUri());
        importTyyppi.setHakukohdekoodi(hkt);

        importTyyppi.setHakukohdeOid(hakukohdeData.getOid());
        importTyyppi.setHakuOid(hakukohdeData.getHakuOid());

        importTyyppi.setValinnanAloituspaikat(hakukohdeData.getValintojenAloituspaikatLkm());

        importTyyppi.setTila(hakukohdeData.getTila());
        if (hakukohdeData.getValintakoes() != null) {
            LOG.debug("Valintakokeita löytyi {}!", hakukohdeData.getValintakoes().size());
            for (ValintakoeRDTO valinakoe : hakukohdeData.getValintakoes()) {
                HakukohteenValintakoeTyyppi v = new HakukohteenValintakoeTyyppi();
                v.setOid(valinakoe.getOid());
                v.setTyyppiUri(valinakoe.getTyyppiUri());
                importTyyppi.getValintakoe().add(v);
            }
        }

        valintaperusteService.tuoHakukohde(importTyyppi);
    }

}
