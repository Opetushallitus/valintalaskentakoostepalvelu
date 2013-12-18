package fi.vm.sade.valinta.kooste.valvomo.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ExceptionStack {

    /**
     * @return true if first
     */
    boolean addException(String e);
}
