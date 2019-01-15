package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri;

import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Suoritus;
import io.reactivex.Observable;

import java.util.List;

public interface SuoritusrekisteriAsyncResource {

    Observable<List<Oppija>> getOppijatByHakukohde(String hakukohdeOid,
                                                   String hakuOid);

    Observable<List<Oppija>> getOppijatByHakukohdeWithoutEnsikertalaisuus(String hakukohdeOid,
                                                                          String hakuOid);

    Observable<Oppija> getSuorituksetByOppija(String opiskelijaOid, String hakuOid);

    Observable<List<Oppija>> getSuorituksetByOppijas(List<String> opiskelijaOids, String hakuOid);

    Observable<Oppija> getSuorituksetWithoutEnsikertalaisuus(String opiskelijaOid);

    Observable<List<Oppija>> getSuorituksetWithoutEnsikertalaisuus(List<String> opiskelijaOids);

    Observable<Suoritus> postSuoritus(Suoritus suoritus);

    Observable<Arvosana> postArvosana(Arvosana arvosana);

    Observable<Arvosana> updateExistingArvosana(String arvosanaId, Arvosana arvosanaWithUpdatedValues);

    Observable<String> deleteSuoritus(String suoritusId);

    Observable<String> deleteArvosana(String arvosanaId);
}
