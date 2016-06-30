package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.dto;

// {"letterTotalCount":3,"letterReadyCount":3,"letterErrorCount":0}
public class LetterBatchCountDto {
    public final long letterTotalCount;
    public final long letterReadyCount;
    public final long letterErrorCount;
    public final boolean readyForPublish;
    public final boolean readyForEPosti;

    public LetterBatchCountDto(long letterTotalCount, long letterReadyCount, long letterErrorCount, boolean readyForPublish, boolean readyForEPosti) {
        this.letterTotalCount = letterTotalCount;
        this.letterReadyCount = letterReadyCount;
        this.letterErrorCount = letterErrorCount;
        this.readyForPublish = readyForPublish;
        this.readyForEPosti = readyForEPosti;
    }

}
