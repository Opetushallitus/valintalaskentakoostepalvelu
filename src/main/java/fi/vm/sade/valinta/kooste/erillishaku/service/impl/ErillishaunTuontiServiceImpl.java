package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.service.ErillishaunTuontiService;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.KoekutsukirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.KoekutsukirjeetImpl;
/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ErillishaunTuontiServiceImpl implements ErillishaunTuontiService {
	
	private static final Logger LOG = LoggerFactory
			.getLogger(ErillishaunTuontiServiceImpl.class);
	private final KoekutsukirjeetKomponentti koekutsukirjeetKomponentti;
	private final ApplicationAsyncResource applicationAsyncResource;
	private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
	private final ValintaperusteetValintakoeAsyncResource valintakoeResource;
	private final ValintalaskentaValintakoeAsyncResource osallistumisetResource;

	@Autowired
	public ErillishaunTuontiServiceImpl(
			KoekutsukirjeetKomponentti koekutsukirjeetKomponentti,
			ApplicationAsyncResource applicationAsyncResource,
			ViestintapalveluAsyncResource viestintapalveluAsyncResource,
			ValintaperusteetValintakoeAsyncResource valintaperusteetValintakoeAsyncResource,
			ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource) {
		this.koekutsukirjeetKomponentti = koekutsukirjeetKomponentti;
		this.applicationAsyncResource = applicationAsyncResource;
		this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
		this.valintakoeResource = valintaperusteetValintakoeAsyncResource;
		this.osallistumisetResource = valintalaskentaValintakoeAsyncResource;
	}
	
	@Override
	public void tuo(ErillishakuDTO erillishaku, InputStream data) {
		
	}
}
