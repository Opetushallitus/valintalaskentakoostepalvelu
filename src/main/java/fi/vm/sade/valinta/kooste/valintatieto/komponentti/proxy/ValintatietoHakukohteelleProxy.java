package fi.vm.sade.valinta.kooste.valintatieto.komponentti.proxy;

import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;

import java.util.List;

public interface ValintatietoHakukohteelleProxy {
    List<HakemusOsallistuminenTyyppi> haeValintatiedotHakukohteelle(List<String> valintakoeOids, String hakukohdeOid);
}
