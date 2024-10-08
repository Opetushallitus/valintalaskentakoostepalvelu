package fi.vm.sade.valinta.kooste.valvomo.service.impl;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import fi.vm.sade.valinta.kooste.valvomo.dto.ExceptionStack;
import fi.vm.sade.valinta.kooste.valvomo.dto.Prosessi;
import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus;
import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus.Status;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoService;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.lang.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValvomoServiceImpl<T> implements ValvomoService<T>, ValvomoAdminService<T> {
  private static final Logger LOG = LoggerFactory.getLogger(ValvomoServiceImpl.class);
  private static final int DEFAULT_CAPACITY = 10;
  private final Buffer processBuffer;
  private static final FastDateFormat FORMATTER = FastDateFormat.getInstance("dd.MM.yyyy HH:mm:ss");

  public ValvomoServiceImpl() {
    this(DEFAULT_CAPACITY);
  }

  public ValvomoServiceImpl(int capacity) {
    this.processBuffer = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(capacity));
  }

  @Override
  public void fail(T process, Exception exception, String message) {
    StringBuffer buffer = newBufferWithDate("VIRHE");
    if (exception == null) {
      LOG.info("Process failed {}", process);
      buffer.append(", ").append("<< null exception >>");
    } else {
      LOG.error("Process failed " + process + " with exception", exception);
      buffer.append(", ").append(exception.getMessage());
    }
    if (exception != null && process instanceof ExceptionStack) {
      LOG.error("fail got exception", exception);
      StringBuffer s =
          new StringBuffer()
              .append(message)
              .append(": ")
              .append(exception.getClass())
              .append(": ")
              .append(exception.getMessage());
      if (!((ExceptionStack) process).addException(s.toString())) {
        // already in process buffer was not first
        return;
      }
    }
    processBuffer.add(new ProsessiJaStatus<T>(process, buffer.toString(), Status.FAILED));
  }

  @Override
  public void finish(T prosessi) {
    processBuffer.add(
        new ProsessiJaStatus<T>(
            prosessi, newBufferWithDate("LOPETETTU").toString(), Status.FINISHED));
  }

  @Override
  public void start(T prosessi) {
    processBuffer.add(
        new ProsessiJaStatus<T>(
            prosessi, newBufferWithDate("ALOITETTU").toString(), Status.STARTED));
  }

  public ProsessiJaStatus<T> getAjossaOlevaProsessiJaStatus() {
    Iterator<ProsessiJaStatus<T>> uusimmat = getUusimmatProsessitJaStatukset().iterator();
    if (uusimmat.hasNext()) {
      ProsessiJaStatus<T> uusin = uusimmat.next();
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
        (Function<ProsessiJaStatus<T>, T>) input -> input.getProsessi());
  }

  @Override
  public Collection<ProsessiJaStatus<T>> getUusimmatProsessitJaStatukset() {
    return Collections.unmodifiableCollection(Lists.reverse(Lists.newArrayList(processBuffer)));
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
    return new StringBuffer().append(status).append(": ").append(FORMATTER.format(new Date()));
  }
}
