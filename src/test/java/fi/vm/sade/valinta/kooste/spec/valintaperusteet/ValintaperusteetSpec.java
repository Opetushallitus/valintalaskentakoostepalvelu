package fi.vm.sade.valinta.kooste.spec.valintaperusteet;

import com.google.common.collect.Lists;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;

/**
 * @author Jussi Jartamo
 */
public class ValintaperusteetSpec {

    public static class ValintakoeDTOBuilder {
        private final ValintakoeDTO valintaperusteDTO = new ValintakoeDTO();

        public ValintakoeDTOBuilder setOid(String oid) {
            valintaperusteDTO.setOid(oid);
            return this;
        }

        public ValintakoeDTO build() {
            return valintaperusteDTO;
        }
    }
    public static class HakukohdeJaValintakoeBuilder {
        private HakukohdeJaValintakoeDTO hakukohdeJaValintakoe = new HakukohdeJaValintakoeDTO("", Lists.newArrayList());

        public HakukohdeJaValintakoeBuilder setHakukohdeOid(String hakukohdeOid) {
            hakukohdeJaValintakoe = new HakukohdeJaValintakoeDTO(hakukohdeOid,hakukohdeJaValintakoe.getValintakoeDTO());
            return this;
        }
        public HakukohdeJaValintakoeBuilder addValintakoe(String valintakoeOid) {
            ValintakoeDTO vk = new ValintakoeDTO();
            vk.setOid(valintakoeOid);
            hakukohdeJaValintakoe.getValintakoeDTO().add(vk);
            return this;
        }
        public HakukohdeJaValintakoeDTO build() {
            return hakukohdeJaValintakoe;
        }
    }


    public static ValintakoeDTOBuilder valintakoe() {
        return new ValintakoeDTOBuilder();
    }
    public static HakukohdeJaValintakoeBuilder hakukohdeJaValintakoe() {
        return new HakukohdeJaValintakoeBuilder();
    }
}
