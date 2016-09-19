package fi.vm.sade.valinta.kooste.spec.hakemus;

import com.google.common.collect.Maps;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.spec.ConstantsSpec;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;

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
            this.hakemus.getAnswers().setHenkilotiedot(Maps.newHashMap());
        }
        public HakemusBuilder setEtunimiJaSukunimi(String etunimi, String sukunimi) {
            hakemus.getAnswers().getHenkilotiedot().put(HakemusWrapper.ETUNIMET, etunimi);
            hakemus.getAnswers().getHenkilotiedot().put(HakemusWrapper.SUKUNIMI, sukunimi);
            return this;
        }
        public HakemusBuilder setHenkilotunnus(String henkilotunnus) {
            hakemus.getAnswers().getHenkilotiedot().put(HakemusWrapper.HETU, henkilotunnus);
            return this;
        }
        public HakemusBuilder setSyntymaaika(String syntymaaika) {
            hakemus.getAnswers().getHenkilotiedot().put(HakemusWrapper.SYNTYMAAIKA, syntymaaika);
            return this;
        }
        public HakemusBuilder setSahkoposti(String sahkoposti) {
            hakemus.getAnswers().getHenkilotiedot().put(HakemusWrapper.SAHKOPOSTI, sahkoposti);
            return this;
        }
        public HakemusBuilder setVainSahkoinenViestinta(boolean vainSahkoinenViestinta) {
            hakemus.getAnswers().getLisatiedot().put(HakemusWrapper.LUPA_SAHKOISEEN_VIESTINTAAN, vainSahkoinenViestinta ? "true" : "false");
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
            hakemus.getAnswers().getLisatiedot().put(HakemusWrapper.ASIOINTIKIELI, asiointikieli);
            return this;
        }
        public HakemusBuilder addHakutoive(String hakukohdeOid) {
            hakemus.getAnswers().getHakutoiveet().put("preference"+ (index++) +"-Koulutus-id", hakukohdeOid);
            return this;
        }

        public Hakemus build() {
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
