package fi.vm.sade.valinta.kooste.external.resource.seuranta;

import fi.vm.sade.valinta.seuranta.dto.*;

import java.util.List;
import java.util.function.Consumer;

public interface DokumentinSeurantaAsyncResource {

    void luoDokumentti(String kuvaus, Consumer<String> callback, Consumer<Throwable> failureCallback);

    void paivitaKuvaus(String uuid, String kuvaus, Consumer<DokumenttiDto> callback, Consumer<Throwable> failureCallback);

    void paivitaDokumenttiId(String uuid, String dokumenttiId, Consumer<DokumenttiDto> callback, Consumer<Throwable> failureCallback);

    void lisaaVirheilmoituksia(String uuid, List<VirheilmoitusDto> virheilmoitukset, Consumer<DokumenttiDto> callback, Consumer<Throwable> failureCallback);

}
