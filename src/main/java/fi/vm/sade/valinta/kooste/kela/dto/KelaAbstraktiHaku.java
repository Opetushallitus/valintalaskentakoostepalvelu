package fi.vm.sade.valinta.kooste.kela.dto;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.kela.komponentti.*;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public abstract class KelaAbstraktiHaku {

    public static final String SUKUNIMI = "sukunimi";
    public static final String ETUNIMET = "Etunimet";

    private final HakuV1RDTO haku;
    private final PaivamaaraSource paivamaaraSource;

    public KelaAbstraktiHaku(HakuV1RDTO haku, PaivamaaraSource paivamaaraSource) {
        this.haku = haku;
        this.paivamaaraSource = paivamaaraSource;
    }

    public HakuV1RDTO getHaku() {
        return haku;
    }

    protected PaivamaaraSource getPaivamaaraSource() {
        return paivamaaraSource;
    }

    /**
     * @return kela hakuun liittyvat hakemus oidit
     */
    public abstract Collection<String> getHakemusOids();

    public abstract List<String> getPersonOids();

    public abstract Collection<KelaHakijaRivi> createHakijaRivit(
            Date alkuPvm,
            Date loppuPvm,
            String hakuOid,
            KelaProsessi prosessi,
            HenkilotietoSource henkilotietoSource,
            HakukohdeSource hakukohdeSource,
            LinjakoodiSource linjakoodiSource,
            OppilaitosSource oppilaitosSource,
            TutkinnontasoSource tutkinnontasoSource,
            TilaSource tilaSource);
}
