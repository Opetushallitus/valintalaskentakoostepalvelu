package fi.vm.sade.valinta.kooste.spec.valintaperusteet;

import com.google.common.collect.Lists;
import fi.vm.sade.service.valintaperusteet.dto.*;
import fi.vm.sade.service.valintaperusteet.dto.model.Funktiotyyppi;
import fi.vm.sade.valinta.kooste.spec.ConstantsSpec;
import org.mockito.Mockito;

import java.util.Arrays;

/**
 * @author Jussi Jartamo
 */
public class ValintaperusteetSpec extends ConstantsSpec {

    // ValintaperusteetDTO
    public static class ValintaperusteetBuilder {
        private final ValintaperusteetDTO valintaperusteetDTO = new ValintaperusteetDTO();
        private final ValintaperusteetValinnanVaiheDTO vv = new ValintaperusteetValinnanVaiheDTO();

        public ValintaperusteetDTO build() {
            valintaperusteetDTO.setValinnanVaihe(vv);
            return valintaperusteetDTO;
        }
    }

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
        public ValintakoeDTOBuilder setTunniste(String tunniste) {
            valintaperusteDTO.setTunniste(tunniste);
            return this;
        }
        public ValintakoeDTOBuilder setNimi(String nimi) {
            valintaperusteDTO.setNimi(nimi);
            return this;
        }
        public ValintakoeDTOBuilder setSelvitettyTunniste(String tunniste) {
            valintaperusteDTO.setSelvitettyTunniste(tunniste);
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
            ValintakoeDTO vk = Mockito.spy(new ValintakoeDTO());
            vk.setOid(valintakoeOid);
            vk.setTunniste(valintakoeOid);
            Mockito.when(vk.getSelvitettyTunniste()).thenAnswer(a -> {
               throw new UnsupportedOperationException("Selvitettyä tunnistetta ei ole laitettu hakukohde ja valintakoe resurssin läpi!");
            });
            hakukohdeJaValintakoe.getValintakoeDTO().add(vk);
            return this;
        }
        public HakukohdeJaValintakoeDTO build() {
            return hakukohdeJaValintakoe;
        }
    }

    public static class HakukohdeViiteBuilder {
        private HakukohdeViiteDTO hakukohdeViiteDTO = new HakukohdeViiteDTO();

        public HakukohdeViiteBuilder setHakukohdeOid(String hakukohdeOid) {
            hakukohdeViiteDTO.setOid(hakukohdeOid);
            return this;
        }

        public HakukohdeViiteDTO build() {
            hakukohdeViiteDTO.setTarjoajaOid(TARJOAJA1);
            hakukohdeViiteDTO.setTila("JULKAISTU");
            return hakukohdeViiteDTO;
        }
    }
    public static HakukohdeViiteBuilder hakukohdeviite() {
        return new HakukohdeViiteBuilder();
    }
    public static ValintaperusteBuilder valintaperuste() {
        return new ValintaperusteBuilder();
    }
    public static ValintaperusteetBuilder valintaperusteet() {
        return new ValintaperusteetBuilder();
    }
    public static ValintakoeDTOBuilder valintakoe() {
        return new ValintakoeDTOBuilder();
    }
    public static HakukohdeJaValintakoeBuilder hakukohdeJaValintakoe() {
        return new HakukohdeJaValintakoeBuilder();
    }
}
