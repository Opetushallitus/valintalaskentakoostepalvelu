package fi.vm.sade.valinta.kooste.valvomo.service.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.Property;
import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.lang.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import fi.vm.sade.valinta.kooste.valvomo.dto.ExceptionStack;
import fi.vm.sade.valinta.kooste.valvomo.dto.Prosessi;
import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus;
import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus.Status;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoService;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Unit test all process registering routes.
 * @SuppressWarnings Apache Commons Collections is Java 1.4 compatible => no
 *                   generics.
 */
@SuppressWarnings("unchecked")
public class ValvomoServiceImpl<T> implements ValvomoService<T>,
		ValvomoAdminService<T> {

	private static final Logger LOG = LoggerFactory
			.getLogger(ValvomoServiceImpl.class);
	private static final int DEFAULT_CAPACITY = 10;
	private final Buffer processBuffer;
	private final static FastDateFormat FORMATTER = FastDateFormat
			.getInstance("dd.MM.yyyy HH:mm:ss");

	public ValvomoServiceImpl() {
		this(DEFAULT_CAPACITY);
	}

	public ValvomoServiceImpl(int capacity) {
		this.processBuffer = BufferUtils
				.synchronizedBuffer(new CircularFifoBuffer(capacity));
	}

	@Override
	public void fail(@Property(PROPERTY_VALVOMO_PROSESSI) T process,
			@Property(Exchange.EXCEPTION_CAUGHT) Exception exception,
			@Header("message") String message) {
		StringBuffer buffer = newBufferWithDate("VIRHE");
		if (exception == null) {
			LOG.info("Process failed {}", process);
			buffer.append(", ").append("<< null exception >>");
		} else {
			LOG.error("Process failed {} with exception {}", new Object[] {
					process, exception });
			buffer.append(", ").append(exception.getMessage());
		}
		if (process instanceof ExceptionStack) {
			StringBuffer s = new StringBuffer().append(message).append(": ")
					.append(exception.getClass()).append(": ")
					.append(exception.getMessage());
			if (!((ExceptionStack) process).addException(s.toString())) { // already
				// in
				// process buffer
				return; // was not first
			}
		}
		processBuffer.add(new ProsessiJaStatus<T>(process, buffer.toString(),
				Status.FAILED));
	}

	@Override
	public void finish(@Property(PROPERTY_VALVOMO_PROSESSI) T prosessi) {
		processBuffer.add(new ProsessiJaStatus<T>(prosessi, newBufferWithDate(
				"LOPETETTU").toString(), Status.FINISHED));
	}

	@Override
	public void start(@Property(PROPERTY_VALVOMO_PROSESSI) T prosessi) {
		processBuffer.add(new ProsessiJaStatus<T>(prosessi, newBufferWithDate(
				"ALOITETTU").toString(), Status.STARTED));
	}

	public ProsessiJaStatus<T> getAjossaOlevaProsessiJaStatus() {
		Iterator<ProsessiJaStatus<T>> uusimmat = getUusimmatProsessitJaStatukset()
				.iterator();
		// loytyyko prosesseja
		if (uusimmat.hasNext()) {
			ProsessiJaStatus<T> uusin = uusimmat.next();
			// onko uusin prosessi kaynnissa
			if (Status.STARTED.equals(uusin.getStatus())) {
				return uusin;
			}
		}
		return null;
	}

	@Override
	public Collection<T> getUusimmatProsessit() {
		return Collections2.transform(
				Lists.reverse(Lists.newArrayList(processBuffer)),
				new Function<ProsessiJaStatus<T>, T>() {
					public T apply(ProsessiJaStatus<T> input) {
						return input.getProsessi();
					}
				});
	}

	@Override
	public Collection<ProsessiJaStatus<T>> getUusimmatProsessitJaStatukset() {

		return Collections.unmodifiableCollection(Lists.reverse(Lists
				.newArrayList(processBuffer)));
	}

	@Override
	public ProsessiJaStatus<T> getProsessiJaStatus(String uuid) {
		for (Object t : processBuffer) {
			ProsessiJaStatus<T> p = (ProsessiJaStatus<T>) t;
			if (p.getProsessi() instanceof Prosessi) {
				Prosessi p0 = (Prosessi) p.getProsessi();
				if (uuid.equals(p0.getId())) {
					return p;
				}
			}

		}
		return null;
	}

	private static StringBuffer newBufferWithDate(String status) {
		return new StringBuffer().append(status).append(": ")
				.append(FORMATTER.format(new Date()));
	}
}
