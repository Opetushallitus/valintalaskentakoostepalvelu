package fi.vm.sade.valinta.kooste.external.resource.seuranta;

import fi.vm.sade.valinta.seuranta.dto.DokumenttiDto;
import fi.vm.sade.valinta.seuranta.dto.VirheilmoitusDto;
import io.reactivex.Observable;
import java.util.List;

public interface DokumentinSeurantaAsyncResource {

  Observable<String> luoDokumentti(String kuvaus);

  Observable<DokumenttiDto> paivitaKuvaus(String uuid, String kuvaus);

  Observable<DokumenttiDto> paivitaDokumenttiId(String uuid, String dokumenttiId);

  Observable<DokumenttiDto> lisaaVirheilmoituksia(
      String uuid, List<VirheilmoitusDto> virheilmoitukset);
}
