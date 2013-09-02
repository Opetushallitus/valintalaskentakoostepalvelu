package fi.vm.sade.valinta.kooste.exception;

/**
 * User: wuoti
 * Date: 29.8.2013
 * Time: 14.11
 */
public class ValintalaskentapalveluException extends RuntimeException {

    public ValintalaskentapalveluException() {
    }

    public ValintalaskentapalveluException(String message) {
        super(message);
    }

    public ValintalaskentapalveluException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValintalaskentapalveluException(Throwable cause) {
        super(cause);
    }
}
