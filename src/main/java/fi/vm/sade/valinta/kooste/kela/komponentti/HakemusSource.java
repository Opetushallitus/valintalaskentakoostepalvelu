package fi.vm.sade.valinta.kooste.kela.komponentti;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;

public interface HakemusSource {
    Hakemus getHakemusByOid(String oid);
}
