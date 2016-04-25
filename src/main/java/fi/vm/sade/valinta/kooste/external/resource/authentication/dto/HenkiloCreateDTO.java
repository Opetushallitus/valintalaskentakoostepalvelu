package fi.vm.sade.valinta.kooste.external.resource.authentication.dto;

import java.util.Date;

import fi.vm.sade.authentication.model.Kansalaisuus;
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
    public final String sukupuoli; //MIES tai NAINEN
    public final Kielisyys aidinkieli;
    public final Kielisyys asiointiKieli;
    public final Kansalaisuus kansalaisuus;


    public HenkiloCreateDTO(String aidinkieli, String sukupuoli, String etunimet, String sukunimi, String hetu, Date syntymaaika, String oidHenkilo,
                            HenkiloTyyppi henkiloTyyppi, String asiointiKieli, String kansalaisuus) {
        this.aidinkieli = createKielisyys(aidinkieli);
        this.sukupuoli = sukupuoli;
        this.etunimet = etunimet;
        this.kutsumanimi = etunimet;
        this.sukunimi = sukunimi;
        this.hetu = hetu;
        this.syntymaaika = syntymaaika;
        this.oidHenkilo = oidHenkilo;
        this.henkiloTyyppi = henkiloTyyppi;
        this.asiointiKieli = createKielisyys(asiointiKieli);
        this.kansalaisuus = createKansalaisuus(kansalaisuus);
    }

    private Kielisyys createKielisyys(String kielikoodi) {
        if (null == StringUtils.trimToNull(kielikoodi)) {
            return null;
        } else {
            Kielisyys kielisyys = new Kielisyys();
            kielisyys.setKieliKoodi(kielikoodi.toLowerCase());
            return kielisyys;
        }
    }

    private Kansalaisuus createKansalaisuus(String maakoodi) {
        if (null == StringUtils.trimToNull(maakoodi)) {
            return null;
        } else {
            Kansalaisuus kansalaisuus = new Kansalaisuus();
            kansalaisuus.setKansalaisuusKoodi(maakoodi.toLowerCase());
            return kansalaisuus;
        }
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