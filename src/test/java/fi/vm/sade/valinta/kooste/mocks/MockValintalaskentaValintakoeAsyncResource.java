package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Jussi Jartamo
 */
@Service
public class MockValintalaskentaValintakoeAsyncResource implements ValintalaskentaValintakoeAsyncResource {

    private static final AtomicReference<List<ValintakoeOsallistuminenDTO>> osallistumistiedot = new AtomicReference<>();
    private static final AtomicReference<List<HakemusOsallistuminenDTO>> hakemusOsallistuminen = new AtomicReference<>();


    public static void setHakemusOsallistuminenResult(List<HakemusOsallistuminenDTO> res) {
        hakemusOsallistuminen.set(res);
    }

    public static void setResult(List<ValintakoeOsallistuminenDTO> res) {
        osallistumistiedot.set(res);
    }

    public static void clear() {
        osallistumistiedot.set(null);
        hakemusOsallistuminen.set(null);
    }

    @Override
    public Observable<List<HakemusOsallistuminenDTO>> haeValintatiedotHakukohteelle(String hakukohdeOid, List<String> valintakoeOid) {
        return Observable.just(hakemusOsallistuminen.get());
    }

    @Override
    public Observable<ValintakoeOsallistuminenDTO> haeHakemukselle(String hakemusOid) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Observable<List<ValintakoeOsallistuminenDTO>> haeHakutoiveelle(String hakukohdeOid) {
        return Observable.just(osallistumistiedot.get());
    }

    @Override
    public Observable<List<ValintakoeOsallistuminenDTO>> haeHakutoiveille(Collection<String> hakukohdeOids) {
        return Observable.just(osallistumistiedot.get());
    }
}
