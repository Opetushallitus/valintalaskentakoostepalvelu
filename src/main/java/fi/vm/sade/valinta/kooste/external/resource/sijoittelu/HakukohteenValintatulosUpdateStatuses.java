package fi.vm.sade.valinta.kooste.external.resource.sijoittelu;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HakukohteenValintatulosUpdateStatuses {
    public String message;
    public List<ValintatulosUpdateStatus> statuses;

    public HakukohteenValintatulosUpdateStatuses() {
    }

    public HakukohteenValintatulosUpdateStatuses(List<ValintatulosUpdateStatus> statuses) {
        this.message = null;
        this.statuses = statuses;
    }

    public HakukohteenValintatulosUpdateStatuses(String message, List<ValintatulosUpdateStatus> statuses) {
        this.message = message;
        this.statuses = statuses;
    }

    @Override
    public String toString() {
        String resp = "message:" + message + " statuses:";
        for(ValintatulosUpdateStatus s : statuses) {
            resp += s.toString() + " ";
        }
        return resp;
    }

}
