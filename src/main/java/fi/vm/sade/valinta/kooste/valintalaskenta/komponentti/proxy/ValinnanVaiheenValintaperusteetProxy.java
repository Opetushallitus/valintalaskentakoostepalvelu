package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.proxy;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;

import java.util.List;

/**
 * User: wuoti
 * Date: 5.8.2013
 * Time: 12.20
 */
public interface ValinnanVaiheenValintaperusteetProxy {
    List<ValintaperusteetDTO> haeValintaperusteet(String hakukohdeOid,
                                                     int valinnanVaiheJarjestysluku);
}
