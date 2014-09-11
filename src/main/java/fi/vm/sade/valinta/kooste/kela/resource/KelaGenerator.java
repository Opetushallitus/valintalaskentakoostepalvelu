package fi.vm.sade.valinta.kooste.kela.resource;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.valinta.kooste.kela.dto.KelaHakuFiltteri;
import fi.vm.sade.valinta.kooste.kela.dto.KelaProsessi;
import fi.vm.sade.valinta.kooste.kela.route.KelaFtpRoute;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;
@Component
@Configurable
public class KelaGenerator {

	@Autowired
	private KoodiService koodiService;

	@Autowired
	private KelaRoute kelaRoute;

	@Autowired
	private KelaFtpRoute kelaFtpRoute;

	@Autowired
	private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;

	public ProsessiId aktivoiKelaTiedostonluonti(KelaHakuFiltteri hakuTietue) {
		// tietoe ei ole viela saatavilla
		if (hakuTietue == null || hakuTietue.getHakuOids() == null
				|| hakuTietue.getHakuOids().isEmpty()) {
			throw new RuntimeException(
					"Vähintään yksi hakuOid on annettava Kela-dokumentin luontia varten.");
		}
		String aineistonNimi = hakuTietue.getAineisto();// "Toisen asteen vastaanottotiedot";
		String organisaationNimi = "OPH";
		KelaProsessi kelaProsessi = new KelaProsessi("Kela-dokumentin luonti",
				hakuTietue.getHakuOids());
		kelaRoute.aloitaKelaLuonti(kelaProsessi, hakuTietue.getHakuOids(),
				aineistonNimi, organisaationNimi, SecurityContextHolder
						.getContext().getAuthentication());
		dokumenttiProsessiKomponentti.tuoUusiProsessi(kelaProsessi);
		return kelaProsessi.toProsessiId();
	}
	
	public String aktivoiKelaTiedostonluonti(String[] args) {
		KelaHakuFiltteri kelaHakuFiltteri = new KelaHakuFiltteri();
		kelaHakuFiltteri.setAineisto("");
		kelaHakuFiltteri.setHakuOids(Arrays.asList(args));
		aktivoiKelaTiedostonluonti(kelaHakuFiltteri);
		System.out.println("STARTED");
		return "STARTED";
	}
	public static void main(String[] args) {
				if( args.length==0) {
					System.err.println("No hakuOids given.");
					return;
				}
		        final ApplicationContext context = new ClassPathXmlApplicationContext("/spring/application-context.xml");
		        KelaGenerator kelaGenerator = context.getBean(KelaGenerator.class);
		        kelaGenerator.aktivoiKelaTiedostonluonti(args);
		        return;
    }
}
