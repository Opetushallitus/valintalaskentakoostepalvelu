package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.dto;

// {"letterTotalCount":3,"letterReadyCount":3,"letterErrorCount":0}
public class LetterBatchCountDto {
    public final long letterTotalCount;
    public final long letterReadyCount;
    public final long letterErrorCount;

    public LetterBatchCountDto(long letterTotalCount, long letterReadyCount, long letterErrorCount) {
        this.letterTotalCount = letterTotalCount;
        this.letterReadyCount = letterReadyCount;
        this.letterErrorCount = letterErrorCount;
    }

}
