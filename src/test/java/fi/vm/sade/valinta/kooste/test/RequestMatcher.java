package fi.vm.sade.valinta.kooste.test;

import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.Request;
import org.mockito.ArgumentMatcher;

public class RequestMatcher implements ArgumentMatcher<Request> {
  private final Request left;

  public RequestMatcher(final Request left) {
    this.left = left;
  }

  @Override
  public boolean matches(Request right) {
    if (right == null) {
      return false;
    }
    return StringUtils.equals(left.getMethod(), right.getMethod())
        && StringUtils.equals(left.getUrl(), right.getUrl());
  }
}
