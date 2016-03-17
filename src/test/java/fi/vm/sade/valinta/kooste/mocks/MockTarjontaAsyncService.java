package fi.vm.sade.valinta.kooste.mocks;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import com.google.common.util.concurrent.Futures;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import rx.Observable;

@Service
public class MockTarjontaAsyncService implements TarjontaAsyncResource {
    private static Map<String, HakuV1RDTO> mockHaku= new HashMap<>();

    @Override
    public Observable<HakuV1RDTO> haeHaku(String hakuOid) {
        if(mockHaku.containsKey(hakuOid)) {
            return Observable.just(mockHaku.get(hakuOid));
        }
        HakuV1RDTO hakuV1RDTO = new HakuV1RDTO();
        hakuV1RDTO.setOid(hakuOid);
        return Observable.just(hakuV1RDTO);
    }

    @Override
    public Observable<HakukohdeV1RDTO> haeHakukohde(String hakukohdeOid) {
        HakukohdeV1RDTO hakukohdeDTO = new HakukohdeV1RDTO();
        hakukohdeDTO.setHakuOid(MockData.hakuOid);
        hakukohdeDTO.setOid(hakukohdeOid);
        return Observable.just(hakukohdeDTO);
    }

    public static void setMockHaku(HakuV1RDTO mockHaku) {
        MockTarjontaAsyncService.mockHaku.put(mockHaku.getOid(), mockHaku);
    }
}
