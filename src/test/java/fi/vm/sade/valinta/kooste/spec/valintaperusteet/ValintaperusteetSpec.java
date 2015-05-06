package fi.vm.sade.valinta.kooste.spec.valintaperusteet;

import com.google.common.collect.Lists;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.model.Funktiotyyppi;

import java.util.Arrays;

/**
 * @author Jussi Jartamo
 */
public class ValintaperusteetSpec {

    public static class ValintaperusteBuilder {
        private final ValintaperusteDTO valintaperusteDTO = new ValintaperusteDTO();
        public ValintaperusteBuilder setTunniste(String tunniste) {
            valintaperusteDTO.setTunniste(tunniste);
            return this;
        }
        public ValintaperusteBuilder setOsallistumisenTunniste(String tunniste) {
            valintaperusteDTO.setOsallistuminenTunniste(tunniste);
            return this;
        }
        public ValintaperusteBuilder setKuvaus(String kuvaus) {
            valintaperusteDTO.setKuvaus(kuvaus);
            return this;
        }
        public ValintaperusteBuilder setLukuarvofunktio() {
            valintaperusteDTO.setFunktiotyyppi(Funktiotyyppi.LUKUARVOFUNKTIO);
            return this;
        }
        public ValintaperusteBuilder setArvot(String ... arvot) {
            valintaperusteDTO.setArvot(Arrays.asList(arvot));
            return this;
        }
        public ValintaperusteDTO build() {
            return valintaperusteDTO;
        }
    }

    public static class ValintakoeDTOBuilder {
        private final ValintakoeDTO valintaperusteDTO = new ValintakoeDTO();

        public ValintakoeDTOBuilder setOid(String oid) {
            valintaperusteDTO.setOid(oid);
            return this;
        }
        public ValintakoeDTOBuilder setKaikkiKutsutaan() {
            valintaperusteDTO.setKutsutaankoKaikki(true);
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


    public static ValintaperusteBuilder valintaperusteet() {
        return new ValintaperusteBuilder();
    }
    public static ValintakoeDTOBuilder valintakoe() {
        return new ValintakoeDTOBuilder();
    }
    public static HakukohdeJaValintakoeBuilder hakukohdeJaValintakoe() {
        return new HakukohdeJaValintakoeBuilder();
    }
}
