package fi.vm.sade.valinta.kooste.valvomo.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 * @param <T>
 */
public class ProsessiJaStatus<T> {

    public static enum Status {
        STARTED, FINISHED, FAILED
    }

    private T prosessi;
    private String message;
    private Status status;

    public ProsessiJaStatus() {
        this.prosessi = null;
        this.message = null;
        this.status = null;
    }

    public ProsessiJaStatus(T prosessi, String message, Status status) {
        this.prosessi = prosessi;
        this.message = message;
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public T getProsessi() {
        return prosessi;
    }

    public String getMessage() {
        return message;
    }

}
