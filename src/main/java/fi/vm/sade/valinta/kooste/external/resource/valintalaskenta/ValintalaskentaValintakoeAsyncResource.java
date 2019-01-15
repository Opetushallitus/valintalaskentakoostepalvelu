package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta;

import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;
import io.reactivex.Observable;

import java.util.Collection;
import java.util.List;

public interface ValintalaskentaValintakoeAsyncResource {
    Observable<List<ValintakoeOsallistuminenDTO>> haeHakutoiveelle(String hakukohdeOid);
    Observable<List<ValintakoeOsallistuminenDTO>> haeHakutoiveille(Collection<String> hakukohdeOids);
    Observable<List<HakemusOsallistuminenDTO>> haeValintatiedotHakukohteelle(String hakukohdeOid, List<String> valintakoeTunnisteet);
    Observable<ValintakoeOsallistuminenDTO> haeHakemukselle(String hakemusOid);
}
