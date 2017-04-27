package fi.vm.sade.valinta.kooste.mocks;

import java.util.*;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultOrganization;
import org.springframework.stereotype.Service;

import com.google.common.util.concurrent.Futures;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import rx.Observable;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import static fi.vm.sade.valinta.kooste.mocks.MockData.*;

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
    public Observable<List<ResultOrganization>> hakukohdeSearchByOrganizationGroupOids(Collection<String> organizationGroupOids) {
        return null;
    }

    @Override
    public Observable<List<ResultOrganization>> hakukohdeSearchByOrganizationOids(Collection<String> organizationOids) {
        return null;
    }

    @Override
    public Observable<HakukohdeV1RDTO> haeHakukohde(String hakukohdeOid) {
        HakukohdeV1RDTO hakukohdeDTO = new HakukohdeV1RDTO();
        hakukohdeDTO.setHakuOid(hakuOid);
        hakukohdeDTO.setOid(hakukohdeOid);
        hakukohdeDTO.setTarjoajaOids(ImmutableSet.of("1.2.3.44444.5"));
        return Observable.just(hakukohdeDTO);
    }

    @Override
    public Observable<Set<String>> findHakuOidsForAutosyncTarjonta() {
        Set<String> set = new HashSet<>();
        set.add(hakuOid);
        set.add(hakuOid + "-1");
        return Observable.just(set);
    }

    @Override
    public Observable<Map<String, List<String>>> hakukohdeRyhmasForHakukohdes(String hakuOid) {
        return Observable.just(Maps.newHashMap());
    }

    public static void setMockHaku(HakuV1RDTO mockHaku) {
        MockTarjontaAsyncService.mockHaku.put(mockHaku.getOid(), mockHaku);
    }
}
