package fi.vm.sade.valinta.kooste.mocks;

import com.google.common.util.concurrent.Futures;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;

import java.util.concurrent.Future;

import org.springframework.stereotype.Service;

@Service
public class MockTarjontaAsyncService implements TarjontaAsyncResource {

    @Override
    public Future<HakuV1RDTO> haeHaku(String hakuOid) {
        HakuV1RDTO hakuV1RDTO = new HakuV1RDTO();
        hakuV1RDTO.setOid(hakuOid);
        return Futures.immediateFuture(hakuV1RDTO);
    }

    @Override
    public Future<HakukohdeDTO> haeHakukohde(String hakukohdeOid) {
        HakukohdeDTO hakukohdeDTO = new HakukohdeDTO();
        hakukohdeDTO.setHakuOid(MockData.hakuOid);
        hakukohdeDTO.setOid(hakukohdeOid);
        return Futures.immediateFuture(hakukohdeDTO);
    }
}
