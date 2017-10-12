package fi.vm.sade.valinta.kooste.valintalaskenta.spec;

import com.google.common.collect.Maps;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import rx.Observable;

import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author Jussi Jartamo
 */
public class ValintalaskentaSpec {

    public static class ValintalaskentaMock {
        private final Map<Predicate<List<LaskeDTO>>, BiConsumer<Consumer<String>, Consumer<Throwable>>> filters = Maps.newHashMap();

        public ValintalaskentaMock addFilter(Predicate<List<LaskeDTO>> hakukohdeOidPredicate,
                                              BiConsumer<Consumer<String>, Consumer<Throwable>> callback) {
            filters.put(hakukohdeOidPredicate, callback);
            return this;
        }

        public ValintalaskentaAsyncResource build() {
            ValintalaskentaAsyncResource valintalaskentaAsyncResource = Mockito.mock(ValintalaskentaAsyncResource.class);
            Mockito.when(valintalaskentaAsyncResource.laskeJaSijoittele(Mockito.anyList()))
                    .thenAnswer(answer -> {
                        final List<LaskeDTO> laskeDto = (List<LaskeDTO> ) answer.getArguments()[0];
                        Consumer<String> cb = (Consumer<String>) answer.getArguments()[1];
                        Consumer<Throwable> cb2 = (Consumer<Throwable>) answer.getArguments()[2];
                        if(filters.entrySet().stream().anyMatch(a -> a.getKey().test(laskeDto))) {
                            return Observable.just("OK");
                        } else {
                            return Observable.error(new RuntimeException("No return val!"));
                        }
                    });
            return valintalaskentaAsyncResource;
        }
    }

    public static class LaskentaSeurantaMock {

