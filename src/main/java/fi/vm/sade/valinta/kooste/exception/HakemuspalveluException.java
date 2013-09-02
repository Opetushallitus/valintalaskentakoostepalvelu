package fi.vm.sade.valinta.kooste.exception;

/**
 * User: wuoti
 * Date: 29.8.2013
 * Time: 14.11
 */
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
