package fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class HenkiloCreateDTO implements Serializable {

    public final String etunimet;
    public final String kutsumanimi;
    public final String sukunimi;
    public final String hetu;
    public final String syntymaaika;
    public final String oidHenkilo;
    public final HenkiloTyyppi henkiloTyyppi;
    public final String sukupuoli; //MIES tai NAINEN
    public final KielisyysDto aidinkieli;
    public final KielisyysDto asiointiKieli;
    public final Set<KansalaisuusDto> kansalaisuus;


    public HenkiloCreateDTO(String aidinkieli, String sukupuoli, String etunimet, String sukunimi, String hetu, String syntymaaika, String oidHenkilo,
                            HenkiloTyyppi henkiloTyyppi, String asiointiKieli, String kansalaisuus) {
        this.aidinkieli = createKielisyys(aidinkieli);
        this.sukupuoli = sukupuoli;
        this.etunimet = etunimet;
        this.kutsumanimi = etunimet;
        this.sukunimi = sukunimi;
        this.hetu = hetu;
        this.syntymaaika = syntymaaika;
        this.oidHenkilo = oidHenkilo.isEmpty() ? null : oidHenkilo;
        this.henkiloTyyppi = henkiloTyyppi;
        this.asiointiKieli = createKielisyys(asiointiKieli);
        this.kansalaisuus = createKansalaisuusSet(kansalaisuus);
    }

    private KielisyysDto createKielisyys(String kielikoodi) {
        if (null == StringUtils.trimToNull(kielikoodi)) {
            return null;
        } else {
            return new KielisyysDto(kielikoodi.toLowerCase());
        }
    }

    private Set<KansalaisuusDto> createKansalaisuusSet(String maakoodi) {
        if (null == StringUtils.trimToNull(maakoodi)) {
            return null;
        } else {
            Set<KansalaisuusDto> kansalaisuusSet = new HashSet<>();
            KansalaisuusDto kansalaisuus = new KansalaisuusDto(maakoodi.toLowerCase());
            kansalaisuusSet.add(kansalaisuus);
            return kansalaisuusSet;
        }
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        String overrideHetu = hetu == null ? null : "'***HETU***'";
        return "HenkiloCreateDTO{" +
                "etunimet='" + etunimet + '\'' +
                ", kutsumanimi='" + kutsumanimi + '\'' +
                ", sukunimi='" + sukunimi + '\'' +
                ", hetu=" + overrideHetu +
                ", syntymaaika='" + syntymaaika + '\'' +
                ", oidHenkilo='" + oidHenkilo + '\'' +
                ", henkiloTyyppi=" + henkiloTyyppi +
                ", sukupuoli='" + sukupuoli + '\'' +
                ", aidinkieli=" + aidinkieli +
                ", asiointiKieli=" + asiointiKieli +
                ", kansalaisuus=" + kansalaisuus +
                '}';
    }
}