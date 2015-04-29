package fi.vm.sade.valinta.kooste.spec.hakemus;

import com.google.common.collect.Maps;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;

/**
 * @author Jussi Jartamo
 */
public class HakemusSpec {

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
}
