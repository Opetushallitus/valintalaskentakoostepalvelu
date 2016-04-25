package fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice;

import com.fasterxml.jackson.core.JsonProcessingException;

import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class VastaanottoRecordDTOTest {
    private final VastaanottoRecordDTO dto = new VastaanottoRecordDTO();
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void acceptsNonNullValues() {
        dto.setHakemusOid("1.2.246.562.11.00004865241");
        dto.setHakukohdeOid("1.2.246.562.20.42208535494");
        dto.setHakuOid("1.2.246.562.29.75203638285");
        dto.setHenkiloOid("1.2.246.562.24.47613339338");
        dto.setIlmoittaja("1.2.246.562.24.47613339339");
        dto.setSelite("Tämä on hyvin merkittävä testimuutos");
        dto.setTila(ValintatuloksenTila.EHDOLLISESTI_VASTAANOTTANUT);
        dto.setValintatapajonoOid("1453450848255-2023325699999498073");
    }

    @Test
    public void mayNotHaveNullProperties() throws JsonProcessingException {
        exception.expect(IllegalArgumentException.class);
        dto.setHakemusOid(null);
    }
}
