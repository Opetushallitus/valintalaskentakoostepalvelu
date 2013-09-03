package fi.vm.sade.valinta.kooste.rest.haku.dto;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * User: wuoti
 * Date: 3.9.2013
 * Time: 14.40
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HakemusList {
    private Integer totalCount;
    private List<SuppeaHakemus> results = new ArrayList<SuppeaHakemus>();

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public List<SuppeaHakemus> getResults() {
        return results;
    }

    public void setResults(List<SuppeaHakemus> results) {
        this.results = results;
    }
}
