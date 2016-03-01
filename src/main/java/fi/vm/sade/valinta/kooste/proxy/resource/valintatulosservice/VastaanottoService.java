package fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VastaanottoService {


    @Autowired
    private ValintaTulosServiceAsyncResource valintaTulosServiceResource;

    public Observable<Integer> tallenna(List<Valintatulos> valintatulokset, String muokkaaja, String selite) {
        List<VastaanottoRecordDTO> tallennettavat = valintatulokset.stream()
            .<VastaanottoRecordDTO>map(valintatulos -> VastaanottoRecordDTO.of(valintatulos, muokkaaja, selite))
            .collect(Collectors.toList());
        return valintaTulosServiceResource.tallenna(tallennettavat).map(v -> tallennettavat.size());
    }
}
