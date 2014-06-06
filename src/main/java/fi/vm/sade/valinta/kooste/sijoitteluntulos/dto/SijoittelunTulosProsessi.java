package fi.vm.sade.valinta.kooste.sijoitteluntulos.dto;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;

public class SijoittelunTulosProsessi extends DokumenttiProsessi {

	private final AtomicInteger valmis = new AtomicInteger(-1);
	private final Collection<String> tulosIds = new ConcurrentSkipListSet<String>();
	private final Collection<String> ohitetutOids = new ConcurrentSkipListSet<String>();
	private final Collection<String> ohitetutTarjoajaOids = new ConcurrentSkipListSet<String>();
	private final Collection<String> onnistuneetOids = new ConcurrentSkipListSet<String>();
	private final Collection<String> onnistuneetTarjoajaOids = new ConcurrentSkipListSet<String>();

	public SijoittelunTulosProsessi(String resurssi, String toiminto,
			String hakuOid, List<String> tags) {
		super(resurssi, toiminto, hakuOid, tags);
	}

	public int inkrementoi() {
		inkrementoiKokonaistyota();
		return valmis.decrementAndGet();
	}

	@Override
	public void setKokonaistyo(int arvo) {
		valmis.set(arvo);
		super.setKokonaistyo(arvo);
	}

	public Collection<String> getTulosIds() {
		return tulosIds;
	}

	public Collection<String> getOhitetutOids() {
		return ohitetutOids;
	}

	public Collection<String> getOhitetutTarjoajaOids() {
		return ohitetutTarjoajaOids;
	}

	public Collection<String> getOnnistuneetOids() {
		return onnistuneetOids;
	}

	public Collection<String> getOnnistuneetTarjoajaOids() {
		return onnistuneetTarjoajaOids;
	}
}
