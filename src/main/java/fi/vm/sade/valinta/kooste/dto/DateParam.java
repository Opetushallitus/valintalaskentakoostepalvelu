package fi.vm.sade.valinta.kooste.dto;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Päivämäärän syöttö koostepalvelulle!
 */
public class DateParam {

    private final Date date;

    public static void main(String[] args) {
        System.out.println(new DateParam("22.11.1983").getDate());
    }

    public DateParam(String dateStr) throws WebApplicationException {
        final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        try {

            date = dateFormat.parse(dateStr);
        } catch (Exception e) {
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST)
                    .entity("Virheellinen päivämäärä " + dateStr + "! Päivämäärän pitäisi olla muotoa pp.kk.vvvv!")
                    .build());
        }
    }

    public Date getDate() {
        return date;
    }
}