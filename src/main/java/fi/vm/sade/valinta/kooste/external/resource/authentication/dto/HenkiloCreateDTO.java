package fi.vm.sade.valinta.kooste.external.resource.authentication.dto;

import java.util.Date;

import org.apache.commons.lang.builder.EqualsBuilder;

import fi.vm.sade.authentication.model.HenkiloTyyppi;

public class HenkiloCreateDTO {

    public final String etunimet;
    public final String kutsumanimi;
    public final  String sukunimi;
    public final String hetu;
    public final Date syntymaaika;
    public final HenkiloTyyppi henkiloTyyppi;

    public HenkiloCreateDTO(String etunimet, String sukunimi, String hetu, Date syntymaaika, HenkiloTyyppi henkiloTyyppi) {
        this.etunimet = etunimet;
        this.kutsumanimi = etunimet;
        this.sukunimi = sukunimi;
        this.hetu = hetu;
        this.syntymaaika = syntymaaika;
        this.henkiloTyyppi = henkiloTyyppi;
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}