package fi.vm.sade.valinta.kooste.pistesyotto.service;

import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper;
import fi.vm.sade.valinta.kooste.pistesyotto.service.AbstractPistesyottoKoosteService.SingleKielikoeTulos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AmmatillisenKielikoetulosUpdates {
    private static final Logger LOG = LoggerFactory.getLogger(AmmatillisenKielikoetulosUpdates.class);
    private final Map<String, List<SingleKielikoeTulos>> resultsToSendToSure = new HashMap<>();

    public AmmatillisenKielikoetulosUpdates(String myontajaOid, List<Oppija> oppijatiedotSuresta,
                                            Map<String, List<SingleKielikoeTulos>> kielikoetuloksetHakemuksittain,
                                            Function<String, String> findPersonOidByHakemusOid) {
        for (String hakemusOid : kielikoetuloksetHakemuksittain.keySet()) {
            List<SingleKielikoeTulos> inputValuesForHakemus = kielikoetuloksetHakemuksittain.get(hakemusOid);
            String personOid = findPersonOidByHakemusOid.apply(hakemusOid);
            if (personOid == null) {
                LOG.warn(String.format("Ei löytynyt hakijaOidia hakemukselle %s tallennettaessa ammatillisen kielikoetuloksia myöntäjälle %s . " +
                    "Lisätään arvosanat %s hakemukselle Suoritusrekisteriin lähetettäviin.", hakemusOid, myontajaOid,
                    inputValuesForHakemus));
                resultsToSendToSure.put(hakemusOid, inputValuesForHakemus);
            } else {
                Optional<Oppija> oppijaFromExistingSureResults = oppijatiedotSuresta.stream().filter(o -> personOid.equals(o.getOppijanumero())).findFirst();
                if (!oppijaFromExistingSureResults.isPresent()) {
                    LOG.info(String.format("Ei löytynyt hakijan %s ammatillisen kielikoesuorituksia myöntäjälle %s . " +
                        "Lisätään arvosanat %s hakemukselle %s Suoritusrekisteriin lähetettäviin.", personOid, myontajaOid, inputValuesForHakemus, hakemusOid));
                    resultsToSendToSure.put(hakemusOid, inputValuesForHakemus);
                } else {
                    addOnlyUpdatedResults(myontajaOid, hakemusOid, inputValuesForHakemus, personOid, oppijaFromExistingSureResults.get());
                }
            }
        }
    }

    private void addOnlyUpdatedResults(String myontajaOid, String hakemusOid, List<SingleKielikoeTulos> inputValuesForHakemus,
                                       String personOid, Oppija oppijaFromExistingSureResults) {
        List<SuoritusJaArvosanat> existingResultsInSure = oppijaFromExistingSureResults.getSuoritukset().stream()
            .filter(SuoritusJaArvosanatWrapper::isAmmatillisenKielikoe)
            .filter(sja -> myontajaOid.equals(sja.getSuoritus().getMyontaja()))
            .collect(Collectors.toList());

        List<SingleKielikoeTulos> tuloksetToSendForHakemus = inputValuesForHakemus.stream().filter(singleKielikoeTulos ->
            containsUpdateFor(existingResultsInSure, singleKielikoeTulos)).collect(Collectors.toCollection(LinkedList::new));
        if (!tuloksetToSendForHakemus.isEmpty()) {
            LOG.info(String.format("Lisätään hakijan %s arvosanat %s hakemukselle %s Suoritusrekisteriin lähetettäviin myöntäjälle %s.",
                personOid, tuloksetToSendForHakemus, hakemusOid, myontajaOid));
            resultsToSendToSure.put(hakemusOid, tuloksetToSendForHakemus);
        } else {
            LOG.info(String.format("Hakijan %s ammatillisen kielikoearvosanat myöntäjälle %s " +
                "löytyvät jo samoilla arvoilla Suoritusrekisteristä, ei päivitetä.", personOid, myontajaOid));
        }
    }

    private boolean containsUpdateFor(List<SuoritusJaArvosanat> existingResultsInSure, SingleKielikoeTulos kielikoeTulosFromInput) {
        String kieli = kielikoeTulosFromInput.kieli();
        List<Arvosana> existingArvosanatForKieli = existingResultsInSure.stream().flatMap(sja -> sja.getArvosanat().stream())
            .filter(a -> a.getLisatieto().equalsIgnoreCase(kieli)).collect(Collectors.toList());
        Collections.sort(existingArvosanatForKieli, new Arvosana.DescendingMyonnettyOrder());
        if (existingArvosanatForKieli.isEmpty()) {
            return true;
        }
        Arvosana latestArvosana = existingArvosanatForKieli.get(0);
        return !kielikoeTulosFromInput.arvioArvosana.equals(latestArvosana.getArvio().getArvosana());
    }

    public Map<String, List<SingleKielikoeTulos>> getResultsToSendToSure() {
        return resultsToSendToSure;
    }
}
