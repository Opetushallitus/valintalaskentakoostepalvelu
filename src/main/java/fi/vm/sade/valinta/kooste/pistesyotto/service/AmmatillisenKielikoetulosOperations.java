package fi.vm.sade.valinta.kooste.pistesyotto.service;

import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.tyhja;

import com.google.common.collect.Lists;

import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvio;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Suoritus;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper;
import fi.vm.sade.valinta.kooste.pistesyotto.service.AbstractPistesyottoKoosteService.SingleKielikoeTulos;
import fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AmmatillisenKielikoetulosOperations {
    private static final Logger LOG = LoggerFactory.getLogger(AmmatillisenKielikoetulosOperations.class);
    private final Map<String, Optional<CompositeCommand>> resultsToSendToSure = new HashMap<>();

    public AmmatillisenKielikoetulosOperations(String sourceOid, List<Oppija> oppijatiedotSuresta,
                                               Map<String, List<SingleKielikoeTulos>> kielikoetuloksetHakemuksittain,
                                               Function<String, String> findPersonOidByHakemusOid) {
        for (String hakemusOid : kielikoetuloksetHakemuksittain.keySet()) {
            Optional<CompositeCommand> operationForResult = createOperation(sourceOid, oppijatiedotSuresta, kielikoetuloksetHakemuksittain, findPersonOidByHakemusOid, hakemusOid);
            resultsToSendToSure.put(hakemusOid, operationForResult);
        }

    }

    private Optional<CompositeCommand> createOperation(String sourceOid, List<Oppija> oppijatiedotSuresta, Map<String, List<SingleKielikoeTulos>> kielikoetuloksetHakemuksittain, Function<String, String> findPersonOidByHakemusOid, String hakemusOid) {
        List<SingleKielikoeTulos> inputValuesForHakemus = kielikoetuloksetHakemuksittain.get(hakemusOid);
        if (inputValuesForHakemus.isEmpty()) {
            throw new IllegalArgumentException(String.format("Tyhjä kielikoepistetieto syötetty hakemukselle %s", hakemusOid));
        }
        String personOid = findPersonOidByHakemusOid.apply(hakemusOid);
        if (personOid == null) {
            LOG.warn(String.format("Ei löytynyt hakijaOidia hakemukselle %s tallennettaessa ammatillisen kielikoetuloksia lähteelle %s . " +
                "Lisätään arvosanat %s hakemukselle Suoritusrekisteriin lähetettäviin.", hakemusOid, sourceOid,
                inputValuesForHakemus));
            return Optional.of(createSingleNewArvosanaSave(sourceOid, hakemusOid, inputValuesForHakemus, personOid));
        } else {
            Optional<Oppija> oppijaFromExistingSureResults = oppijatiedotSuresta.stream().filter(o -> personOid.equals(o.getOppijanumero())).findFirst();
            if (!oppijaFromExistingSureResults.isPresent()) {
                LOG.info(String.format("Ei löytynyt hakijan %s ammatillisen kielikoesuorituksia lähteelle %s . " +
                    "Lisätään arvosanat %s hakemukselle %s Suoritusrekisteriin lähetettäviin.", personOid, sourceOid, inputValuesForHakemus, hakemusOid));
                return Optional.of(createSingleNewArvosanaSave(sourceOid, hakemusOid, inputValuesForHakemus, personOid));
            } else {
                return createExistingSuoritusUpdate(sourceOid, hakemusOid, inputValuesForHakemus, personOid, oppijaFromExistingSureResults.get());
            }
        }
    }

    private CompositeCommand createSingleNewArvosanaSave(String sourceOid, String hakemusOid, List<SingleKielikoeTulos> inputValuesForHakemus, String personOid) {
        List<ArvosanaCommand> createArvosanas = inputValuesForHakemus.stream().map(tulos -> new CreateArvosana(tulos, sourceOid)).collect(Collectors.toList());
        return new SaveSuoritus(inputValuesForHakemus.get(0), hakemusOid, personOid, createArvosanas);
    }

    private Optional<CompositeCommand> createExistingSuoritusUpdate(String sourceOid, String hakemusOid, List<SingleKielikoeTulos> inputValuesForHakemus,
                                                                    String personOid, Oppija oppijaFromExistingSureResults) {
        List<SuoritusJaArvosanat> existingResultsOfHakemusInSure = oppijaFromExistingSureResults.getSuoritukset().stream()
            .filter(SuoritusJaArvosanatWrapper::isAmmatillisenKielikoe)
            .filter(sja -> hakemusOid.equals(sja.getSuoritus().getMyontaja()))
            .collect(Collectors.toList());

        if (existingResultsOfHakemusInSure.size() > 1) {
            throw new IllegalStateException(String.format("Suoritusrekisteristä löytyi %d " +
                "ammatillisen kielikokeen suoritusta hakijan %s hakemukselle %s , " +
                "vaikka yhdelle hakemukselle pitäisi olla 0 tai 1 suoritusta: %s",
                existingResultsOfHakemusInSure.size(), personOid, hakemusOid, existingResultsOfHakemusInSure));
        }

        Optional<SuoritusJaArvosanat> existingKielikoeSuoritusJaArvosanat = existingResultsOfHakemusInSure.stream().findFirst();
        boolean deleteEverything = inputValuesForHakemus.stream().allMatch(tulos -> tulos.arvioArvosana == tyhja);

        if (deleteEverything && !existingKielikoeSuoritusJaArvosanat.isPresent()) {
            LOG.info(String.format("Hakijan %s hakemuksen %s ammatillisen kielikoearvosanat ovat tyhjiä eikä Suoritusrekisteristä " +
                "löydy olemassaolevia, joten ei päivitetä niitä lähteen %s päivitysten yhteydessä.", personOid, hakemusOid, sourceOid));
            return Optional.empty();
        }

        if (deleteEverything) {
            List<DeleteArvosana> deleteArvosanas = existingKielikoeSuoritusJaArvosanat.get().getArvosanat().stream().map(DeleteArvosana::new).collect(Collectors.toList());
            DeleteSuoritus deleteSuoritus = new DeleteSuoritus(existingKielikoeSuoritusJaArvosanat.get().getSuoritus(), deleteArvosanas);
            return Optional.of(deleteSuoritus);
        }

        List<CreateArvosana> createArvosanas  = new ArrayList<>();
        List<UpdateArvosana> updateArvosanas = new ArrayList<>();
        List<DeleteArvosana> deleteArvosanas = new ArrayList<>();

        inputValuesForHakemus.forEach(tulosFromInput -> {
            Optional<Arvosana> existingArvosanaOption = existingKielikoeSuoritusJaArvosanat.flatMap(existingSja ->
                existingSja.getArvosanat().stream().filter(a -> a.getLisatieto().equalsIgnoreCase(tulosFromInput.kieli())).findFirst());
            if (existingArvosanaOption.isPresent()) {
                Arvosana existingArvosana = existingArvosanaOption.get();
                if (tulosFromInput.arvioArvosana.equals(tyhja)) {
                    deleteArvosanas.add(new DeleteArvosana(existingArvosana));
                } else if (!tulosFromInput.arvioArvosana.name().equals(existingArvosana.getArvio().getArvosana())) {
                    updateArvosanas.add(new UpdateArvosana(existingArvosana, tulosFromInput));
                }
            } else {
                createArvosanas.add(new CreateArvosana(tulosFromInput, sourceOid));
            }
        });

        List<ArvosanaCommand> allArvosanaCommandForHakemus = Lists.newArrayList();
        allArvosanaCommandForHakemus.addAll(createArvosanas);
        allArvosanaCommandForHakemus.addAll(updateArvosanas);
        allArvosanaCommandForHakemus.addAll(deleteArvosanas);
        SaveSuoritus saveSuoritus = new SaveSuoritus(inputValuesForHakemus.get(0), hakemusOid, personOid, allArvosanaCommandForHakemus);

        if (!allArvosanaCommandForHakemus.isEmpty()) {
            LOG.info(String.format("Päivitetään hakijan %s ammatillisen kielikokeen arvosanoihin tiedot %s hakemukselle %s Suoritusrekisteriin lähetettäviin myöntäjälle %s.",
                personOid, allArvosanaCommandForHakemus, hakemusOid, sourceOid));
            return Optional.of(saveSuoritus);
        } else {
            LOG.info(String.format("Hakijan %s ammatillisen kielikoearvosanat myöntäjälle %s " +
                "löytyvät jo samoilla arvoilla Suoritusrekisteristä, ei päivitetä.", personOid, sourceOid));
            return Optional.empty();
        }
    }

    public static Suoritus createSuoritus(String hakemusOid, String personOid, SingleKielikoeTulos singleKielikoeTulos) {
        String valmistuminen = new SimpleDateFormat(SuoritusJaArvosanatWrapper.SUORITUS_PVM_FORMAT).format(singleKielikoeTulos.valmistuminen);

        Suoritus suoritus = new Suoritus();
        suoritus.setTila(AbstractPistesyottoKoosteService.KIELIKOE_SUORITUS_TILA);
        suoritus.setYksilollistaminen(AbstractPistesyottoKoosteService.KIELIKOE_SUORITUS_YKSILOLLISTAMINEN);
        suoritus.setHenkiloOid(personOid);
        suoritus.setVahvistettu(true);
        suoritus.setSuoritusKieli("FI"); // Hakemuskohtainen kielitieto ei voi olla oikein, koska samalla hakemuksella voi joutua sekä suomen että ruotsin kokeeseen
        suoritus.setMyontaja(hakemusOid);
        suoritus.setKomo(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE);
        suoritus.setValmistuminen(valmistuminen);
        return suoritus;
    }

    public static Arvosana createArvosana(String sourceOid, Suoritus tallennettuSuoritus, String kieli, String arvioArvosana, String valmistuminen) {
        Arvosana arvosana = new Arvosana();
        arvosana.setAine(AbstractPistesyottoKoosteService.KIELIKOE_ARVOSANA_AINE);
        arvosana.setLisatieto(kieli.toUpperCase());
        arvosana.setArvio(new Arvio(arvioArvosana, AmmatillisenKielikoetuloksetSurestaConverter.SURE_ASTEIKKO_HYVAKSYTTY, null));
        arvosana.setSuoritus(tallennettuSuoritus.getId());
        arvosana.setMyonnetty(valmistuminen);
        arvosana.setSource(sourceOid);
        return arvosana;
    }

    public Map<String, Optional<CompositeCommand>> getResultsToSendToSure() {
        return resultsToSendToSure;
    }

    public abstract static class CompositeCommand {
        public abstract Observable<List<Observable<Arvosana>>> createSureOperation(SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource);

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    public abstract static class ArvosanaCommand {
        public abstract Observable<Arvosana> createSureOperation(Suoritus savedSuoritus, SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource);

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    public static class DeleteSuoritus extends CompositeCommand {
        public final Suoritus existingSuoritus;
        public final List<DeleteArvosana> deleteArvosanas;

        public DeleteSuoritus(Suoritus existingSuoritus, List<DeleteArvosana> deleteArvosanas) {
            this.existingSuoritus = existingSuoritus;
            this.deleteArvosanas = deleteArvosanas;
        }

        @Override
        public Observable<List<Observable<Arvosana>>> createSureOperation(SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource) {
            return suoritusrekisteriAsyncResource.deleteSuoritus(existingSuoritus.getId())
                .onErrorResumeNext(t -> Observable.error(new IllegalStateException(String.format(
                    "Kielikokeen suorituksen %s poistaminen Suoritusrekisteristä epäonnistui", existingSuoritus), t)))
                .map(x ->
                    deleteArvosanas.stream().map(arvosanaCommand ->
                        arvosanaCommand.createSureOperation(existingSuoritus, suoritusrekisteriAsyncResource)).collect(Collectors.toList()));
        }
    }

    public static class SaveSuoritus extends CompositeCommand {
        public final Suoritus suoritus;
        public final SingleKielikoeTulos kielikoeTulos;
        public final List<ArvosanaCommand> allArvosanaCommandsForHakemus;

        public SaveSuoritus(SingleKielikoeTulos kielikoeTulos, String hakemusOid, String personOid, List<ArvosanaCommand> allArvosanaOperationsForHakemus) {
            this.kielikoeTulos = kielikoeTulos;
            this.allArvosanaCommandsForHakemus = allArvosanaOperationsForHakemus;
            this.suoritus = createSuoritus(hakemusOid, personOid, kielikoeTulos);
        }

        @Override
        public Observable<List<Observable<Arvosana>>> createSureOperation(final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource) {
            return suoritusrekisteriAsyncResource.postSuoritus(suoritus)
                .onErrorResumeNext(t -> Observable.error(new IllegalStateException(String.format(
                    "Suorituksen %s tallentaminen suoritusrekisteriin epäonnistui", suoritus), t)))
                .map(savedSuoritus ->
                    allArvosanaCommandsForHakemus.stream().map(arvosanaCommand ->
                        arvosanaCommand.createSureOperation(savedSuoritus, suoritusrekisteriAsyncResource)).collect(Collectors.toList()));
        }
    }

    public static class DeleteArvosana extends ArvosanaCommand {
        public final Arvosana existingArvosana;

        public DeleteArvosana(Arvosana existingArvosana) {
            this.existingArvosana = existingArvosana;
        }

        @Override
        public Observable<Arvosana> createSureOperation(Suoritus deletedSuoritus, SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource) {
            return suoritusrekisteriAsyncResource.deleteArvosana(existingArvosana.getId())
                .onErrorResumeNext(t -> Observable.error(new IllegalStateException(String.format(
                    "Kielikokeen arvosanan %s poistaminen Suoritusrekisteristä epäonnistui", existingArvosana), t)))
                .map(x -> existingArvosana);
        }
    }

    public static class CreateArvosana extends ArvosanaCommand {
        public final SingleKielikoeTulos kielikoeTulos;
        private final String sourceOid;

        public CreateArvosana(SingleKielikoeTulos kielikoeTulos, String sourceOid) {
            this.kielikoeTulos = kielikoeTulos;
            this.sourceOid = sourceOid;
        }

        @Override
        public Observable<Arvosana> createSureOperation(Suoritus savedSuoritus, SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource) {
            Arvosana newArvosana = createArvosana(sourceOid, savedSuoritus, kielikoeTulos.kieli(), kielikoeTulos.arvioArvosana.name(),
                new SimpleDateFormat(SuoritusJaArvosanatWrapper.SUORITUS_PVM_FORMAT).format(kielikoeTulos.valmistuminen));
            return suoritusrekisteriAsyncResource.postArvosana(newArvosana)
                .onErrorResumeNext(t -> Observable.error(new IllegalStateException(String.format(
                    "Uuden arvosanan %s luominen Suoritusrekisteriin epäonnistui", newArvosana), t)));
        }
    }

    public static class UpdateArvosana extends ArvosanaCommand {
        public final Arvosana existingArvosana;
        public final SingleKielikoeTulos kielikoeTulos;

        public UpdateArvosana(Arvosana existingArvosana, SingleKielikoeTulos kielikoeTulos) {
            this.existingArvosana = existingArvosana;
            this.kielikoeTulos = kielikoeTulos;
        }

        @Override
        public Observable<Arvosana> createSureOperation(Suoritus savedSuoritus, SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource) {
            Arvosana newArvosana = createArvosana(existingArvosana.getSource(), savedSuoritus, kielikoeTulos.kieli(), kielikoeTulos.arvioArvosana.name(),
                existingArvosana.getMyonnetty());
            return suoritusrekisteriAsyncResource.updateExistingArvosana(existingArvosana.getId(), newArvosana)
                .onErrorResumeNext(t -> Observable.error(new IllegalStateException(String.format(
                    "Olemassaolevan arvosanan %s päivittäminen Suoritusrekisteriin epäonnistui", newArvosana), t)));
        }
    }
}
