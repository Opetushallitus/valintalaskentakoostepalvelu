package fi.vm.sade.valinta.kooste.laskentakerralla;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetValinnanVaiheDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.*;

import java.util.Date;

public class LaskentaKerrallaTestData {
    final static String PK_KOMO = "1.2.246.562.13.62959769647";

    public static ParametritDTO ohjausparametrit() {
        ParametritDTO p = new ParametritDTO();
        ParametriDTO p0 = new ParametriDTO();
        p0.setDateStart(new Date());
        p.setPH_VLS(p0);
        return p;
    }

    public static ValintaperusteetDTO valintaperusteet(String hakuOid, String tarjoajaOid, String hakuKohdeOid) {
        ValintaperusteetDTO valintaperusteetDTO = new ValintaperusteetDTO();
        ValintaperusteetValinnanVaiheDTO valinnanVaiheDTO = new ValintaperusteetValinnanVaiheDTO();
        valintaperusteetDTO.setHakuOid(hakuOid);
        valintaperusteetDTO.setTarjoajaOid(tarjoajaOid);
        valintaperusteetDTO.setHakukohdeOid(hakuKohdeOid);
        valintaperusteetDTO.setValinnanVaihe(valinnanVaiheDTO);
        return valintaperusteetDTO;
    }

    public static HakukohdeViiteDTO julkaistuHakukohdeViite(String hakukohdeOid, String tarjoajaOid) {
        HakukohdeViiteDTO hakukohdeViiteDTO = new HakukohdeViiteDTO();
        hakukohdeViiteDTO.setOid(hakukohdeOid);
        hakukohdeViiteDTO.setTarjoajaOid(tarjoajaOid);
        hakukohdeViiteDTO.setTila("JULKAISTU");
        return hakukohdeViiteDTO;
    }

    public static Hakemus hakemus(String hakemusOid, String personOid) {
        Hakemus hakemus = new Hakemus();
        hakemus.setOid(hakemusOid);
        hakemus.setPersonOid(personOid);
        return hakemus;
    }

    public static Oppija oppija() {
        return new Oppija();
    }
}
