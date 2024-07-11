package fi.vm.sade.valinta.kooste.external.resource.koski;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KoskiOppijaQuery {

  private String type = "valintalaskenta";

  private String format = "application/json";

  @SerializedName("rajapäivä")
  private String dateLimit;

  private List<String> oppijaOids = new ArrayList<>();

  // Tämä on default arvo myös Kosken puolella
  private String koulutusmuoto = "ammatillinenkoulutus";

  // Nämä ovat defaulttina myös Kosken puolella
  private List<String> suoritustyypit =
      Arrays.asList("ammatillinentutkinto", "ammatillinentutkintoosittainen");

  public KoskiOppijaQuery() {}

  public KoskiOppijaQuery(List<String> oppijaOids, String dateLimit) {
    this.oppijaOids = oppijaOids;
    this.dateLimit = dateLimit;
  }

  public String getType() {
    return type;
  }

  public KoskiOppijaQuery setType(String type) {
    this.type = type;
    return this;
  }

  public String getFormat() {
    return format;
  }

  public KoskiOppijaQuery setFormat(String format) {
    this.format = format;
    return this;
  }

  public String getDateLimit() {
    return dateLimit;
  }

  public KoskiOppijaQuery setDateLimit(String dateLimit) {
    this.dateLimit = dateLimit;
    return this;
  }

  public List<String> getOppijaOids() {
    return oppijaOids;
  }

  public KoskiOppijaQuery setOppijaOids(List<String> oppijaOids) {
    this.oppijaOids = oppijaOids;
    return this;
  }

  public String getKoulutusmuoto() {
    return koulutusmuoto;
  }

  public KoskiOppijaQuery setKoulutusmuoto(String koulutusmuoto) {
    this.koulutusmuoto = koulutusmuoto;
    return this;
  }

  public List<String> getSuoritustyypit() {
    return suoritustyypit;
  }

  public KoskiOppijaQuery setSuoritustyypit(List<String> suoritustyypit) {
    this.suoritustyypit = suoritustyypit;
    return this;
  }
}
