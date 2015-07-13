package fi.vm.sade.valinta.kooste.viestintapalvelu;

import com.google.common.collect.Maps;
import com.google.common.collect.TreeMultiset;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.Organisaatio;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.LueHakijapalvelunOsoite;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsoiteHaku {

    private static final Logger LOG = LoggerFactory.getLogger(OsoiteHaku.class);

    private static Organisaatio responseToOrganisaatio(
            HaeOsoiteKomponentti haeOsoiteKomponentti,
            OrganisaatioAsyncResource organisaatioAsyncResource,
            Response organisaatioResponse) throws IOException {
        InputStream stream = (InputStream) organisaatioResponse.getEntity();
        String json = StringUtils.trimToEmpty(IOUtils.toString(stream));
        IOUtils.closeQuietly(stream);
        return new Gson().fromJson(json, Organisaatio.class);
    }

    private static Osoite haeOsoiteHierarkisesti(
            HaeOsoiteKomponentti haeOsoiteKomponentti,
            OrganisaatioAsyncResource organisaatioAsyncResource,
            String kieli, List<String> oids, Organisaatio rdto, Teksti organisaationimi) {
        Osoite hakijapalveluidenOsoite;
        try {
            if (organisaationimi.isArvoton()) {
                organisaationimi = new Teksti(rdto.getNimi());
            }
            hakijapalveluidenOsoite = LueHakijapalvelunOsoite.lueHakijapalvelunOsoite(haeOsoiteKomponentti, kieli, rdto, organisaationimi);
            if (rdto == null) {
                LOG.error("Organisaatiopalvelusta ei saatu organisaatiota tunnisteelle {}. Eli ei saatu hakijapalveluiden osoitetta.", Arrays.toString(oids.toArray()));
                return null;
            }
            if (oids == null) {
                LOG.error("Oidi-listaa ei voitu kerätä kun listaa ei ollut annettu!");
                return null;
            }
            try {
                oids.add(rdto.getParentOid());
            } catch (Exception e) {
                LOG.error("Oidia ei voitu lisätä oidilistaan oid=" + rdto.getParentOid(), e);
                throw new RuntimeException("Oidia ei voitu lisätä oidilistaan", e);
            }
            if (hakijapalveluidenOsoite != null) {
                LOG.error("Hakijapalveluiden osoite saatiin tarjoajalta {}.\r\n{}", Arrays.toString(oids.toArray()), new GsonBuilder().setPrettyPrinting().create().toJson(hakijapalveluidenOsoite));
                return hakijapalveluidenOsoite;
            }
            if (rdto.getParentOid() != null) {
                LOG.error("Ei saatu hakijapalveluiden osoitetta talta organisaatiolta. Tutkitaan seuraava {}", Arrays.toString(oids.toArray()));
                return haeOsoiteHierarkisesti(haeOsoiteKomponentti, organisaatioAsyncResource, kieli, oids, responseToOrganisaatio(haeOsoiteKomponentti, organisaatioAsyncResource, organisaatioAsyncResource
                        .haeOrganisaatio(rdto.getParentOid()).get()), organisaationimi);
            } else {
                LOG.error("Ei saatu hakijapalveluiden osoitetta! Kaytiin lapi organisaatiot {}!", Arrays.toString(oids.toArray()));
                return null;
            }
        } catch (Exception e) {
            LOG.error("Hakijapalveluiden osoitteen haussa odottamaton virhe", e);
        }
        return null;
    }

    public static Osoite organisaatioResponseToHakijapalveluidenOsoite(
            HaeOsoiteKomponentti haeOsoiteKomponentti,
            OrganisaatioAsyncResource organisaatioAsyncResource,
            List<String> oids, String kieli, Response organisaatioResponse) {
        Organisaatio org;
        Teksti organisaationimi;
        try {
            org = responseToOrganisaatio(haeOsoiteKomponentti, organisaatioAsyncResource, organisaatioResponse);
            organisaationimi = new Teksti(org.getNimi());
        } catch (Exception e) {
            LOG.error("Ei saatu organisaatiota!", e);
            throw new RuntimeException(e);
        }
        return haeOsoiteHierarkisesti(haeOsoiteKomponentti, organisaatioAsyncResource, kieli, oids, org, organisaationimi);
    }
}
