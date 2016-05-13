package fi.vm.sade.valinta.kooste.valintatapajono.excel;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import fi.vm.sade.valinta.kooste.valintatapajono.dto.Kuvaus;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Maps;

import static fi.vm.sade.valinta.kooste.util.KieliUtil.*;

import fi.vm.sade.valintalaskenta.domain.valinta.JarjestyskriteerituloksenTila;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;

@ApiModel()
public class ValintatapajonoRivi {
    @ApiModelProperty(required = true, value = "Hakemuksen OID")
    private final String oid;
    @ApiModelProperty(hidden = true)
    private final String nimi;
    @ApiModelProperty(required = true, allowableValues = "HYVAKSYTTAVISSA,HYLATTY,HYVAKSYTTY_HARKINNANVARAISESTI")
    private final String tila;
    @ApiModelProperty(required = true)
    private final String jonosija;
    @ApiModelProperty(required = false)
    private final String pisteet;
    @ApiModelProperty(required = false, value = "Kielikoodi ja kuvaus. Esim {\"FI\":\"Suomenkielinen kuvaus\",\"SV\":\"...\"}")
    private final Map<String, String> kuvaus;

    @ApiModelProperty(hidden = true)
    private transient Integer jonosijaNumerona;
    @ApiModelProperty(hidden = true)
    private transient JarjestyskriteerituloksenTila tilaEnumeraationa;

    public ValintatapajonoRivi() {
        this.oid = null;
        this.nimi = null;
        this.tila = null;
        this.pisteet = null;
        this.kuvaus = null;
        this.jonosija = null;
    }

    public ValintatapajonoRivi(String oid, String jonosija, String nimi,
                               String tila, String pisteet, String fi, String sv, String en) {

        this.oid = oid;
        this.jonosija = jonosija; //new BigDecimal(jonosija).intValue();
        this.nimi = nimi;
        this.tila = tila;
        this.pisteet = pisteet;
        this.kuvaus = Arrays.asList(new Kuvaus(SUOMI, fi), new Kuvaus(RUOTSI, sv), new Kuvaus(ENGLANTI, en)).stream()
                .filter(k -> StringUtils.trimToNull(k.getTeksti()) != null)
                .collect(Collectors.toMap(
                        k -> k.getKieli(), k -> k.getTeksti()
                ));
    }

    @ApiModelProperty(hidden = true)
    public boolean isValidi() {
        return asTila() != JarjestyskriteerituloksenTila.MAARITTELEMATON;
    }

    public JarjestyskriteerituloksenTila asTila() {
        if (tilaEnumeraationa == null) {
            if (tila == null) {
                tilaEnumeraationa = JarjestyskriteerituloksenTila.MAARITTELEMATON;
            } else if (ValintatapajonoExcel.VAIHTOEHDOT_KONVERSIO.containsKey(tila)) {
                tilaEnumeraationa = JarjestyskriteerituloksenTila.valueOf(tila);
            } else if (ValintatapajonoExcel.VAIHTOEHDOT_TAKAISINPAIN_KONVERSIO.containsKey(tila)) {
                tilaEnumeraationa = JarjestyskriteerituloksenTila.valueOf(ValintatapajonoExcel.VAIHTOEHDOT_TAKAISINPAIN_KONVERSIO.get(tila));
            }
        }
        return tilaEnumeraationa;
    }

    public int asJonosija() {
        if (jonosijaNumerona == null) {
            if (StringUtils.trimToNull(jonosija) == null) {
                jonosijaNumerona = 0;
            } else {
                try {
                    jonosijaNumerona = new BigDecimal(jonosija).intValue();
                } catch (Exception e) {
                    jonosijaNumerona = 0;
                }
            }
        }
        return jonosijaNumerona;
    }

    public String getTila() {
        return tila;
    }

    public String getJonosija() {
        return jonosija;
    }

    public Map<String, String> getKuvaus() {
        return kuvaus;
    }

    public String getNimi() {
        return nimi;
    }

    public String getOid() {
        return oid;
    }

    public String getPisteet() {
        return pisteet;
    }
}
