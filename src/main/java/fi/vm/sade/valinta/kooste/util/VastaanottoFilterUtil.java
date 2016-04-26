package fi.vm.sade.valinta.kooste.util;

import com.google.common.collect.ImmutableSet;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.HakemusDTO;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import org.apache.commons.collections.keyvalue.MultiKey;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author nobody is willing to claim such honor
 */
public class VastaanottoFilterUtil {
    private static final Set<HakemuksenTila> PRESERVING_STATES = ImmutableSet.of(
            HakemuksenTila.HYVAKSYTTY,
            HakemuksenTila.HARKINNANVARAISESTI_HYVAKSYTTY,
            HakemuksenTila.VARASIJALTA_HYVAKSYTTY,
            HakemuksenTila.PERUNUT,
            HakemuksenTila.PERUUTETTU
    );

    /**
     * As dumb as it sounds, this method nullifies vastaanotto states if hakemus state is one of the specific states.
     * This is due the behaviour of showing vastaanotto state only in the "meaningful" valintatapajono.
     *
     * This code should exist in valinta-tulos-service, but implemented here because "there is no time to test before
     * going live".
     *
     * @param tulokset
     * @param hakukohde
     * @return
     */
    public static List<Valintatulos> nullifyVastaanottoBasedOnHakemuksenTila(final List<Valintatulos> tulokset,
                                                                             final HakukohdeDTO hakukohde) {
        final Map<MultiKey, HakemusDTO> hakemuksetByJonoAndHakemusOid = hakukohde.getValintatapajonot().stream()
                .flatMap(jono -> jono.getHakemukset().stream())
                .collect(Collectors.toMap(hakemus ->
                        new MultiKey(hakemus.getValintatapajonoOid(), hakemus.getHakemusOid()), Function.identity()));

        return tulokset.stream().map(tulos -> {
            Optional<HakemuksenTila> hakemuksenTila = Optional.ofNullable(
                    hakemuksetByJonoAndHakemusOid.get(new MultiKey(tulos.getValintatapajonoOid(), tulos.getHakemusOid()))
            ).map(HakemusDTO::getTila);
            if (hakemuksenTila.isPresent() && !PRESERVING_STATES.contains(hakemuksenTila.get())) {
                tulos.setTila(null, null, "", "");
            }
            return tulos;
        }).collect(Collectors.toList());
    }
}
