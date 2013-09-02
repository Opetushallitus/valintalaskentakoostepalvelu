package fi.vm.sade.valinta.kooste.exception;

/**
 * User: wuoti
 * Date: 29.8.2013
 * Time: 14.11
 */
public class ValintaperustepalveluException extends RuntimeException {

    public ValintaperustepalveluException() {
    }

    public ValintaperustepalveluException(String message) {
        super(message);
    }

    public ValintaperustepalveluException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValintaperustepalveluException(Throwable cause) {
        super(cause);
    }
}
