package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.proxy;

import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

import java.util.List;

/**
 * User: wuoti
 * Date: 5.8.2013
 * Time: 12.20
 */
public interface ValinnanVaiheenValintaperusteetProxy {
    List<ValintaperusteetTyyppi> haeValintaperusteet(String hakukohdeOid,
                                                     int valinnanVaiheJarjestysluku);
}
