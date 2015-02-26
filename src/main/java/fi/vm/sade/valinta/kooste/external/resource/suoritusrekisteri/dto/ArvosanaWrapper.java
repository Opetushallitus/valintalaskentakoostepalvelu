package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto;

import com.google.gson.Gson;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;

/**
 * @author Jussi Jartamo
 */
public class ArvosanaWrapper {
    private final static Logger LOG = LoggerFactory.getLogger(ArvosanaWrapper.class);
    private final Arvosana arvosana;
    public static final org.joda.time.format.DateTimeFormatter ARVOSANA_DTF = DateTimeFormat.forPattern("dd.MM.yyyy");

    public ArvosanaWrapper(Arvosana arvosana) {
        this.arvosana = arvosana;
    }

    public DateTime getMyonnettyAsDateTime() {
        if(arvosana.getMyonnetty() == null) {
            LOG.error("Arvosanalla ei ole myöntämispäivämäärää: {}", new Gson().toJson(arvosana));
            throw new RuntimeException("Arvosanalla ei ole myöntämispäivämäärää");
        }
        return ARVOSANA_DTF.parseDateTime(arvosana.getMyonnetty());
    }

    public boolean onkoMyonnettyEnnen(DateTime referenssiPvm) {
        if(referenssiPvm == null) {
            return true;
        }
        return referenssiPvm.isAfter(getMyonnettyAsDateTime());
    }

    public Arvosana getArvosana() {
        return arvosana;
    }
}
