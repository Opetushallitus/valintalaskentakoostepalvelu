package fi.vm.sade.valinta.kooste.valvomo.service;


/**
 *         Camel only!
 */
public interface ValvomoAdminService<T> {
    final String PROPERTY_VALVOMO_PROSESSI = "property_valvomo_prosessi";
    final String PROPERTY_VALVOMO_PROSESSIKUVAUS = "property_prosessikuvaus";

    void fail(T prosessi, Exception exception, String message);

    void start(T prosessi);

    void finish(T prosessi);
}
