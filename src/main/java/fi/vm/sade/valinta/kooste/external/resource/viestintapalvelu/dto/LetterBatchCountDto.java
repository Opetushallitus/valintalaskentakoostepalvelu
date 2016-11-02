package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.dto;

// {"letterTotalCount":3,"letterReadyCount":3,"letterErrorCount":0}
public class LetterBatchCountDto {
    public final Long letterBatchId;
    public final long letterTotalCount;
    public final long letterReadyCount;
    public final long letterErrorCount;
    public final boolean readyForPublish;
    public final boolean readyForEPosti;

    public LetterBatchCountDto(Long letterBatchId, long letterTotalCount, long letterReadyCount, long letterErrorCount, boolean readyForPublish, boolean readyForEPosti) {
        this.letterBatchId = letterBatchId;
        this.letterTotalCount = letterTotalCount;
        this.letterReadyCount = letterReadyCount;
        this.letterErrorCount = letterErrorCount;
        this.readyForPublish = readyForPublish;
        this.readyForEPosti = readyForEPosti;
    }

}
