package fi.vm.sade.valinta.kooste.external.resource.seuranta;

import java.util.List;

import fi.vm.sade.valinta.seuranta.dto.DokumenttiDto;
import fi.vm.sade.valinta.seuranta.dto.VirheilmoitusDto;
import rx.Observable;

public interface DokumentinSeurantaAsyncResource {

    Observable<String> luoDokumentti(String kuvaus);

    Observable<DokumenttiDto> paivitaKuvaus(String uuid, String kuvaus);

    Observable<DokumenttiDto> paivitaDokumenttiId(String uuid, String dokumenttiId);

    Observable<DokumenttiDto> lisaaVirheilmoituksia(String uuid, List<VirheilmoitusDto> virheilmoitukset);
}
