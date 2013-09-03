package fi.vm.sade.valinta.kooste.viestintapalvelu;

import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Ei palauta PDF-tiedostoa vaan URI:n varsinaiseen resurssiin - koska
 *         AngularJS resurssin palauttaman datan konvertoiminen selaimen
 *         ladattavaksi tiedostoksi on ongelmallista (mutta ei mahdotonta - onko
 *         tarpeen?).
 */
@Controller
@Path("hyvaksymiskirjeBatch")
public class HyvaksymiskirjeBatchAktivointiResource {

    private static final Logger LOG = LoggerFactory.getLogger(HyvaksymiskirjeBatchAktivointiResource.class);

}
