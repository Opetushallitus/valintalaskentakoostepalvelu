package fi.vm.sade.valinta.kooste.viestintapalvelu;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.koodisto.service.types.common.KieliType;
import fi.vm.sade.koodisto.service.types.common.KoodiMetadataType;
import fi.vm.sade.koodisto.service.types.common.KoodiType;
import fi.vm.sade.koodisto.util.KoodiServiceSearchCriteriaBuilder;
import fi.vm.sade.valinta.kooste.util.Memoizer;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

@Component
public class PostitoimipaikkaMemoize {

    public final Function<KieliType, Function<String, String>> postitoimipaikka;

    @Autowired
    public PostitoimipaikkaMemoize(KoodiService koodiService) {
        postitoimipaikka = kielityyppi -> Memoizer.memoize(koodiUri -> {
            List<KoodiType> koodiTypes = koodiService.searchKoodis(KoodiServiceSearchCriteriaBuilder.latestKoodisByUris(koodiUri));
            Optional<String> postitoimipaikka = koodiTypes.stream()
                    .filter(type -> type.getMetadata() != null)
                    .map(checkKuvausAndNimi(kielityyppi))
                    .flatMap(opt -> opt.isPresent() ? Stream.of(opt.get()) : Stream.empty())
                    .findFirst();
            return postitoimipaikka.flatMap(ptp -> Optional.of(StringUtils.capitalize(ptp.toLowerCase()))).orElse(StringUtils.EMPTY);
        }, 12, TimeUnit.HOURS);
    }

    private Function<KoodiType, Optional<String>> checkKuvausAndNimi(KieliType kielityyppi) {
        return type -> {
            Optional<String> kuvaus = getKuvaus(type.getMetadata(), kielityyppi);
            if (kuvaus.isPresent()) {
                return kuvaus;
            } else {
                return getNimi(type.getMetadata());
            }
        };
    }

    private static Optional<String> getNimi(List<KoodiMetadataType> meta) {
        return meta.stream().findFirst().map(KoodiMetadataType::getNimi);
    }

    private static Optional<String> getKuvaus(List<KoodiMetadataType> meta, KieliType kieli) {
        return meta.stream().filter(data -> kieli.equals(data.getKieli())).findFirst().map(KoodiMetadataType::getKuvaus);
    }


}
