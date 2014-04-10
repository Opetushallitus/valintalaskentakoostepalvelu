package fi.vm.sade.valinta.kooste.util;

import java.util.concurrent.BlockingQueue;

import org.apache.camel.component.seda.BlockingQueueFactory;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class BlockingLifoQueueFactory<E> implements BlockingQueueFactory<E> {

	@Override
	public BlockingQueue<E> create() {
		return new BlockingLifoQueue<E>();
	}

	@Override
	public BlockingQueue<E> create(int capacity) {
		return new BlockingLifoQueue<E>();
	}

}
