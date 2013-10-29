package fi.vm.sade.valinta.kooste.valintatieto.komponentti.proxy;

import java.util.List;

import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;

public interface ValintatietoHakukohteelleProxy {
    List<HakemusOsallistuminenTyyppi> haeValintatiedotHakukohteelle(List<String> valintakoeOids, String hakukohdeOid);
}
