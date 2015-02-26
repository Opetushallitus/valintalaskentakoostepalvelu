package fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto;

import java.util.Date;

/**
 * @author Jussi Jartamo
 */
public class ParametriDTO {
    private Date date;
    private Date dateStart;
    private Date dateEnd;

    public void setDate(Date date) {
        this.date = date;
    }

    public void setDateEnd(Date dateEnd) {
        this.dateEnd = dateEnd;
    }

    public void setDateStart(Date dateStart) {
        this.dateStart = dateStart;
    }

    public Date getDate() {
        return date;
    }

    public Date getDateEnd() {
        return dateEnd;
    }

    public Date getDateStart() {
        return dateStart;
    }
}
