package fi.vm.sade.valinta.kooste.valintatapajono.excel;

import static fi.vm.sade.valinta.kooste.util.KieliUtil.*;

import fi.vm.sade.valinta.kooste.valintatapajono.dto.Kuvaus;
import fi.vm.sade.valintalaskenta.domain.valinta.JarjestyskriteerituloksenTila;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;

@Schema()
public class ValintatapajonoRivi {
  @Schema(required = true, description = "Hakemuksen OID")
  private final String oid;

  @Schema(hidden = true)
  private final String nimi;

  @Schema(
      required = true,
      allowableValues = "HYVAKSYTTAVISSA,HYLATTY,HYVAKSYTTY_HARKINNANVARAISESTI")
  private final String tila;

  @Schema(required = true)
  private final String jonosija;

  @Schema(required = false)
  private final String pisteet;

  @Schema(
      required = false,
      description = "Kielikoodi ja kuvaus. Esim {\"FI\":\"Suomenkielinen kuvaus\",\"SV\":\"...\"}")
  private final Map<String, String> kuvaus;

  @Schema(hidden = true)
  private transient Integer jonosijaNumerona;

  @Schema(hidden = true)
  private transient JarjestyskriteerituloksenTila tilaEnumeraationa;

  public ValintatapajonoRivi() {
    this.oid = null;
    this.nimi = null;
    this.tila = null;
    this.pisteet = null;
    this.kuvaus = null;
    this.jonosija = null;
  }

  public ValintatapajonoRivi(
      String oid,
      String jonosija,
      String nimi,
      String tila,
      String pisteet,
      String fi,
      String sv,
      String en) {

    this.oid = oid;
    this.jonosija = jonosija; // new BigDecimal(jonosija).intValue();
    this.nimi = nimi;
    this.tila = tila;
    this.pisteet = pisteet;
    this.kuvaus =
        Arrays.asList(new Kuvaus(SUOMI, fi), new Kuvaus(RUOTSI, sv), new Kuvaus(ENGLANTI, en))
            .stream()
            .filter(k -> StringUtils.trimToNull(k.getTeksti()) != null)
            .collect(Collectors.toMap(k -> k.getKieli(), k -> k.getTeksti()));
  }

  @Schema(hidden = true)
  public boolean isValidi() {
    boolean hasPisteetTaiJonosija =
        StringUtils.isNotBlank(getJonosija()) || StringUtils.isNotBlank(getPisteet());
    return !isMaarittelematon() || hasPisteetTaiJonosija;
  }

  @Schema(hidden = true)
  public boolean isMaarittelematon() {
    return asTila() == JarjestyskriteerituloksenTila.MAARITTELEMATON;
  }

  public JarjestyskriteerituloksenTila asTila() {
    if (tilaEnumeraationa == null) {
      if (tila == null || StringUtils.isEmpty(tila)) {
        tilaEnumeraationa = JarjestyskriteerituloksenTila.MAARITTELEMATON;
      } else if (ValintatapajonoExcel.VAIHTOEHDOT_KONVERSIO.containsKey(tila)) {
        tilaEnumeraationa = JarjestyskriteerituloksenTila.valueOf(tila);
      } else if (ValintatapajonoExcel.VAIHTOEHDOT_TAKAISINPAIN_KONVERSIO.containsKey(tila)) {
        tilaEnumeraationa =
            JarjestyskriteerituloksenTila.valueOf(
                ValintatapajonoExcel.VAIHTOEHDOT_TAKAISINPAIN_KONVERSIO.get(tila));
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
