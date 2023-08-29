package fi.vm.sade.valinta.kooste.external.resource;

import java.net.URI;

public class ServiceTicket {
  public final String service;
  public final String serviceTicket;

  public ServiceTicket(String service, String serviceTicket) {
    this.service = service;
    this.serviceTicket = serviceTicket;
  }

  public URI getLoginUrl() {
    return URI.create(this.service + "?ticket=" + this.serviceTicket);
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o != null && this.getClass() == o.getClass()) {
      ServiceTicket that = (ServiceTicket) o;
      return !this.service.equals(that.service)
          ? false
          : this.serviceTicket.equals(that.serviceTicket);
    } else {
      return false;
    }
  }

  public int hashCode() {
    int result = this.service.hashCode();
    result = 31 * result + this.serviceTicket.hashCode();
    return result;
  }

  public String toString() {
    return "ServiceTicket{service='"
        + this.service
        + "', serviceTicket='"
        + this.serviceTicket
        + "'}";
  }
}
