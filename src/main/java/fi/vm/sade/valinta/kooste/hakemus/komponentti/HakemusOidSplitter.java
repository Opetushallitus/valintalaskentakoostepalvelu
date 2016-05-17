package fi.vm.sade.valinta.kooste.hakemus.komponentti;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.SuppeaHakemus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component("hakemusOidSplitter")
public class HakemusOidSplitter {

    public List<String> splitHakemusOid(Collection<SuppeaHakemus> hakemukset) {
        List<String> oids = new ArrayList<String>();
        for (SuppeaHakemus h : hakemukset) {
            oids.add(h.getOid());
        }
        return oids;
    }
}
