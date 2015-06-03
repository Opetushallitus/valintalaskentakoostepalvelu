package fi.vm.sade.valinta.kooste.mocks;

import com.google.common.util.concurrent.Futures;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import rx.Observable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

@Service
public class MockTarjontaAsyncService implements TarjontaAsyncResource {
    private static Map<String, HakuV1RDTO> mockHaku= new HashMap<>();

    @Override
    public Observable<HakuV1RDTO> haeHaku(String hakuOid) {
        HakuV1RDTO hakuV1RDTO = new HakuV1RDTO();
        hakuV1RDTO.setOid(hakuOid);
        return Observable.just(hakuV1RDTO);
    }

    @Override
    public Observable<HakukohdeDTO> haeHakukohde(String hakukohdeOid) {
        HakukohdeDTO hakukohdeDTO = new HakukohdeDTO();
        hakukohdeDTO.setHakuOid(MockData.hakuOid);
        hakukohdeDTO.setOid(hakukohdeOid);
        return Observable.just(hakukohdeDTO);
    }

    @Override
    public Peruutettava haeHaku(String hakuOid, Consumer<HakuV1RDTO> callback, Consumer<Throwable> failureCallback) {
        HakuV1RDTO hakuV1RDTO = new HakuV1RDTO();
        hakuV1RDTO.setOid(hakuOid);
        callback.accept(mockHaku.getOrDefault(hakuOid, hakuV1RDTO));
        return new PeruutettavaImpl(Futures.immediateFuture(mockHaku.getOrDefault(hakuOid, hakuV1RDTO)));
    }

    @Override
    public Peruutettava haeHakukohde(String hakuOid, String hakukohdeOid, Consumer<HakukohdeDTO> callback, Consumer<Throwable> failureCallback) {
        HakukohdeDTO hakukohdeDTO = new HakukohdeDTO();
        hakukohdeDTO.setHakuOid(MockData.hakuOid);
        hakukohdeDTO.setOid(hakukohdeOid);
        callback.accept(hakukohdeDTO);
        return new PeruutettavaImpl(Futures.immediateFuture(hakukohdeDTO));
    }

    public static void setMockHaku(HakuV1RDTO mockHaku) {
        MockTarjontaAsyncService.mockHaku.put(mockHaku.getOid(), mockHaku);
        callback.accept(hakuV1RDTO);
        return new PeruutettavaImpl(Futures.immediateFuture(hakuV1RDTO));
    }
}
