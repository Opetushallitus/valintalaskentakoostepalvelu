package fi.vm.sade.valinta.kooste.external.resource;

import java.util.concurrent.Future;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class PeruutettavaImpl implements Peruutettava {

	private final Future<?> future;

	public PeruutettavaImpl(Future<?> future) {
		this.future = future;
	}

	public void peruuta() {
		future.cancel(true);
	}

	public boolean onTehty() {
		return future.isDone();
	}
}
