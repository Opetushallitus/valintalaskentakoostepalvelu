package fi.vm.sade.valinta.kooste.ohjausparametrit;

import com.google.gson.GsonBuilder;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.impl.OhjausparametritAsyncResourceImpl;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Jussi Jartamo
 */
public class OhjausparametritAsyncResourceTest {

    private static final Logger LOG = LoggerFactory.getLogger(OhjausparametritAsyncResourceTest.class);

    @Test
    public void testaaOhjausparametriJsoninSarjallistus() throws Exception {
        OhjausparametritAsyncResourceImpl oi = new OhjausparametritAsyncResourceImpl("");
        ParametritDTO parametrit = oi.gson().fromJson(IOUtils.toString(new ClassPathResource("ohjausparametrit/parametrit2.json").getInputStream()), ParametritDTO.class);
        LOG.info("{}", new GsonBuilder().setPrettyPrinting().create().toJson(parametrit));
    }
}
