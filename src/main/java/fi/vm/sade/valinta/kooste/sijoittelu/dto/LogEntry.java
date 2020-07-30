package fi.vm.sade.valinta.kooste.sijoittelu.dto;

import java.util.Date;
import org.bson.types.ObjectId;

public class LogEntry {
  private ObjectId id;
  private Date luotu;
  private String muokkaaja;
  private String muutos;
  private String selite;

  public ObjectId getId() {
    return id;
  }

  public void setId(ObjectId id) {
    this.id = id;
  }

  public Date getLuotu() {
    return luotu;
  }

  public void setLuotu(Date luotu) {
    this.luotu = luotu;
  }

  public String getMuokkaaja() {
    return muokkaaja;
  }

  public void setMuokkaaja(String muokkaaja) {
    this.muokkaaja = muokkaaja;
  }

  public String getMuutos() {
    return muutos;
  }

  public void setMuutos(String muutos) {
    this.muutos = muutos;
  }

  public String getSelite() {
    return selite;
  }

  public void setSelite(String selite) {
    this.selite = selite;
  }
}
