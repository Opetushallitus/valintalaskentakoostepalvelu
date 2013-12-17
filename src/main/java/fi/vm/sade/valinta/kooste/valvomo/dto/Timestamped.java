package fi.vm.sade.valinta.kooste.valvomo.dto;

import java.util.Date;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Mahdollistaa Valvomon automaattisen kestolaskurin toiminnan
 */
public interface Timestamped {

    Date getCreatedAt();
}
