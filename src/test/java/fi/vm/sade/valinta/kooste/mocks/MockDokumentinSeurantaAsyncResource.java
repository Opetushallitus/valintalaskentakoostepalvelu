package fi.vm.sade.valinta.kooste.mocks;

import java.util.List;

import org.springframework.stereotype.Service;

import fi.vm.sade.valinta.kooste.external.resource.seuranta.DokumentinSeurantaAsyncResource;
import fi.vm.sade.valinta.seuranta.dto.DokumenttiDto;
import fi.vm.sade.valinta.seuranta.dto.VirheilmoitusDto;
import io.reactivex.Observable;

/**
 * @author Jussi Jartamo
 */
@Service
public class MockDokumentinSeurantaAsyncResource implements DokumentinSeurantaAsyncResource {

    public static final String GENEERINEN_DOKUMENTTI_ID = "geneerinendokumenttiid";

    @Override
    public Observable<String> luoDokumentti(String kuvaus) {
        return Observable.just(GENEERINEN_DOKUMENTTI_ID);
    }

    @Override
    public Observable<DokumenttiDto> paivitaKuvaus(final String uuid, final String kuvaus) {
        return Observable.just(new DokumenttiDto(GENEERINEN_DOKUMENTTI_ID,null,null,null));
    }

    @Override
    public Observable<DokumenttiDto> paivitaDokumenttiId(final String uuid, final String dokumenttiId) {
        return Observable.just(new DokumenttiDto(GENEERINEN_DOKUMENTTI_ID,null,null,null));
    }

    @Override
    public Observable<DokumenttiDto> lisaaVirheilmoituksia(final String uuid, final List<VirheilmoitusDto> virheilmoitukset) {
        return Observable.just(new DokumenttiDto(GENEERINEN_DOKUMENTTI_ID,null,null,null));
    }
}
