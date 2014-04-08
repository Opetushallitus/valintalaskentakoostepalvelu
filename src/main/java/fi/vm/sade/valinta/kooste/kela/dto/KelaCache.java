package fi.vm.sade.valinta.kooste.kela.dto;

import static fi.vm.sade.valinta.kooste.util.TarjontaUriToKoodistoUtil.toSearchCriteria;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.koodisto.service.types.common.KoodiType;
import fi.vm.sade.tarjonta.service.resources.dto.HakuDTO;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.kela.komponentti.HakemusSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.PaivamaaraSource;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class KelaCache implements HakemusSource, PaivamaaraSource {

	private static final Logger LOG = LoggerFactory.getLogger(KelaCache.class);
	private final ConcurrentHashMap<String, HakuDTO> haut = new ConcurrentHashMap<String, HakuDTO>();
	private final ConcurrentHashMap<String, HakukohdeDTO> hakukohteet = new ConcurrentHashMap<String, HakukohdeDTO>();
	private final ConcurrentHashMap<String, Hakemus> hakemukset = new ConcurrentHashMap<String, Hakemus>();
	private final ConcurrentHashMap<String, String> hakutyyppiArvo = new ConcurrentHashMap<String, String>();
	private final CopyOnWriteArrayList<KelaAbstraktiHaku> kelaHaut = new CopyOnWriteArrayList<KelaAbstraktiHaku>();
	private final ConcurrentHashMap<String, Date> lukuvuosi = new ConcurrentHashMap<String, Date>();
	private final Date now;
	private final KoodiService koodiService;

	public KelaCache(KoodiService koodiService) {
		now = new Date();
		this.koodiService = koodiService;
	}

	@Override
	public Date lukuvuosi(HakuDTO hakuDTO) {
		String uri = hakuDTO.getKoulutuksenAlkamiskausiUri();
		if (!lukuvuosi.contains(uri)) {
			int vuosi = hakuDTO.getKoulutuksenAlkamisVuosi();
			int kuukausi = 1;
			// haku.get
			try {
				for (KoodiType koodi : koodiService
						.searchKoodis(toSearchCriteria(hakuDTO
								.getKoulutuksenAlkamiskausiUri()))) {
					if ("S".equals(StringUtils.upperCase(koodi.getKoodiArvo()))) {

						kuukausi = 8;
					} else if ("K".equals(StringUtils.upperCase(koodi
							.getKoodiArvo()))) {
						kuukausi = 1;
					} else {
						LOG.error(
								"Viallinen arvo {}, koodilla {} ",
								new Object[] { koodi.getKoodiArvo(),
										hakuDTO.getKoulutuksenAlkamiskausiUri() });
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				LOG.error("Ei voitu hakea lukuvuotta tarjonnalta syystä {}",
						e.getMessage());
				throw new RuntimeException(e);
			}
			lukuvuosi.put(uri, new DateTime(vuosi, kuukausi, 1, 1, 1).toDate());
		}
		return lukuvuosi.get(uri);
	}

	@Override
	public Date poimintapaivamaara(HakuDTO haku) {
		return now;
	}

	@Override
	public Date valintapaivamaara(HakuDTO haku) {
		return now;
	}

	@Override
	public Hakemus getHakemusByOid(String oid) {
		return hakemukset.get(oid);
	}

	public void put(HakuDTO haku) {
		haut.put(haku.getOid(), haku);
	}

	public void put(Hakemus hakemus) {
		hakemukset.put(hakemus.getOid(), hakemus);
	}

	public void put(HakukohdeDTO hakukohde) {
		hakukohteet.put(hakukohde.getOid(), hakukohde);
	}

	public void putHakutyyppi(String hakutyyppi, String arvo) {
		hakutyyppiArvo.put(hakutyyppi, arvo);
	}

	public String getHakutyyppi(String hakutyyppi) {
		return hakutyyppiArvo.get(hakutyyppi);
	}

	public void addKelaHaku(KelaAbstraktiHaku kelaHaku) {
		kelaHaut.add(kelaHaku);
	}

	public Collection<KelaAbstraktiHaku> getKelaHaut() {
		return kelaHaut;
	}
}
