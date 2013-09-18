package fi.vm.sade.valinta.kooste.viestintapalvelu.exception;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class NoReplyException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NoReplyException(String reason) {
        super(reason);
    }
}
