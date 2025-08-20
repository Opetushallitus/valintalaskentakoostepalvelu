package fi.vm.sade.valinta.kooste.external.resource.koski;

import java.util.ArrayList;
import java.util.List;

public class KoskiOppijaQueryResult {
  private String status;
  private String resultsUrl;
  private List<String> files = new ArrayList<>();

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getResultsUrl() {
    return resultsUrl;
  }

  public void setResultsUrl(String resultsUrl) {
    this.resultsUrl = resultsUrl;
  }

  public List<String> getFiles() {
    return files;
  }

  public void setFiles(List<String> files) {
    this.files = files;
  }

  public boolean isCompleted() {
    return "complete".equals(this.status);
  }

  public boolean isFailed() {
    return "failed".equals(this.status);
  }
}
