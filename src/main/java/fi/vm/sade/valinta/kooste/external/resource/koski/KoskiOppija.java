package fi.vm.sade.valinta.kooste.external.resource.koski;

/**
 * TODO: Lisää opiskeluoikeudet, mielellään JSONina
 */
public class KoskiOppija {
    private KoskiHenkilö henkilö;

    public String getOppijanumero() {
        return henkilö.oid;
    }

    public static class KoskiHenkilö {
        public String oid;
    }
}
