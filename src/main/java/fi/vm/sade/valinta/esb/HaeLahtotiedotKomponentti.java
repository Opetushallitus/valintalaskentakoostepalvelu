package fi.vm.sade.valinta.esb;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.HakemusService;
import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

@Component("HaeLahtotiedotKomponentti")
public class HaeLahtotiedotKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HaeLahtotiedotKomponentti.class);

    @Autowired
    private ValintaperusteService valintaperusteService;

    @Autowired
    private HakemusService hakemusService;

    // HUOM! Tämä ei toimi koska @OutHeaders annotaatiossa on joku bugi näemmä!
    // public void haeLahtotiedotKomponentti(@Header("hakukohdeOid") String
    // hakukohdeOid,
    // @Header("valinnanvaihe") Integer valinnanvaihe, @OutHeaders Map<String,
    // Object> out) {
    public void haeLahtotiedot(Exchange exchange) {

        String hakukohdeOid = exchange.getIn().getHeader("hakukohdeOid", String.class);
        Integer valinnanvaihe = exchange.getIn().getHeader("valinnanvaihe", Integer.class);
        LOG.info("Haetaan lähtötietoja laskentaa varten hakukohteelle({}) ja valinnanvaiheelle({})", new Object[] {
                hakukohdeOid, valinnanvaihe });
        List<HakemusTyyppi> hakemukset = hakemusService.haeHakemukset(Arrays.asList(hakukohdeOid));

        HakuparametritTyyppi hakuparametri = new HakuparametritTyyppi();
        hakuparametri.setHakukohdeOid(hakukohdeOid);
        hakuparametri.setValinnanVaiheJarjestysluku(valinnanvaihe);
        List<ValintaperusteetTyyppi> valintaperusteet = valintaperusteService.haeValintaperusteet(Arrays
                .asList(hakuparametri));

        Map<String, Object> out = exchange.getOut().getHeaders();
        out.put("hakukohdeOid", hakukohdeOid);
        out.put("valinnanvaihe", valinnanvaihe);
        out.put("hakemukset", hakemukset);
        out.put("valintaperusteet", valintaperusteet);
    }
}
