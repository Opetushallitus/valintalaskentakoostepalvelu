package fi.vm.sade.valinta.kooste.external.resource;

import java.net.HttpCookie;

public class SessionToken {
  public final ServiceTicket serviceTicket;
  public final HttpCookie cookie;

  public SessionToken(ServiceTicket serviceTicket, HttpCookie cookie) {
    this.serviceTicket = serviceTicket;
    this.cookie = cookie;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o != null && this.getClass() == o.getClass()) {
      SessionToken session = (SessionToken) o;
      return !this.serviceTicket.equals(session.serviceTicket)
          ? false
          : this.cookie.equals(session.cookie);
    } else {
      return false;
    }
  }

  public int hashCode() {
    int result = this.serviceTicket.hashCode();
    result = 31 * result + this.cookie.hashCode();
    return result;
  }

  public String toString() {
    return "SessionToken{serviceTicket=" + this.serviceTicket + ", cookie=" + this.cookie + "}";
  }
}
