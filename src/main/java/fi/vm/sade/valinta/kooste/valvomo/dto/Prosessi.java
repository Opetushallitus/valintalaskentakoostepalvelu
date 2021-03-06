package fi.vm.sade.valinta.kooste.valvomo.dto;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Abstrakti perustyyppi prosesseille. Poimii muun muassa prosessin luojan tiedot metatiedoiksi.
 * Valvomolle voi antaa minka tahansa olion prosessin kuvaukseksi joten kaytto on valinnaista.
 */
public class Prosessi implements Comparable<Prosessi>, Timestamped, ExceptionStack {
  private String id;
  private String resurssi;
  private String toiminto;
  private String createdBy;
  private Date createdAt;
  private String hakuOid;
  private AtomicBoolean wasFirst = new AtomicBoolean(true);
  private Collection<String> exceptions =
      Collections.synchronizedCollection(Lists.<String>newArrayList());

  public Prosessi(String resurssi, String toiminto, String hakuOid) {
    this.id = UUID.randomUUID().toString();
    this.createdBy = getAuthenticatedUserName();
    this.createdAt = new Date();
    this.resurssi = resurssi;
    this.toiminto = toiminto;
    this.hakuOid = hakuOid;
  }

  public Prosessi() {
    this("<< tuntematon resurssi >>", "<< tuntematon toiminto >>", "<< tuntematon haku >>");
  }

  @Override
  public boolean addException(String e) {
    exceptions.add(e);
    return wasFirst.compareAndSet(true, false);
  }

  public int compareTo(Prosessi o) {
    int times = createdAt.compareTo(o.createdAt);
    if (times == 0) {
      return id.compareTo(o.id);
    }
    return times;
  }

  public String getId() {
    return id;
  }

  public String getToiminto() {
    return toiminto;
  }

  public String getResurssi() {
    return resurssi;
  }

  public Collection<String> getExceptions() {
    return exceptions;
  }

  @Override
  public Date getCreatedAt() {
    return createdAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public String getHakuOid() {
    return hakuOid;
  }

  private static String getAuthenticatedUserName() {
    try {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null) {
        return auth.getName();
      } else {
        return "<< user not authenticated >>";
      }
    } catch (Exception e) {
      return "<< stacktrace :: " + e.getMessage() + " >>";
    }
  }

  @Override
  public String toString() {
    return Arrays.toString(new Object[] {getResurssi(), getToiminto(), getCreatedAt()});
  }
}
