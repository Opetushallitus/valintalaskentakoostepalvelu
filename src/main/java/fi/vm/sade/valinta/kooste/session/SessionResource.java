package fi.vm.sade.valinta.kooste.session;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("SessionResource")
@RequestMapping("/resources/session")
@Api(value = "/session", description = "Sessionhallinta")
public class SessionResource {

  @GetMapping(value = "/maxinactiveinterval", produces = MediaType.TEXT_PLAIN_VALUE)
  @PreAuthorize("isAuthenticated()")
  @ApiOperation(
      value = "Palauttaa session erääntymisen aikarajan sekunteina",
      notes = "Tarvitsee HTTP kutsun, jossa on session id",
      response = String.class)
  public String maxInactiveInterval(HttpServletRequest req) {
    return Integer.toString(req.getSession().getMaxInactiveInterval());
  }
}
