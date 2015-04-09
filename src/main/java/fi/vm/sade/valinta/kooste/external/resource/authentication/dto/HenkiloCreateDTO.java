package fi.vm.sade.valinta.kooste.external.resource.authentication.dto;

import java.util.Date;

import fi.vm.sade.authentication.model.Kielisyys;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import fi.vm.sade.authentication.model.HenkiloTyyppi;

public class HenkiloCreateDTO {

    public final String etunimet;
    public final String kutsumanimi;
    public final  String sukunimi;
    public final String hetu;
    public final Date syntymaaika;
    public final String oidHenkilo;
    public final HenkiloTyyppi henkiloTyyppi;
    public final String sukupuoli;
    public final Kielisyys aidinkieli;

    public HenkiloCreateDTO(String aidinkieli, String sukupuoli, String etunimet, String sukunimi, String hetu, Date syntymaaika, String oidHenkilo, HenkiloTyyppi henkiloTyyppi) {
        if(StringUtils.trimToNull(aidinkieli)==null){
            this.aidinkieli = null;
        } else {
            this.aidinkieli = new Kielisyys();
            this.aidinkieli.setKieliKoodi(aidinkieli);
        }
        this.sukupuoli = sukupuoli;
        this.etunimet = etunimet;
        this.kutsumanimi = etunimet;
        this.sukunimi = sukunimi;
        this.hetu = hetu;
        this.syntymaaika = syntymaaika;
        this.oidHenkilo = oidHenkilo;
        this.henkiloTyyppi = henkiloTyyppi;
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}