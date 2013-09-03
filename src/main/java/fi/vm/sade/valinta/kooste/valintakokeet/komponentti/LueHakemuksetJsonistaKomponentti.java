package fi.vm.sade.valinta.kooste.valintakokeet.komponentti;

import com.google.gson.Gson;
import fi.vm.sade.valinta.kooste.rest.haku.ApplicationResource;
import org.apache.camel.language.Simple;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * User: wuoti
 * Date: 29.8.2013
 * Time: 13.50
 */
@Component("lueHakemuksetJsonistaKomponentti")
public class LueHakemuksetJsonistaKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(LueHakemuksetJsonistaKomponentti.class);

    @Autowired
    private ApplicationResource applicationResource;

    private class HakemusList {
        private Integer totalCount;
        private List<Hakemus> results = new ArrayList<Hakemus>();

        private Integer getTotalCount() {
            return totalCount;
        }

        private void setTotalCount(Integer totalCount) {
            this.totalCount = totalCount;
        }

        private List<Hakemus> getResults() {
            return results;
        }

        private void setResults(List<Hakemus> results) {
            this.results = results;
        }
    }

    private class Hakemus {
        private String oid;

        private String getOid() {
            return oid;
        }

        private void setOid(String oid) {
            this.oid = oid;
        }
    }

    public List<String> lueHakemuksetJsonista(@Simple("${property.hakuOid}") String hakuOid) {
        String applications = applicationResource.findApplications(null, null, null, null, hakuOid, 0,
                Integer.MAX_VALUE);
        HakemusList hakemusList = new Gson().fromJson(applications, HakemusList.class);
        List<String> hakemusOids = new ArrayList<String>();
        for (Hakemus hakemus : hakemusList.getResults()) {
            hakemusOids.add(hakemus.getOid());
        }

        return hakemusOids;
    }
}
