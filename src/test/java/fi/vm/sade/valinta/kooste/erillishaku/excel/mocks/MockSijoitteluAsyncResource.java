package fi.vm.sade.valinta.kooste.erillishaku.excel.mocks;


import com.google.common.util.concurrent.Futures;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

public class MockSijoitteluAsyncResource implements SijoitteluAsyncResource {
    @Override
    public Future<List<Valintatulos>> getValintatuloksetHakukohteelle(String hakukohdeOid, String valintatapajonoOid) {
        Valintatulos valintatulos = new Valintatulos();
        valintatulos.setHakemusOid("hakemus1");
        valintatulos.setHakijaOid("hakija1");
        valintatulos.setHakukohdeOid(hakukohdeOid);
        valintatulos.setHakuOid("haku1");
        valintatulos.setValintatapajonoOid(valintatapajonoOid);
        return Futures.immediateFuture(Arrays.asList(valintatulos));
    }

    @Override
    public Future<HakijaPaginationObject> getKaikkiHakijat(String hakuOid, String hakukohdeOid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<HakijaPaginationObject> getKoulutuspaikkallisetHakijat(String hakuOid, String hakukohdeOid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<HakijaPaginationObject> getHakijatIlmanKoulutuspaikkaa(String hakuOid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<HakukohdeDTO> getLatestHakukohdeBySijoittelu(String hakuOid, String hakukohdeOid) {
        HakukohdeDTO hakukohdeDTO = new HakukohdeDTO();
        hakukohdeDTO.setOid(hakukohdeOid);
        hakukohdeDTO.setTarjoajaOid("tarjoaja1");


        return Futures.immediateFuture(hakukohdeDTO);
    }
}
