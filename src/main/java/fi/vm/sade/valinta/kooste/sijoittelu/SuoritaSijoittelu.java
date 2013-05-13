package fi.vm.sade.valinta.kooste.sijoittelu;

import fi.vm.sade.service.sijoittelu.schema.TarjontaTyyppi;
import fi.vm.sade.service.sijoittelu.types.SijoitteleTyyppi;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.service.valintatiedot.schema.HakukohdeTyyppi;
import fi.vm.sade.tarjonta.service.types.HakuTyyppi;
import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 *
 */
@Component("suoritaSijoittelu")
public class SuoritaSijoittelu {

    private static final Logger LOG = LoggerFactory.getLogger(SuoritaSijoittelu.class);

    @Autowired
    private fi.vm.sade.service.valintatiedot.ValintatietoService valintatietoService;

    @Autowired
    private fi.vm.sade.service.sijoittelu.SijoitteluService sijoitteluService;

    public void haeLahtotiedot(@Simple("${property.hakuOid}") String hakuOid) {

        LOG.info("KOOSTEPALVELU: Haetaan valintatiedot haulle {}", new Object[] {hakuOid});
        List<fi.vm.sade.service.valintatiedot.schema.HakukohdeTyyppi> hakukohteet = valintatietoService.haeValintatiedot(hakuOid);
        LOG.info("Haettu valinnan tulokset");

        SijoitteleTyyppi sijoittelu = new SijoitteleTyyppi();
        sijoittelu.setTarjonta( new TarjontaTyyppi());
        sijoittelu.getTarjonta().getHakukohde().addAll(hakukohteet);
        sijoittelu.getTarjonta().setHaku(new HakuTyyppi());
        sijoittelu.getTarjonta().getHaku().setOid(hakuOid);
        sijoitteluService.sijoittele(sijoittelu);
    }
}


