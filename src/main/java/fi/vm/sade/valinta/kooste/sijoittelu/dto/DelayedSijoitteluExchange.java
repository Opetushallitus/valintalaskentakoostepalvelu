package fi.vm.sade.valinta.kooste.sijoittelu.dto;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.spi.Synchronization;
import org.apache.camel.spi.UnitOfWork;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class DelayedSijoitteluExchange implements Exchange, Delayed {

	private final Exchange delegate;
	private final DelayedSijoittelu delayedSijoittelu;

	public DelayedSijoitteluExchange(DelayedSijoittelu delayedSijoittelu,
			Exchange exchange) {
		exchange.getIn().setBody(delayedSijoittelu);
		this.delegate = exchange;
		this.delayedSijoittelu = delayedSijoittelu;
	}

	public DelayedSijoittelu getDelayedSijoittelu() {
		return delayedSijoittelu;
	}

	public long getDelay(TimeUnit unit) {
		return delayedSijoittelu.getDelay(unit);
	}

	public int compareTo(Delayed o) {
		return delayedSijoittelu.compareTo(o);
	}

	@Override
	public void addOnCompletion(Synchronization onCompletion) {
		delegate.addOnCompletion(onCompletion);
	}

	@Override
	public boolean containsOnCompletion(Synchronization onCompletion) {
		return delegate.containsOnCompletion(onCompletion);
	}

	@Override
	public Exchange copy() {
		return new DelayedSijoitteluExchange(delayedSijoittelu, delegate.copy());
	}

	@Override
	public CamelContext getContext() {
		return delegate.getContext();
	}

	@Override
	public Exception getException() {
		return delegate.getException();
	}

	@Override
	public <T> T getException(Class<T> type) {
		return delegate.getException(type);
	}

	@Override
	public String getExchangeId() {
		return delegate.getExchangeId();
	}

	@Override
	public Endpoint getFromEndpoint() {
		return delegate.getFromEndpoint();
	}

	@Override
	public String getFromRouteId() {
		return delegate.getFromRouteId();
	}

	@Override
	public Message getIn() {
		return delegate.getIn();
	}

	@Override
	public <T> T getIn(Class<T> type) {
		return delegate.getIn(type);
	}

	@Override
	public Message getOut() {
		return delegate.getOut();
	}

	@Override
	public <T> T getOut(Class<T> type) {
		return delegate.getOut(type);
	}

	@Override
	public ExchangePattern getPattern() {
		return delegate.getPattern();
	}

	@Override
	public Map<String, Object> getProperties() {
		return delegate.getProperties();
	}

	@Override
	public Object getProperty(String name) {
		return delegate.getProperty(name);
	}

	@Override
	public <T> T getProperty(String name, Class<T> type) {
		return delegate.getProperty(name, type);
	}

	@Override
	public Object getProperty(String name, Object defaultValue) {
		return delegate.getProperty(name, defaultValue);
	}

	@Override
	public <T> T getProperty(String name, Object defaultValue, Class<T> type) {
		return delegate.getProperty(name, defaultValue, type);
	}

	@Override
	public UnitOfWork getUnitOfWork() {
		return delegate.getUnitOfWork();
	}

	@Override
	public List<Synchronization> handoverCompletions() {
		return delegate.handoverCompletions();
	}

	@Override
	public void handoverCompletions(Exchange target) {
		delegate.handoverCompletions(target);
	}

	@Override
	public boolean hasOut() {
		return delegate.hasOut();
	}

	@Override
	public boolean hasProperties() {
		return delegate.hasProperties();
	}

	@Override
	public Boolean isExternalRedelivered() {
		return delegate.isExternalRedelivered();
	}

	@Override
	public boolean isFailed() {
		return delegate.isFailed();
	}

	@Override
	public boolean isRollbackOnly() {
		return delegate.isRollbackOnly();
	}

	@Override
	public boolean isTransacted() {
		return delegate.isTransacted();
	}

	@Override
	public Object removeProperty(String name) {
		return delegate.removeProperty(name);
	}

	@Override
	public void setException(Throwable t) {
		delegate.setException(t);
	}

	@Override
	public void setExchangeId(String id) {
		delegate.setExchangeId(id);
	}

	@Override
	public void setFromEndpoint(Endpoint fromEndpoint) {
		delegate.setFromEndpoint(fromEndpoint);
	}

	@Override
	public void setFromRouteId(String fromRouteId) {
		delegate.setFromRouteId(fromRouteId);
	}

	@Override
	public void setIn(Message in) {
		delegate.setIn(in);
	}

	@Override
	public void setOut(Message out) {
		delegate.setOut(out);
	}

	@Override
	public void setPattern(ExchangePattern pattern) {
		delegate.setPattern(pattern);
	}

	@Override
	public void setProperty(String name, Object value) {
		delegate.setProperty(name, value);
	}

	@Override
	public void setUnitOfWork(UnitOfWork unitOfWork) {
		delegate.setUnitOfWork(unitOfWork);
	}
}