        public LaskentaSeurantaAsyncResource build() {
            LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource = Mockito.mock(LaskentaSeurantaAsyncResource.class);
            Mockito.doAnswer(answer -> {

                return answer;
            }).when(laskentaSeurantaAsyncResource)
                    .merkkaaHakukohteenTila(Mockito.anyString(), Mockito.anyString(), Mockito.any(HakukohdeTila.class), Mockito.any(Optional.class));
            Mockito.doAnswer(answer -> {

                return answer;
            }).when(laskentaSeurantaAsyncResource)
                    .merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(LaskentaTila.class), Mockito.any(Optional.class));
            Mockito.doAnswer(answer -> {

                return answer;
            }).when(laskentaSeurantaAsyncResource)
                    .merkkaaLaskennanTila(Mockito.anyString(),
                            Mockito.any(LaskentaTila.class),
                            Mockito.any(HakukohdeTila.class), Mockito.any(Optional.class));
            return laskentaSeurantaAsyncResource;
        }
    }

    public static class PeruutettavaMock implements Peruutettava {
        private final Consumer<Throwable> p;
        public PeruutettavaMock(Consumer<Throwable> p) {
            this.p = p;
        }

        @Override
        public void peruuta() {
        }
    }

    public static class ValintaperusteetMock {
        private final Map<BiPredicate<String,Integer>, BiConsumer<Consumer<List<ValintaperusteetDTO>>, Consumer<Throwable>>> filters = Maps.newHashMap();

        public ValintaperusteetMock addFilter(BiPredicate<String,Integer> hakukohdeOidPredicate,
                                              BiConsumer<Consumer<List<ValintaperusteetDTO>>, Consumer<Throwable>> callback) {
            filters.put(hakukohdeOidPredicate, callback);
            return this;
        }

        public ValintaperusteetAsyncResource build(ValintaperusteetAsyncResource valintaperusteetAsyncResource) {
            Mockito.when(valintaperusteetAsyncResource.haeValintaperusteet(Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.any())).then(
                    answer -> {
                        final Consumer<List<ValintaperusteetDTO>> cb =(Consumer<List<ValintaperusteetDTO>>) answer.getArguments()[2];
                        final Consumer<Throwable> cb2 = (Consumer<Throwable>) answer.getArguments()[3];
                        final String hakukohdeOid = (String) answer.getArguments()[0];
                        final Integer valinnanvaihe = (Integer) answer.getArguments()[1];
                        if(filters.entrySet().stream().anyMatch(a -> a.getKey().test(hakukohdeOid,valinnanvaihe))) {
                            filters.entrySet().stream().filter(
                                    a -> a.getKey().test(hakukohdeOid,valinnanvaihe)
                            ).findFirst().ifPresent(
                                    (a) -> {
                                        a.getValue().accept(cb, cb2);
                                    }
                            );
                        } else {
                            cb2.accept(new RuntimeException("Ei paluuarvoa!"));
                        }
                        return new PeruutettavaMock(cb2);
                    }
            );
            return valintaperusteetAsyncResource;
        }
    }
    public static class HakijaryhmaMock {
        private final Map<Predicate<String>, BiConsumer<Consumer<List<ValintaperusteetHakijaryhmaDTO>>, Consumer<Throwable>>> filters = Maps.newHashMap();

        public HakijaryhmaMock addFilter(Predicate<String> hakukohdeOidPredicate,
                                         BiConsumer<Consumer<List<ValintaperusteetHakijaryhmaDTO>>, Consumer<Throwable>> callback) {
            filters.put(hakukohdeOidPredicate, callback);
            return this;
        }

        public ValintaperusteetAsyncResource build(ValintaperusteetAsyncResource valintaperusteetAsyncResource) {
            Mockito.when(valintaperusteetAsyncResource.haeHakijaryhmat(Mockito.anyString(), Mockito.any(), Mockito.any())).then(
                    answer -> {
                        final Consumer<List<ValintaperusteetHakijaryhmaDTO>> cb =(Consumer<List<ValintaperusteetHakijaryhmaDTO>>) answer.getArguments()[1];
                        final Consumer<Throwable> cb2 = (Consumer<Throwable>) answer.getArguments()[2];
                        final String hakukohdeOid = (String) answer.getArguments()[0];
                        if(filters.entrySet().stream().anyMatch(a -> a.getKey().test(hakukohdeOid))) {
                            filters.entrySet().stream().filter(
                                    a -> a.getKey().test(hakukohdeOid)
                            ).findFirst().ifPresent(
                                    (a) -> {
                                        a.getValue().accept(cb, cb2);
                                    }
                            );
                        } else {
                            cb2.accept(new RuntimeException("Ei paluuarvoa!"));
                        }
                        return new PeruutettavaMock(cb2);
                    }
            );
            return valintaperusteetAsyncResource;
        }
    }

    public static class ApplicationMock {
        private final Map<BiPredicate<String,String>, List<Hakemus>> filters = Maps.newHashMap();

        public ApplicationMock addFilter(BiPredicate<String,String> hakuOidJaHakukohdeOidPredicate, List<Hakemus> hakemukset) {
            filters.put(hakuOidJaHakukohdeOidPredicate, hakemukset);
            return this;
        }


        public ApplicationAsyncResource build() {
            ApplicationAsyncResource applicationAsyncResource = Mockito.mock(ApplicationAsyncResource.class);
            Mockito.when(applicationAsyncResource.getApplicationsByOid(Mockito.anyString(), Mockito.anyString())).then(
                    answer -> {
                        final String hakuOid = (String) answer.getArguments()[0];
                        final String hakukohdeOid = (String) answer.getArguments()[1];
                        if(filters.entrySet().stream().anyMatch(a -> a.getKey().test(hakuOid, hakukohdeOid))) {
                            final Optional<List<Hakemus>> hakemukset = filters.entrySet().stream().filter(entry -> entry.getKey().test(hakuOid, hakukohdeOid)).map(entry -> entry.getValue()).findFirst();
                            if (hakemukset.isPresent()) {
                                return Observable.just(hakemukset.get());
                            } else {
                                return Observable.just(Collections.EMPTY_LIST);
                            }
                        } else {
                            return Observable.error(new RuntimeException("Ei paluuarvoa!"));
                        }
                    }
            );
            return applicationAsyncResource;
        }
    }
    public static class SuoritusrekisteriMock {
        private final Map<BiPredicate<String,String>, List<Oppija>> filters = Maps.newHashMap();

        public SuoritusrekisteriMock addFilter(BiPredicate<String,String> hakukohdeOidJaHakuOidPredicate, List<Oppija> oppijat) {
            filters.put(hakukohdeOidJaHakuOidPredicate, oppijat);
            return this;
        }
        public SuoritusrekisteriAsyncResource build() {
            SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource = Mockito.mock(SuoritusrekisteriAsyncResource.class);
            Mockito.when(suoritusrekisteriAsyncResource.getOppijatByHakukohde(
                    Mockito.anyString(), Mockito.anyString())).then(
                    answer -> {
                        final String hakuOid = (String) answer.getArguments()[0];
                        final String hakukohdeOid = (String) answer.getArguments()[1];
                        if(filters.entrySet().stream().anyMatch(a -> a.getKey().test(hakuOid, hakukohdeOid))) {
                            final Optional<List<Oppija>> oppijat = filters.entrySet().stream().filter(entry -> entry.getKey().test(hakuOid, hakukohdeOid)).map(entry -> entry.getValue()).findFirst();
                            if (oppijat.isPresent()) {
                                return Observable.just(oppijat.get());
                            } else {
                                return Observable.just(Collections.EMPTY_LIST);
                            }
                        } else {
                            return Observable.error(new RuntimeException("Ei paluuarvoa!"));
                        }
                    }

            );
            return suoritusrekisteriAsyncResource;
        }
    }

}
