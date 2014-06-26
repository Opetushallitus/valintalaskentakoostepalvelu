package fi.vm.sade.valinta.kooste.converter;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.util.Converter;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConversionException;
import org.apache.camel.support.TypeConverterSupport;

public class HakemusToHakemusDTOConverter extends TypeConverterSupport {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T convertTo(Class<T> type, Exchange exchange, Object value) throws TypeConversionException {
        if (value instanceof Hakemus) {
            Hakemus h = (Hakemus) value;
            return (T) Converter.hakemusToHakemusDTO(h);
        }
        throw new TypeConversionException(value, HakemusTyyppi.class, new RuntimeException(
                "Can only convert fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus!"));
    }
}