package fi.vm.sade.valinta.kooste.valvomo.service;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Camel only!
 */
public interface ValvomoAdminService<T> {

    final String PROPERTY_VALVOMO_PROSESSI = "property_valvomo_prosessi";
    final String PROPERTY_VALVOMO_PROSESSIKUVAUS = "property_prosessikuvaus";

    /**
     * @param prosessi
     * @param exception
     *            null if none
     */
    void fail(T prosessi, Exception exception);

    /**
     * @param prosessi
     */
    void start(T prosessi);

    /**
     * @param prosessi
     */
    void finish(T prosessi);
}
