package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.camel.TypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;

public class LaskentaWrapper {
    private final static Logger LOG = LoggerFactory.getLogger(LaskentaWrapper.class);
    private final boolean onkoToitaOikeaMaara;
    private final boolean onkoLaskemattakinTehtyEliHakukohteelleEiOllutHakemuksiaTaiValintaperusteita;
    private final boolean onkoOhitettavaEliValintaperusteetTaiHakemuksetTaiLisatiedotPuuttui;
    private final List<ValintaperusteetDTO> valintaperusteet;
    private final List<Hakemus> hakemukset;
    private final List<ApplicationAdditionalDataDTO> lisatiedot;
    private final List<ValintaperusteetHakijaryhmaDTO> hakijaryhmat;
    private final String hakukohdeOid;
    private final LaskentaStartParams laskenta;

    public LaskentaWrapper(List<LaskentaJaValintaperusteetJaHakemukset> tyot) {
        if (tyot == null || tyot.size() != 4) {
            this.onkoToitaOikeaMaara = false;
            this.valintaperusteet = null;
            this.hakemukset = null;
            this.lisatiedot = null;
            this.onkoLaskemattakinTehtyEliHakukohteelleEiOllutHakemuksiaTaiValintaperusteita = false;
            this.onkoOhitettavaEliValintaperusteetTaiHakemuksetTaiLisatiedotPuuttui = false;
            this.hakukohdeOid = null;
            this.laskenta = null;
            this.hakijaryhmat = null;
        } else {
            this.onkoToitaOikeaMaara = true;
            this.valintaperusteet = extractValintaperusteet(tyot);
            this.hakemukset = extractHakemukset(tyot);
            this.lisatiedot = extractLisatiedot(tyot);
            this.hakukohdeOid = extractHakukohdeOid(tyot);
            this.laskenta = extractLaskenta(tyot);
            this.hakijaryhmat = extractHakijaryhmat(tyot);
            this.onkoLaskemattakinTehtyEliHakukohteelleEiOllutHakemuksiaTaiValintaperusteita = isOnkoJokuDataJoukkoTyhja();
            this.onkoOhitettavaEliValintaperusteetTaiHakemuksetTaiLisatiedotPuuttui = isOnkoJokuDataJoukkoNullReferenssi();
        }
    }

    public List<Hakemus> getHakemukset() {
        return hakemukset;
    }

    public List<ValintaperusteetHakijaryhmaDTO> getHakijaryhmat() {
        return hakijaryhmat;
    }

    public LaskentaStartParams getLaskenta() {
        return laskenta;
    }

    public String getHakukohdeOid() {
        return hakukohdeOid;
    }

    public boolean isOnkoLaskemattakinTehtyEliHakukohteelleEiOllutHakemuksiaTaiValintaperusteita() {
        return onkoLaskemattakinTehtyEliHakukohteelleEiOllutHakemuksiaTaiValintaperusteita;
    }

    public boolean isOnkoOhitettavaEliValintaperusteetTaiHakemuksetTaiLisatiedotPuuttui() {
        return onkoOhitettavaEliValintaperusteetTaiHakemuksetTaiLisatiedotPuuttui;
    }

    public List<ApplicationAdditionalDataDTO> getLisatiedot() {
        return lisatiedot;
    }

    public boolean isOnkoToitaOikeaMaara() {
        return onkoToitaOikeaMaara;
    }

    public List<HakemusDTO> convertHakemuksetToHakemuksetDTO(TypeConverter converter) {
        List<HakemusDTO> hx = getHakemuksetLisatiedoilla().parallelStream()
                .map(h -> converter.tryConvertTo(HakemusDTO.class, h))
                .collect(Collectors.toList());
        return hx;
    }

    public List<ValintaperusteetDTO> getValintaperusteet() {
        return valintaperusteet;
    }

    private boolean isOnkoJokuDataJoukkoTyhja() {
        return (valintaperusteet != null && valintaperusteet.isEmpty()) || (hakemukset != null && hakemukset.isEmpty());
    }

    private boolean isOnkoJokuDataJoukkoNullReferenssi() {
        return valintaperusteet == null || hakemukset == null || lisatiedot == null || hakijaryhmat == null;
    }

    private String extractHakukohdeOid(List<LaskentaJaValintaperusteetJaHakemukset> tyot) {
        return tyot.iterator().next().getHakukohdeOid();
    }

    private LaskentaStartParams extractLaskenta(
            List<LaskentaJaValintaperusteetJaHakemukset> tyot) {
        return tyot.iterator().next().getLaskenta();
    }

    private List<ValintaperusteetDTO> extractValintaperusteet(List<LaskentaJaValintaperusteetJaHakemukset> tyot) {
        for (LaskentaJaValintaperusteetJaHakemukset v : tyot) {
            if (v.getValintaperusteet() != null) {
                return v.getValintaperusteet();
            }
        }
        return null;
    }

    private List<Hakemus> extractHakemukset(List<LaskentaJaValintaperusteetJaHakemukset> tyot) {
        for (LaskentaJaValintaperusteetJaHakemukset v : tyot) {
            if (v.getHakemukset() != null) {
                return v.getHakemukset();
            }
        }
        return null;
    }

    private List<ValintaperusteetHakijaryhmaDTO> extractHakijaryhmat(List<LaskentaJaValintaperusteetJaHakemukset> tyot) {
        for (LaskentaJaValintaperusteetJaHakemukset v : tyot) {
            if (v.getHakijaryhmat() != null) {
                return v.getHakijaryhmat();
            }
        }
        return null;
    }

    private List<ApplicationAdditionalDataDTO> extractLisatiedot(List<LaskentaJaValintaperusteetJaHakemukset> tyot) {
        for (LaskentaJaValintaperusteetJaHakemukset v : tyot) {
            if (v.getLisatiedot() != null) {
                return v.getLisatiedot();
            }
        }
        return null;
    }

    private List<Hakemus> getHakemuksetLisatiedoilla() {
        final Map<String, ApplicationAdditionalDataDTO> appData = lisatiedot
                .parallelStream().collect(Collectors.toMap(ApplicationAdditionalDataDTO::getOid, i -> i));
        return hakemukset
                .parallelStream()
                .map(h -> {
                    Map<String, String> addData = appData.get(h.getOid()).getAdditionalData();
                    if (addData == null) {
                        LOG.warn("Lisatietoja ei saatu hakemukselle {}", h.getOid());
                        addData = Collections.emptyMap();
                    }
                    h.setAdditionalInfo(addData);
                    return h;
                }).collect(Collectors.toList());
    }
}
