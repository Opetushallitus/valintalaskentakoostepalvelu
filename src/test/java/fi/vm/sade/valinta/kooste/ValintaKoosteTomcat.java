package fi.vm.sade.valinta.kooste;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;

import fi.vm.sade.valinta.integrationtest.EmbeddedTomcat;
import fi.vm.sade.valinta.integrationtest.ProjectRootFinder;
import fi.vm.sade.valinta.integrationtest.SharedTomcat;

public class ValintaKoosteTomcat {
    static final String VALINTAKOOSTE_MODULE_ROOT = ProjectRootFinder.findProjectRoot() + "/valintalaskentakoostepalvelu";
    static final String VALINTAKOOSTE_CONTEXT_PATH = "valintalaskentakoostepalvelu";

    public final static void main(String... args) throws ServletException, LifecycleException {
        new EmbeddedTomcat(Integer.parseInt(System.getProperty("valintakooste.port", "8080")), VALINTAKOOSTE_MODULE_ROOT, VALINTAKOOSTE_CONTEXT_PATH).start().await();
    }

    public static void startShared() {
        SharedTomcat.start(VALINTAKOOSTE_MODULE_ROOT, VALINTAKOOSTE_CONTEXT_PATH);
    }
}