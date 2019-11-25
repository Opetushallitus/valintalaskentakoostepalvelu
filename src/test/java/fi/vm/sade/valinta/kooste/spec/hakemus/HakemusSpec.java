package fi.vm.sade.valinta.kooste.spec.hakemus;

import com.google.common.collect.Maps;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakutoive;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.spec.ConstantsSpec;
import fi.vm.sade.valinta.kooste.util.AtaruHakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakuappHakemusWrapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jussi Jartamo
 */
public class HakemusSpec extends ConstantsSpec {

    public static class AdditionalDataBuilder {
        private ApplicationAdditionalDataDTO applicationAdditionalDataDTO = new ApplicationAdditionalDataDTO();
        public AdditionalDataBuilder() {
            applicationAdditionalDataDTO.setAdditionalData(Maps.newHashMap());
        }
        public AdditionalDataBuilder setPersonOid(String personOid) {
            applicationAdditionalDataDTO.setPersonOid(personOid);
            return this;
        }
        public AdditionalDataBuilder setOid(String oid) {
            applicationAdditionalDataDTO.setOid(oid);
            return this;
        }
        public AdditionalDataBuilder setEtunimiJaSukunimi(String etunimi, String sukunimi) {
            applicationAdditionalDataDTO.setLastName(sukunimi);
            applicationAdditionalDataDTO.setFirstNames(etunimi);
            return this;
        }
        public AdditionalDataBuilder addLisatieto(String avain, String arvo) {
            applicationAdditionalDataDTO.getAdditionalData().put(avain, arvo);
            return this;
        }
        public ApplicationAdditionalDataDTO build() {
            return applicationAdditionalDataDTO;
        }
    }

    public static class HakemusBuilder {
        private final Hakemus hakemus;
        private int index = 1;
        public HakemusBuilder( ) {
            this.hakemus = new Hakemus();
            this.hakemus.setAnswers(new Answers());
            this.hakemus.getAnswers().setHakutoiveet(Maps.newHashMap());
            this.hakemus.getAnswers().setMaksuvelvollisuus(Maps.newHashMap());
            this.hakemus.getAnswers().setHenkilotiedot(Maps.newHashMap());
        }
        public HakemusBuilder setEtunimiJaSukunimi(String etunimi, String sukunimi) {
            hakemus.getAnswers().getHenkilotiedot().put(HakuappHakemusWrapper.ETUNIMET, etunimi);
            hakemus.getAnswers().getHenkilotiedot().put(HakuappHakemusWrapper.SUKUNIMI, sukunimi);
            return this;
        }
        public HakemusBuilder setHenkilotunnus(String henkilotunnus) {
            hakemus.getAnswers().getHenkilotiedot().put(HakuappHakemusWrapper.HETU, henkilotunnus);
            return this;
        }
        public HakemusBuilder setSyntymaaika(String syntymaaika) {
            hakemus.getAnswers().getHenkilotiedot().put(HakuappHakemusWrapper.SYNTYMAAIKA, syntymaaika);
            return this;
        }
        public HakemusBuilder setSahkoposti(String sahkoposti) {
            hakemus.getAnswers().getHenkilotiedot().put(HakuappHakemusWrapper.SAHKOPOSTI, sahkoposti);
            return this;
        }
        public HakemusBuilder setVainSahkoinenViestinta(boolean vainSahkoinenViestinta) {
            hakemus.getAnswers().getLisatiedot().put(HakuappHakemusWrapper.LUPA_SAHKOISEEN_VIESTINTAAN, vainSahkoinenViestinta ? "true" : "false");
            return this;
        }
        public HakemusBuilder setOid(String oid) {
            hakemus.setOid(oid);
            return this;
        }

        public HakemusBuilder setPersonOid(String personOid) {
            hakemus.setPersonOid(personOid);
            return this;
        }
        public HakemusBuilder setAsiointikieli(String asiointikieli) {
            hakemus.getAnswers().getLisatiedot().put(HakuappHakemusWrapper.ASIOINTIKIELI, asiointikieli);
            return this;
        }
        public HakemusBuilder addHakutoive(String hakukohdeOid) {
            hakemus.getAnswers().getHakutoiveet().put("preference"+ (index++) +"-Koulutus-id", hakukohdeOid);
            return this;
        }

        public HakemusWrapper build() {
            hakemus.setState(ApplicationAsyncResource.DEFAULT_STATES.get(0));
            return new HakuappHakemusWrapper(hakemus);
        }

        public Hakemus buildHakuappHakemus() {
            hakemus.setState(ApplicationAsyncResource.DEFAULT_STATES.get(0));
            return hakemus;
        }
    }

    public static class AtaruHakemusBuilder {
        private final AtaruHakemus hakemus;
        private final HenkiloPerustietoDto henkilo;

        public AtaruHakemusBuilder() {
            hakemus = new AtaruHakemus();
            hakemus.setKeyValues(Maps.newHashMap());
            henkilo = null;
        }

        public AtaruHakemusBuilder setAsiointikieli(String asiointikieli) {
            hakemus.setAsiointikieli(asiointikieli);
            return this;
        }

        public AtaruHakemusBuilder(String oid, String personOid, String hetu) {
            hakemus = new AtaruHakemus();
            hakemus.setKeyValues(Maps.newHashMap());
            hakemus.setHakemusOid(oid);
            henkilo = new HenkiloPerustietoDto();
            henkilo.setOidHenkilo(personOid);
            henkilo.setHetu(hetu);
        }

        public AtaruHakemusBuilder setOid(String oid) {
            hakemus.setHakemusOid(oid);
            return this;
        }

        public AtaruHakemusBuilder setHakutoiveet(List<String> oids) {
            hakemus.setHakutoiveet(
                    oids
                            .stream()
                            .map(oid -> {
                                AtaruHakutoive hakutoive = new AtaruHakutoive();
                                hakutoive.setHakukohdeOid(oid);
                                return hakutoive;
                            })
                            .collect(Collectors.toList()));
            return this;
        }
        public AtaruHakemusBuilder setMaksuvelvollisuus(Map<String, String> maksuvelvollisuus) {
            hakemus.setMaksuvelvollisuus(maksuvelvollisuus);
            return this;
        }
        public AtaruHakemusBuilder setSuomalainenPostinumero(String postinumero) {
            hakemus.getKeyValues().put("postal-code", postinumero);
            return this;
        }
        public AtaruHakemusBuilder setCountryOfResidence(String countryCode) {
            hakemus.getKeyValues().put("country-of-residence", countryCode);
            return this;
        }
        public AtaruHakemusBuilder setPersonOid(String oid) {
            henkilo.setOidHenkilo(oid);
            return this;
        }
        public AtaruHakemusBuilder setHakemusPersonOid(String oid) {
            hakemus.setPersonOid(oid);
            return this;
        }


        public HakemusWrapper build() {
            return new AtaruHakemusWrapper(hakemus, henkilo);
        }
        public AtaruHakemus getHakemus() {
            return hakemus;
        }
    }

    public static HakemusBuilder hakemus() {
        return new HakemusBuilder();
    }
    public static AdditionalDataBuilder lisatiedot() {
        return new AdditionalDataBuilder();
    }
}
