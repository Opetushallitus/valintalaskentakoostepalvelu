package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto;

import java.util.Date;
import java.util.List;

public class Muutoshistoria {

    private List<Change> changes;
    private Date timestamp;

    public List<Change> getChanges() {
        return changes;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}
