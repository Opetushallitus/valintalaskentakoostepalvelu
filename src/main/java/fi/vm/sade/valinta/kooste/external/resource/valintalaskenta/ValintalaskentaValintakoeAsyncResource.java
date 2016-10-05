package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;
import rx.Observable;

public interface ValintalaskentaValintakoeAsyncResource {
    Observable<List<ValintakoeOsallistuminenDTO>> haeHakutoiveelle(String hakukohdeOid);
    Observable<List<ValintakoeOsallistuminenDTO>> haeHakutoiveille(Collection<String> hakukohdeOids);
    Observable<List<HakemusOsallistuminenDTO>> haeValintatiedotHakukohteelle(String hakukohdeOid, List<String> valintakoeTunnisteet);
    Observable<List<ValintakoeOsallistuminenDTO>> haeAmmatillisenKielikokeenOsallistumiset(Date since);
}
