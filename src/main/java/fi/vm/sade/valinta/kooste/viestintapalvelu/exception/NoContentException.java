package fi.vm.sade.valinta.kooste.viestintapalvelu.exception;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Viestipalvelu ei anna tyhjille kutsuille dokumentteja!
 */
public class NoContentException extends RuntimeException {
    private static final long serialVersionUID = -4370383078429183573L;

    public NoContentException(String reason) {
        super(reason);
    }

}
