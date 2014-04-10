package fi.vm.sade.valinta.kooste.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ForwardingBlockingQueue;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         LiFo blocking queue
 */
public class BlockingLifoQueue<E> extends ForwardingBlockingQueue<E> {

	private final LinkedBlockingDeque<E> delegate;

	public BlockingLifoQueue() {
		super();
		this.delegate = new LinkedBlockingDeque<E>();
	}

	public BlockingLifoQueue(int capacity) {
		super();
		this.delegate = new LinkedBlockingDeque<E>(capacity);
	}

	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		return delegate.pollLast(timeout, unit);
	}

	@Override
	public E take() throws InterruptedException {
		return delegate.takeLast();
	}

	@Override
	protected BlockingQueue<E> delegate() {
		return delegate;
	}
}
