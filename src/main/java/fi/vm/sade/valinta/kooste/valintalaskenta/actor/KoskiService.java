package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiOppija;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class KoskiService {
    private static final Logger LOG = LoggerFactory.getLogger(KoskiService.class);

    private static final Predicate<String> INCLUDE_ALL = s -> true;
    private static final Predicate<String> EXCLUDE_ALL = s -> false;
    private final Predicate<String> koskiHakukohdeOidFilter;
    private final KoskiAsyncResource koskiAsyncResource;

    @Autowired
    public KoskiService(@Value("${valintalaskentakoostepalvelu.laskenta.koskesta.haettavat.hakukohdeoidit:none}") String koskiHakukohdeOiditString,
                        KoskiAsyncResource koskiAsyncResource) {
        this.koskiAsyncResource = koskiAsyncResource;
        this.koskiHakukohdeOidFilter = resolveKoskiHakukohdeOidFilter(koskiHakukohdeOiditString);
    }

    private static Predicate<String> resolveKoskiHakukohdeOidFilter(String koskiHakukohdeOiditString) {
        if (StringUtils.isBlank(koskiHakukohdeOiditString)) {
            LOG.info("Saatiin '" + koskiHakukohdeOiditString + "' Koskesta haettaviksi hakukohdeoideiksi => ei haeta ollenkaan tietoja Koskesta.");
            return EXCLUDE_ALL;
        }
        if ("ALL".equals(koskiHakukohdeOiditString)) {
            LOG.info("Saatiin '" + koskiHakukohdeOiditString + "' Koskesta haettaviksi hakukohdeoideiksi => haetaan kaikille tiedot Koskesta.");
            return INCLUDE_ALL;
        }
        List<String> hakukohdeOids = Arrays.asList(koskiHakukohdeOiditString.split(","));
        LOG.info("Saatiin '" + koskiHakukohdeOiditString + "' Koskesta haettaviksi hakukohdeoideiksi => haetaan Koskesta tiedot seuraaville hakukohteille: " + hakukohdeOids);
        return hakukohdeOids::contains;
    }

    public CompletableFuture<Map<String, KoskiOppija>> haeKoskiOppijat(String hakukohdeOid, CompletableFuture<List<HakemusWrapper>> hakemukset) {
        if (koskiHakukohdeOidFilter.test(hakukohdeOid)) {
            return hakemukset.thenComposeAsync(hakemusWrappers -> {
                LOG.info(String.format("Haetaan Koskesta tiedot %d oppijalle hakukohteen %s laskemista varten.", hakemusWrappers.size(), hakukohdeOid));
                List<String> oppijanumerot = hakemusWrappers.stream().map(HakemusWrapper::getPersonOid).collect(Collectors.toList());
                return koskiAsyncResource
                    .findKoskiOppijat(oppijanumerot)
                    .thenApplyAsync(koskioppijat -> {
                        LOG.info(String.format("Saatiin Koskesta %s oppijan tiedot, kun haettiin %d oppijalle hakukohteen %s laskemista varten.",
                            koskioppijat.size(), hakemusWrappers.size(), hakukohdeOid));
                        return koskioppijat.stream().collect(Collectors.toMap(KoskiOppija::getOppijanumero, Function.identity()));
                    });
            });
        } else {
            LOG.info("Ei haeta tietoja Koskesta hakukohteelle " + hakukohdeOid);
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }
    }
}
