package fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static fi.vm.sade.sijoittelu.domain.VastaanotettavuusDTO.VastaanottoAction.matchesInexistingAction;

@Service
public class VastaanottoService {


    @Autowired
    private ValintaTulosServiceAsyncResource valintaTulosServiceResource;

    public Observable<Integer> tallenna(String hakukohdeOid, List<Valintatulos> valintatulokset, String muokkaaja) {
        return paivitaVastaanototValintarekisteriin(valintatulokset, hakukohdeOid, muokkaaja);
    }

    private Observable<Integer> paivitaVastaanototValintarekisteriin(List<Valintatulos> valintatulokset, String hakukohdeOid, String muokkaaja) {
        final Observable<List<VastaanottoRecordDTO>> o = valintaTulosServiceResource.hakukohteenVastaanotot(hakukohdeOid);

        return o.map(aiemmatVastaanotot -> {
            Map<Boolean, List<Valintatulos>> aktiivisetJaPoistettavat = valintatulokset.stream()
                    .filter(valintatulos -> onPaivitettava(valintatulos, aiemmatVastaanotot))
                    .collect(Collectors.partitioningBy(valintatulos -> !matchesInexistingAction(valintatulos.getTila())));

            tallennaAktiivisetVastaanotot(aktiivisetJaPoistettavat.get(true), muokkaaja);
            poistaVastaanotot(aktiivisetJaPoistettavat.get(false), muokkaaja);

            return aiemmatVastaanotot.size();
        });

    }

    private void tallennaAktiivisetVastaanotot(List<Valintatulos> valintatulokset, String muokkaaja) {
        List<VastaanottoRecordDTO> tallennettavat = valintatulokset.stream()
                .<VastaanottoRecordDTO>map(valintatulos -> VastaanottoRecordDTO.of(valintatulos, muokkaaja))
                .collect(Collectors.toList());
        valintaTulosServiceResource.tallenna(tallennettavat);
    }

    private void poistaVastaanotot(List<Valintatulos> valintatulokset, String muokkaaja) {
        valintatulokset.stream()
                .<PoistaVastaanottoDTO>map(valintatulos -> PoistaVastaanottoDTO.of(valintatulos, muokkaaja))
                .forEach(valintaTulosServiceResource::poista);
    }

    private boolean onPaivitettava(Valintatulos valintatulos, List<VastaanottoRecordDTO> aiemmatVastaanotot) {
        Optional<VastaanottoRecordDTO> aiempiVastaanotto = aiemmatVastaanotot.stream()
                .filter(vastaanottoRecordDTO -> vastaanottoRecordDTO.getHenkiloOid().equals(valintatulos.getHakijaOid()))
                .findFirst();

        if (aiempiVastaanotto.isPresent() && aiempiVastaanotto.get().getAction().actionMatches(valintatulos.getTila())) {
            return false;
        } else if (!aiempiVastaanotto.isPresent() && matchesInexistingAction(valintatulos.getTila())) {
            return false;
        }
        return true;
    }

}
