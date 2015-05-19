package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta;

import akka.actor.Props;
import akka.actor.UntypedActor;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaSupervisor;

import java.util.concurrent.atomic.AtomicInteger;

final public class LaskennanKaynnistajaActor extends UntypedActor {

    private final LaskentaSupervisor laskentaSupervisor;
    private final int maxWorkers;
    private AtomicInteger workerCount = new AtomicInteger(0);

    private LaskennanKaynnistajaActor(final LaskentaSupervisor laskentaSupervisor, final int maxWorkers) {
        this.laskentaSupervisor = laskentaSupervisor;
        this.maxWorkers = maxWorkers;
    }

    public static Props props(final LaskentaSupervisor laskentaSupervisor) {
        return Props.create(LaskennanKaynnistajaActor.class, () -> new LaskennanKaynnistajaActor(laskentaSupervisor, 8));
    }

    @Override
    public void onReceive(Object message){
        if (WorkAvailable.class.isInstance(message)) {
            process();
        } else if (WorkerAvailable.class.isInstance(message)){
            workerCount.decrementAndGet();
            process();
        } else if (NoWorkAvailable.class.isInstance(message)) {
            workerCount.decrementAndGet();
        }
    }

    void process() {
        if (workerCount.getAndIncrement() < maxWorkers) {
            laskentaSupervisor.haeJaKaynnistaLaskenta();
        }
    }
}
