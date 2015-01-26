package fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.util;

import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.dto.HakemusSijoitteluntulosMergeDto;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by jussija on 26/01/15.
 */
public class HakemusSijoitteluntulosMergeUtil {

    public static List<HakemusSijoitteluntulosMergeDto> merge(
            List<Hakemus> hakemukset,
            HakukohdeDTO hakukohdeDTO,
            List<ValinnanVaiheJonoillaDTO> valinnanvaiheet,
            List<ValintatietoValinnanvaiheDTO> laskennantulokset,
            Map<Long,HakukohdeDTO> hakukohteetBySijoitteluAjoId
    // <- empty map jos ei erillissijoittelun hakukohteita
    ) {

        return null;
    }
}
