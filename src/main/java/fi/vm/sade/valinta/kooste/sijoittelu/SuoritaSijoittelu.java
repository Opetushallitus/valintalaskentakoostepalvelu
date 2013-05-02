package fi.vm.sade.valinta.kooste.sijoittelu;

import fi.vm.sade.service.sijoittelu.schema.TarjontaTyyppi;
import fi.vm.sade.service.sijoittelu.types.SijoitteleTyyppi;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.service.valintatiedot.schema.HakukohdeTyyppi;
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

        LOG.info("Haetaan valintatiedot haulle {}", new Object[] {hakuOid});

             List<fi.vm.sade.service.valintatiedot.schema.HakukohdeTyyppi>    a =      valintatietoService.haeValintatiedot(hakuOid);


       // SijoitteleTyyppi b = new SijoitteleTyyppi();
     //   b.setTarjonta( new TarjontaTyyppi());
     //   List<fi.vm.sade.service.sijoittelu.schema.HakukohdeTyyppi> c = b.getTarjonta().getHakukohde();

        //sijoitteluService.sijoittele(b);

    }
}


