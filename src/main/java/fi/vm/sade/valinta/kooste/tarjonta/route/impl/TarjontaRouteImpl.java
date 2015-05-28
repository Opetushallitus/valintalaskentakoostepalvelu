package fi.vm.sade.valinta.kooste.tarjonta.route.impl;

import org.apache.camel.spring.SpringRouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.kela.komponentti.impl.LinjakoodiKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakuTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohdeNimiTarjonnaltaKomponentti;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class TarjontaRouteImpl extends SpringRouteBuilder {

	private static final Logger LOG = LoggerFactory
			.getLogger(TarjontaRouteImpl.class);

	private LinjakoodiKomponentti linjakoodiKomponentti;
	private HaeHakukohdeNimiTarjonnaltaKomponentti hakukohdeNimiTarjonnaltaKomponentti;
	private HaeHakuTarjonnaltaKomponentti hakuTarjonnaltaKomponentti;

	@Autowired
	public TarjontaRouteImpl(
			HaeHakuTarjonnaltaKomponentti hakuTarjonnaltaKomponentti,
			LinjakoodiKomponentti linjakoodiKomponentti,
			HaeHakukohdeNimiTarjonnaltaKomponentti hakukohdeNimiTarjonnaltaKomponentti) {
		this.hakuTarjonnaltaKomponentti = hakuTarjonnaltaKomponentti;
		this.linjakoodiKomponentti = linjakoodiKomponentti;
		this.hakukohdeNimiTarjonnaltaKomponentti = hakukohdeNimiTarjonnaltaKomponentti;
	}

	@Override
	public void configure() throws Exception {
		from("direct:tarjontaHakuReitti").bean(hakuTarjonnaltaKomponentti);
		from("direct:tarjontaNimiReitti").bean(hakukohdeNimiTarjonnaltaKomponentti);
		from("direct:linjakoodiReitti").bean(linjakoodiKomponentti);
	}
}
