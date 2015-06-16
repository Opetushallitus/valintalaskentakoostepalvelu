package fi.vm.sade.valinta.kooste.mocks;


import static fi.vm.sade.valinta.kooste.mocks.MockData.*;

import com.google.common.util.concurrent.Futures;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.HakemusDTO;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.ValintatapajonoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;
import rx.Observable;

@Service
public class MockSijoitteluAsyncResource implements SijoitteluAsyncResource {

    private static final AtomicReference<HakukohdeDTO> resultReference = new AtomicReference<>();
    private static final AtomicReference<HakijaPaginationObject> paginationResultReference = new AtomicReference<>();
    public static void setPaginationResult(HakijaPaginationObject result) {
        paginationResultReference.set(result);
    }
    private static final Map<Long, HakukohdeDTO> resultMap = new ConcurrentHashMap<>();
    public static void setResult(HakukohdeDTO result) {
        resultReference.set(result);
    }
    public static void clear() {
        resultReference.set(null);
        resultMap.clear();
    }

    @Override
    public Observable<HakukohdeDTO> getHakukohdeBySijoitteluajoPlainDTO(String hakuOid, String hakukohdeOid) {
        return null;
    }

    @Override
    public Observable<HakijaPaginationObject> getKoulutuspaikkalliset(String hakuOid, String hakukohdeOid) {
        return null;
    }

    public static Map<Long, HakukohdeDTO> getResultMap() {
        return resultMap;
    }

    @Override
    public void getLatestHakukohdeBySijoittelu(String hakuOid, String hakukohdeOid, Consumer<HakukohdeDTO> hakukohde, Consumer<Throwable> poikkeus) {
        hakukohde.accept(resultReference.get());
    }

    @Override
    public Observable<HakijaPaginationObject> getKoulutuspaikkalliset(String hakuOid) {
        return null;
    }

    @Override
    public Peruutettava getKoulutuspaikkallisetHakijat(String hakuOid, String hakukohdeOid, Consumer<HakijaPaginationObject> callback, Consumer<Throwable> failureCallback) {
        callback.accept(paginationResultReference.get());
        return new PeruutettavaImpl(Futures.immediateFuture(paginationResultReference.get()));
    }

    @Override
    public void getLatestHakukohdeBySijoitteluAjoId(String hakuOid, String hakukohdeOid, Long sijoitteluAjoId, Consumer<HakukohdeDTO> hakukohde, Consumer<Throwable> poikkeus) {
        hakukohde.accept(resultMap.get(sijoitteluAjoId));
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
        hakukohdeDTO.setTarjoajaOid(MockData.tarjoajaOid);
        final ValintatapajonoDTO jono = new ValintatapajonoDTO();
        jono.setOid(MockData.valintatapajonoOid);
        final HakemusDTO hakemus = new HakemusDTO();
        hakemus.setHakemusOid(MockData.hakemusOid);
        hakemus.setHakijaOid(MockData.hakijaOid);
        hakemus.setTila(HakemuksenTila.HYLATTY);
        hakemus.setEtunimi(MockData.etunimi);
        hakemus.setSukunimi(MockData.sukunimi);
        jono.getHakemukset().add(hakemus);
        hakukohdeDTO.getValintatapajonot().add(jono);

        return Futures.immediateFuture(hakukohdeDTO);
    }
}
