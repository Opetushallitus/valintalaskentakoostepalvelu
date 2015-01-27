package fi.vm.sade.valinta.kooste;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;

import fi.vm.sade.integrationtest.tomcat.EmbeddedTomcat;
import fi.vm.sade.integrationtest.tomcat.SharedTomcat;
import fi.vm.sade.integrationtest.util.ProjectRootFinder;

public class ValintaKoosteTomcat extends EmbeddedTomcat {
    static final String VALINTAKOOSTE_MODULE_ROOT = ProjectRootFinder.findProjectRoot() + "/valintalaskentakoostepalvelu";
    static final String VALINTAKOOSTE_CONTEXT_PATH = "valintalaskentakoostepalvelu";

    public final static void main(String... args) throws ServletException, LifecycleException {
        new ValintaKoosteTomcat(Integer.parseInt(System.getProperty("valintakooste.port", "8094"))).start().await();
    }

    public ValintaKoosteTomcat(int port) {
        super(port, VALINTAKOOSTE_MODULE_ROOT, VALINTAKOOSTE_CONTEXT_PATH);
    }


    public static void startShared() {
        SharedTomcat.start(VALINTAKOOSTE_MODULE_ROOT, VALINTAKOOSTE_CONTEXT_PATH);
    }
}