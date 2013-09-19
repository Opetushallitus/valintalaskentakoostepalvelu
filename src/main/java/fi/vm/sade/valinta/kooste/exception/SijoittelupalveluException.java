package fi.vm.sade.valinta.kooste.exception;

public class SijoittelupalveluException extends RuntimeException {

    private static final long serialVersionUID = -1200947773912681208L;

    public SijoittelupalveluException(String reason) {
        super(reason);
    }

}
