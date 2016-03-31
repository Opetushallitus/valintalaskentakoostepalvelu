package fi.vm.sade.valinta.kooste.pistesyotto.service;


import com.google.common.collect.Maps;
import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.UlkoinenResponseDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.VirheDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static fi.vm.sade.auditlog.valintaperusteet.LogMessage.builder;
import static fi.vm.sade.valinta.kooste.KoosteAudit.AUDIT;

public class PistesyottoTuontiSoteliService {
    private static final Logger LOG = LoggerFactory.getLogger(PistesyottoTuontiSoteliService.class);

    private final ValintaperusteetAsyncResource valintaperusteetResource;
    private final ApplicationAsyncResource applicationAsyncResource;

    @Autowired
    public PistesyottoTuontiSoteliService(ValintaperusteetAsyncResource valintaperusteetResource, ApplicationAsyncResource applicationAsyncResource) {
        this.valintaperusteetResource = valintaperusteetResource;
        this.applicationAsyncResource = applicationAsyncResource;
    }


    public void tuo(List<HakemusDTO> hakemukset, String username, String hakuOid, String valinnanvaiheOid, BiConsumer<Integer, Collection<VirheDTO>> successHandler, Consumer<Throwable> exceptionHandler) {
        valintaperusteetResource.valintaperusteet(valinnanvaiheOid).subscribe(
                valintaperusteetDTOs -> {
                    final Map<String, VirheDTO> virheet = hakemukset.stream().map(h-> {
                        VirheDTO virheDTO = new VirheDTO();
                        virheDTO.setHakemusOid(h.getHakemusOid());
                        virheDTO.setVirhe("");
                        return virheDTO;
                    }).filter(Objects::nonNull).collect(Collectors.toMap(v -> v.getHakemusOid(), v-> v));

                    List<ApplicationAdditionalDataDTO> uudetPistetiedot =
                            hakemukset.stream().filter(h -> !virheet.containsKey(h.getHakemusOid()))
                                    .map(h -> {
                                        ApplicationAdditionalDataDTO addData = new ApplicationAdditionalDataDTO();
                                        addData.setOid(h.getHakemusOid());
                                        addData.setPersonOid(h.getHenkiloOid());
                                        Map<String, String> additionalData = Maps.newHashMap();
                                        addData.setAdditionalData(additionalData);
                                        return addData;
                                    }).collect(Collectors.toList());
                    if(!uudetPistetiedot.isEmpty()) {
                        applicationAsyncResource.putApplicationAdditionalData(
                                hakuOid, "", uudetPistetiedot).subscribe(response -> {
                            uudetPistetiedot.forEach(p ->
                                    AUDIT.log(builder()
                                            .id(username)
                                            .hakuOid(hakuOid)
                                            .hakijaOid(p.getPersonOid())
                                            .hakemusOid(p.getOid())
                                            .addAll(p.getAdditionalData())
                                            .setOperaatio(ValintaperusteetOperation.PISTETIEDOT_TUONTI_EXCEL)
                                            .build())
                            );
                            successHandler.accept(uudetPistetiedot.size(), virheet.values());
                        }, exception -> exceptionHandler.accept(exception));
                    } else {
                        successHandler.accept(0, virheet.values());
                    }
                },
                exception -> exceptionHandler.accept(exception)
        );
    }
}
