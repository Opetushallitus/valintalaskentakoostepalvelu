package fi.vm.sade.valinta.kooste.spec.hakemus;

import com.google.common.collect.Maps;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;

/**
 * @author Jussi Jartamo
 */
public class HakemusSpec {

    public static class AdditionalDataBuilder {
        private ApplicationAdditionalDataDTO applicationAdditionalDataDTO = new ApplicationAdditionalDataDTO();
        public AdditionalDataBuilder() {
            applicationAdditionalDataDTO.setAdditionalData(Maps.newHashMap());
        }
        public AdditionalDataBuilder setOid(String oid) {
            applicationAdditionalDataDTO.setOid(oid);
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
        public HakemusBuilder setOid(String oid) {
            hakemus.setOid(oid);
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
