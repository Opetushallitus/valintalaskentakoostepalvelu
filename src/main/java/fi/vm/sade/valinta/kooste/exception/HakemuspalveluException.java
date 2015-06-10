package fi.vm.sade.valinta.kooste.exception;

public class HakemuspalveluException extends RuntimeException {

    public HakemuspalveluException() {
    }

    public HakemuspalveluException(String message) {
        super(message);
    }

    public HakemuspalveluException(String message, Throwable cause) {
        super(message, cause);
    }

    public HakemuspalveluException(Throwable cause) {
        super(cause);
    }
}
