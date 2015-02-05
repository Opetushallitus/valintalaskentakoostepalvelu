package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.DokumentinSeurantaAsyncResource;
import fi.vm.sade.valinta.seuranta.dto.DokumenttiDto;
import fi.vm.sade.valinta.seuranta.dto.VirheilmoitusDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author Jussi Jartamo
 */
@Service
public class MockDokumentinSeurantaAsyncResource implements DokumentinSeurantaAsyncResource {

    public static final String GENEERINEN_DOKUMENTTI_ID = "geneerinendokumenttiid";

    @Override
    public void luoDokumentti(String kuvaus, Consumer<String> callback, Consumer<Throwable> failureCallback) {
        callback.accept(GENEERINEN_DOKUMENTTI_ID);
    }

    @Override
    public void paivitaKuvaus(String uuid, String kuvaus, Consumer<DokumenttiDto> callback, Consumer<Throwable> failureCallback) {
        callback.accept(new DokumenttiDto(GENEERINEN_DOKUMENTTI_ID,null,null,null));
    }

    @Override
    public void paivitaDokumenttiId(String uuid, String dokumenttiId, Consumer<DokumenttiDto> callback, Consumer<Throwable> failureCallback) {
        callback.accept(new DokumenttiDto(GENEERINEN_DOKUMENTTI_ID,null,null,null));
    }

    @Override
    public void lisaaVirheilmoituksia(String uuid, List<VirheilmoitusDto> virheilmoitukset, Consumer<DokumenttiDto> callback, Consumer<Throwable> failureCallback) {
        callback.accept(new DokumenttiDto(GENEERINEN_DOKUMENTTI_ID,null,null,null));
    }
}
