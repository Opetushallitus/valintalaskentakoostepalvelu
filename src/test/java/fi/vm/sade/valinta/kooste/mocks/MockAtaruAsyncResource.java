package fi.vm.sade.valinta.kooste.mocks;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.KielisyysDto;
import fi.vm.sade.valinta.kooste.util.AtaruHakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import io.reactivex.Observable;
import org.apache.commons.io.IOUtils;
import org.mockito.internal.util.collections.Sets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MockAtaruAsyncResource implements AtaruAsyncResource {

    private static List<HakemusWrapper> byHakukohdeRes = Lists.newArrayList();
    private static List<HakemusWrapper> byOidsResult = Lists.newArrayList();

    @Override
    public Observable<List<HakemusWrapper>> getApplicationsByHakukohde(String hakukohdeOid) {
        return Observable.just(byHakukohdeRes);
    }

    @Override
    public Observable<List<HakemusWrapper>> getApplicationsByOids(List<String> oids) {
        return Observable.just(byOidsResult);
    }

    public static void setByHakukohdeResult(List<HakemusWrapper> hakemukset) {
        byHakukohdeRes = hakemukset;
    }

    public static void setByOidsResult(List<HakemusWrapper> hakemukset) {
        byOidsResult = hakemukset;
    }

    public static void clear() {
        byOidsResult = Lists.newArrayList();
        byHakukohdeRes = Lists.newArrayList();
    }

    public static List<AtaruHakemus> getAtaruHakemukset(Set<String> oids) {
        try {
            List<AtaruHakemus> hakemukset = new Gson().fromJson(IOUtils
                    .toString(new ClassPathResource("ataruhakemukset.json")
                            .getInputStream()), new TypeToken<List<AtaruHakemus>>() {
            }.getType());

            if (oids == null) {
                return hakemukset;
            } else {
                return hakemukset.stream()
                        .filter(h -> oids.contains(h.getHakemusOid()))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            throw new RuntimeException("Couldn't fetch mock ataru application", e);
        }
    }

    private static HenkiloPerustietoDto createHenkilo() {
        HenkiloPerustietoDto henkilo = new HenkiloPerustietoDto();
        henkilo.setSukunimi("TAUsuL4BQc");
        henkilo.setEtunimet("Zl2A5");
        henkilo.setOidHenkilo("1.2.246.562.24.86368188549");
        henkilo.setHetu("020202A0202");
        henkilo.setAidinkieli(new KielisyysDto("fi", "suomi"));
        return henkilo;
    }

    public static HakemusWrapper getAtaruHakemusWrapper(String s) {
        AtaruHakemus hakemus = getAtaruHakemukset(Sets.newSet(s)).iterator().next();
        return new AtaruHakemusWrapper(hakemus, createHenkilo());
    }
}
