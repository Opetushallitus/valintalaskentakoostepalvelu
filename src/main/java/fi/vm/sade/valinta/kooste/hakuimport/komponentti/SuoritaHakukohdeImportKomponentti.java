package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import org.apache.camel.Body;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import fi.vm.sade.service.valintaperusteet.dto.AvainArvoDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeImportDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdekoodiDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohteenValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.MonikielinenTekstiDTO;
import fi.vm.sade.tarjonta.service.resources.dto.ValintakoeRDTO;
import fi.vm.sade.tarjonta.service.resources.v1.HakukohdeV1ResourceWrapper;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeValintaperusteetV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;

/**
 * User: wuoti Date: 20.5.2013 Time: 10.46
 */
// @Component("suoritaHakukohdeImportKomponentti")
@PreAuthorize("isAuthenticated()")
public class SuoritaHakukohdeImportKomponentti {

	private static final Logger LOG = LoggerFactory
			.getLogger(SuoritaHakukohdeImportKomponentti.class);

	private final HakukohdeV1ResourceWrapper hakukohdeResource;

	@Autowired
	public SuoritaHakukohdeImportKomponentti(
			HakukohdeV1ResourceWrapper hakukohdeResource) {
		this.hakukohdeResource = hakukohdeResource;
	}

	public HakukohdeImportDTO suoritaHakukohdeImport(@Body// @Property(OPH.HAKUKOHDEOID)
			String hakukohdeOid) {
		try {
			ResultV1RDTO<HakukohdeValintaperusteetV1RDTO> result = hakukohdeResource
					.findValintaperusteetByOid(hakukohdeOid);

			HakukohdeValintaperusteetV1RDTO data = result.getResult();
			HakukohdeImportDTO importTyyppi = new HakukohdeImportDTO();

			importTyyppi.setTarjoajaOid(data.getTarjoajaOid());

			if (data.getTarjoajaNimi() != null) {
				for (String s : data.getTarjoajaNimi().keySet()) {
					MonikielinenTekstiDTO m = new MonikielinenTekstiDTO();
					m.setLang(s);
					m.setText(data.getTarjoajaNimi().get(s));
					importTyyppi.getTarjoajaNimi().add(m);
				}
			}

			if (data.getHakukohdeNimi() != null) {
				for (String s : data.getHakukohdeNimi().keySet()) {
					MonikielinenTekstiDTO m = new MonikielinenTekstiDTO();
					m.setLang(s);
					m.setText(data.getHakukohdeNimi().get(s));
					importTyyppi.getHakukohdeNimi().add(m);
				}
			}

			if (data.getHakuKausi() != null) {
				for (String s : data.getHakuKausi().keySet()) {
					MonikielinenTekstiDTO m = new MonikielinenTekstiDTO();
					m.setLang(s);
					m.setText(data.getHakuKausi().get(s));
					importTyyppi.getHakuKausi().add(m);
				}
			}

			importTyyppi.setHakuVuosi(new Integer(data.getHakuVuosi())
					.toString());

			HakukohdekoodiDTO hkt = new HakukohdekoodiDTO();
			if (data.getHakukohdeNimiUri() != null) {
				hkt.setKoodiUri(data.getHakukohdeNimiUri());
			} else {
				if (data.getOid() == null) {
					LOG.error("Hakukohteella ei ole Oidia!");
					throw new RuntimeException("Hakukohteella ei ole Oidia!");
				}
				hkt.setKoodiUri("hakukohteet_" + data.getOid().replace(".", ""));
			}
			importTyyppi.setHakukohdekoodi(hkt);

			importTyyppi.setHakukohdeOid(data.getOid());
			importTyyppi.setHakuOid(data.getHakuOid());
			importTyyppi.setValinnanAloituspaikat(data
					.getValintojenAloituspaikatLkm());
			importTyyppi.setTila(data.getTila());
			if (data.getValintakokeet() != null) {
				LOG.debug("Valintakokeita löytyi {}!", data.getValintakokeet()
						.size());
				for (ValintakoeRDTO valinakoe : data.getValintakokeet()) {
					HakukohteenValintakoeDTO v = new HakukohteenValintakoeDTO();
					v.setOid(valinakoe.getOid());
					v.setTyyppiUri(valinakoe.getTyyppiUri());
					importTyyppi.getValintakoe().add(v);
				}
			}

			String hakukohdeKoodiTunniste = data.getOid()
					.replaceAll("\\.", "_");

			AvainArvoDTO avainArvo = new AvainArvoDTO();

			avainArvo.setAvain("hakukohde_oid");
			avainArvo.setArvo(data.getOid());
			importTyyppi.getValintaperuste().add(avainArvo);

			avainArvo = new AvainArvoDTO();
			avainArvo.setAvain("paasykoe_min");
			avainArvo.setArvo(data.getPaasykoeMin().toString());
			importTyyppi.getValintaperuste().add(avainArvo);

			avainArvo = new AvainArvoDTO();
			avainArvo.setAvain("paasykoe_max");
			avainArvo.setArvo(data.getPaasykoeMax().toString());
			importTyyppi.getValintaperuste().add(avainArvo);

			avainArvo = new AvainArvoDTO();
			avainArvo.setAvain("paasykoe_hylkays_min");
			avainArvo.setArvo(data.getPaasykoeHylkaysMin().toString());
			importTyyppi.getValintaperuste().add(avainArvo);

			avainArvo = new AvainArvoDTO();
			avainArvo.setAvain("paasykoe_hylkays_max");
			avainArvo.setArvo(data.getPaasykoeHylkaysMax().toString());
			importTyyppi.getValintaperuste().add(avainArvo);

			avainArvo = new AvainArvoDTO();
			avainArvo.setAvain("lisanaytto_min");
			avainArvo.setArvo(data.getLisanayttoMin().toString());
			importTyyppi.getValintaperuste().add(avainArvo);

			avainArvo = new AvainArvoDTO();
			avainArvo.setAvain("lisanaytto_max");
			avainArvo.setArvo(data.getLisanayttoMax().toString());
			importTyyppi.getValintaperuste().add(avainArvo);

			avainArvo = new AvainArvoDTO();
			avainArvo.setAvain("lisanaytto_hylkays_min");
			avainArvo.setArvo(data.getLisanayttoHylkaysMin().toString());
			importTyyppi.getValintaperuste().add(avainArvo);

			avainArvo = new AvainArvoDTO();
			avainArvo.setAvain("lisanaytto_hylkays_max");
			avainArvo.setArvo(data.getLisanayttoHylkaysMax().toString());
			importTyyppi.getValintaperuste().add(avainArvo);

			avainArvo = new AvainArvoDTO();
			avainArvo.setAvain("paasykoe_ja_lisanaytto_hylkays_min");
			avainArvo.setArvo(data.getHylkaysMin().toString());
			importTyyppi.getValintaperuste().add(avainArvo);

			avainArvo = new AvainArvoDTO();
			avainArvo.setAvain("paasykoe_ja_lisanaytto_hylkays_max");
			avainArvo.setArvo(data.getHylkaysMax().toString());
			importTyyppi.getValintaperuste().add(avainArvo);

			avainArvo = new AvainArvoDTO();
			avainArvo.setAvain("painotettu_keskiarvo_hylkays_min");
			avainArvo.setArvo(data.getPainotettuKeskiarvoHylkaysMin()
					.toString());
			importTyyppi.getValintaperuste().add(avainArvo);

			avainArvo = new AvainArvoDTO();
			avainArvo.setAvain("painotettu_keskiarvo_hylkays_max");
			avainArvo.setArvo(data.getPainotettuKeskiarvoHylkaysMax()
					.toString());
			importTyyppi.getValintaperuste().add(avainArvo);

			avainArvo = new AvainArvoDTO();
			avainArvo.setAvain("paasykoe_tunniste");
			avainArvo.setArvo(data.getPaasykoeTunniste() != null ? data
					.getPaasykoeTunniste() : hakukohdeKoodiTunniste
					+ "_paasykoe");
			importTyyppi.getValintaperuste().add(avainArvo);

			avainArvo = new AvainArvoDTO();
			avainArvo.setAvain("lisanaytto_tunniste");
			avainArvo.setArvo(data.getLisanayttoTunniste() != null ? data
					.getLisanayttoTunniste() : hakukohdeKoodiTunniste
					+ "_lisanaytto");
			importTyyppi.getValintaperuste().add(avainArvo);

			avainArvo = new AvainArvoDTO();
			avainArvo.setAvain("lisapiste_tunniste");
			avainArvo.setArvo(data.getLisapisteTunniste() != null ? data
					.getLisapisteTunniste() : hakukohdeKoodiTunniste
					+ "_lisapiste");
			importTyyppi.getValintaperuste().add(avainArvo);

			avainArvo = new AvainArvoDTO();
			avainArvo.setAvain("urheilija_lisapiste_tunniste");
			avainArvo
					.setArvo(data.getUrheilijaLisapisteTunniste() != null ? data
							.getUrheilijaLisapisteTunniste()
							: hakukohdeKoodiTunniste + "_urheilija_lisapiste");
			importTyyppi.getValintaperuste().add(avainArvo);

			String opetuskieli = null;
			if (data.getOpetuskielet().size() > 0) {
				avainArvo = new AvainArvoDTO();
				opetuskieli = data.getOpetuskielet().get(0);
				avainArvo.setAvain("opetuskieli");
				avainArvo.setArvo(opetuskieli);
				importTyyppi.getValintaperuste().add(avainArvo);
			}

			// Kielikoetunnisteen selvittäminen
			String kielikoetunniste = null;
			if (StringUtils.isNotBlank(data.getKielikoeTunniste())) {
				kielikoetunniste = data.getKielikoeTunniste();
			} else if (StringUtils.isNotBlank(opetuskieli)) {
				kielikoetunniste = "kielikoe_" + opetuskieli;
			} else {
				kielikoetunniste = hakukohdeKoodiTunniste + "_kielikoe";
			}

			avainArvo = new AvainArvoDTO();
			avainArvo.setAvain("kielikoe_tunniste");
			avainArvo.setArvo(kielikoetunniste);
			importTyyppi.getValintaperuste().add(avainArvo);

			for (String avain : data.getPainokertoimet().keySet()) {
				avainArvo = new AvainArvoDTO();
				avainArvo.setAvain(avain);
				avainArvo.setArvo(data.getPainokertoimet().get(avain));
				importTyyppi.getValintaperuste().add(avainArvo);
			}

			return importTyyppi;
		} catch (Exception e) {
			LOG.error(
					"\r\n###\r\n### Importointi hakukohteelle {} epaonnistui! Virhe {}\r\n###",
					hakukohdeOid, e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

}
