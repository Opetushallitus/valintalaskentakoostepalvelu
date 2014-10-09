package fi.vm.sade.valinta.kooste.viestintapalvelu;

//import org.jdeferred.Deferred;
//import org.jdeferred.DeferredManager;
//import org.jdeferred.impl.DefaultDeferredManager;
//import org.jdeferred.impl.DeferredObject;
import static rx.Observable.from;
import static rx.Observable.zip;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class KoekutsukirjeetObserverTest {

	private static final Logger LOG = LoggerFactory
			.getLogger(KoekutsukirjeetObserverTest.class);

	@Test
	public void t() {
		zip(from(createFuture("o1")), from(createFuture("o2")),
				new Func2<String, String, String>() {
					@Override
					public String call(String t1, String t2) {
						return t1 + t2;
					}
				}).subscribeOn(Schedulers.newThread()).subscribe(
				new Action1<String>() {
					@Override
					public void call(String t1) {
						System.err.println("GFDSA");
					}
				});
	}

	@Ignore
	@Test
	public void testaaDeferredReitti() {
		{

			PublishSubject<Void> stop = PublishSubject.create();
			Observable.interval(1, TimeUnit.SECONDS).take(5).takeUntil(stop)
					.subscribe(new Action1<Long>() {
						int i = 0;

						public void call(Long t1) {
							System.err.println(t1);
							++i;
							if (i > 3) {
								stop.onNext(null);
							}
						}
					}, new Action1<Throwable>() {
						public void call(Throwable t1) {

						}
					}, new Action0() {
						public void call() {
							System.err.println("COMP");
						}
					});

		}

	}

	private Future<String> createFuture(String msg) {
		return new Future<String>() {
			volatile boolean f = false;

			public String get() throws InterruptedException, ExecutionException {
				Thread.sleep(100);
				f = true;
				return msg;
			}

			public boolean cancel(boolean mayInterruptIfRunning) {
				return false;
			}

			public String get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException,
					TimeoutException {
				return null;
			}

			public boolean isCancelled() {
				return false;
			}

			public boolean isDone() {
				return f;
			}
		};
	}

	private Future<Integer> createFuture(Integer msg) {
		return new Future<Integer>() {
			volatile boolean f = false;

			public Integer get() throws InterruptedException,
					ExecutionException {
				Thread.sleep(100);
				f = true;
				return msg;
			}

			public boolean cancel(boolean mayInterruptIfRunning) {
				return false;
			}

			public Integer get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException,
					TimeoutException {
				return null;
			}

			public boolean isCancelled() {
				return false;
			}

			public boolean isDone() {
				return f;
			}
		};
	}
}
