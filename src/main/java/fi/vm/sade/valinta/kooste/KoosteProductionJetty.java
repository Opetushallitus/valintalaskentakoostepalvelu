package fi.vm.sade.valinta.kooste;

import fi.vm.sade.jetty.OpintopolkuJetty;

public class KoosteProductionJetty extends OpintopolkuJetty{
    public static final String contextPath = "/valintalaskentakoostepalvelu";
    public static final KoosteProductionJetty JETTY = new KoosteProductionJetty();

    public static void main(String[] args) {
        JETTY.start(contextPath);
    }
}
