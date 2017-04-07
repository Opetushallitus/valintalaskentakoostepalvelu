package fi.vm.sade.valinta.kooste.external.resource.sijoittelu;

import java.util.Collection;
import java.util.List;

import javax.ws.rs.core.Response;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import rx.Observable;

public interface TilaAsyncResource {
    Observable<Response> tuoErillishaunTilat(String hakuOid, String hakukohdeOid, Collection<ErillishaunHakijaDTO> erillishaunHakijat);

    Observable<List<Valintatulos>> getValintatuloksetValintatapajonolle(String hakukohdeOid, String valintatapajonoOid);

    Observable<List<Valintatulos>> getValintatuloksetHakukohteelle(String hakukohdeOid);

    Observable<Valintatulos> getHakemuksenSijoittelunTulos(String hakemusOid, String hakuOid, String hakukohdeOid, String valintatapajonoOid);

    Observable<List<Valintatulos>> getHakemuksenTulokset(String hakemusOid);
}
