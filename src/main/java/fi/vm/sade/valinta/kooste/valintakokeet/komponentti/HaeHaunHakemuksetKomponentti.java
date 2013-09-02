package fi.vm.sade.valinta.kooste.valintakokeet.komponentti;

import com.google.gson.Gson;
import fi.vm.sade.valinta.kooste.exception.HakemuspalveluException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * User: wuoti
 * Date: 29.8.2013
 * Time: 13.50
 */
@Component("haeHaunHakemuksetKomponentti")
public class HaeHaunHakemuksetKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HaeHaunHakemuksetKomponentti.class);

    @Value("${valintalaskentakoostepalvelu.hakemus.all.rest.url}")
    private String hakemusUrl;

    private final String HAKU_OID_QUERY_PARAMETER = "asId";
    private final String HAKEMUS_TILA_QUERY_PARAMETER = "appState";
    private final String HAKEMUS_AKTIIVINEN_TILA = "ACTIVE";
    private final String HAKEMUS_PUUTTEELLINEN_TILA = "INCOMPLETE";

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

    public List<String> haeHaunHakemukset(String hakuOid) {

        Reader reader = null;
        try {
            GetMethod getMethod = new GetMethod(hakemusUrl);
            getMethod.setQueryString(new NameValuePair[]{
                    new NameValuePair(HAKU_OID_QUERY_PARAMETER, hakuOid),
                    new NameValuePair(HAKEMUS_TILA_QUERY_PARAMETER, HAKEMUS_AKTIIVINEN_TILA),
                    new NameValuePair(HAKEMUS_TILA_QUERY_PARAMETER, HAKEMUS_PUUTTEELLINEN_TILA)
            });

            HttpClient httpClient = new HttpClient();
            httpClient.executeMethod(getMethod);
            reader = new BufferedReader(new InputStreamReader(getMethod.getResponseBodyAsStream()));
            HakemusList hakemusList = new Gson().fromJson(reader, HakemusList.class);

            List<String> hakemusOids = new ArrayList<String>();
            for (Hakemus hakemus : hakemusList.getResults()) {
                hakemusOids.add(hakemus.getOid());
            }

            return hakemusOids;
        } catch (Exception e) {
            LOG.error("Virhe haun hakemusten hakemisessa", e);
            throw new HakemuspalveluException(e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                LOG.error("Virhe haun hakemusten hakemisessa", e);
                throw new HakemuspalveluException(e);
            }
        }
    }

}
