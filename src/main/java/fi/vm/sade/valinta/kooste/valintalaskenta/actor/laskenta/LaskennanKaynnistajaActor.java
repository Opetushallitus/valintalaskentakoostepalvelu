package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta;

import akka.actor.Props;
import akka.actor.UntypedActor;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaSupervisor;

final public class LaskennanKaynnistajaActor extends UntypedActor {

    final LaskentaSupervisor laskentaSupervisor;
    final int maxWorkers;
    int workerCount = 0;

    private LaskennanKaynnistajaActor(final LaskentaSupervisor laskentaSupervisor, final int maxWorkers){
        this.laskentaSupervisor = laskentaSupervisor;
        this.maxWorkers = maxWorkers;
    }

    public static Props props(final LaskentaSupervisor laskentaSupervisor) {
        return Props.create(LaskennanKaynnistajaActor.class, () -> {
            return new LaskennanKaynnistajaActor(laskentaSupervisor, 8);
        });
    }

    public static Props props(final LaskentaSupervisor laskentaSupervisor, final int maxWorkers) {
        return Props.create(LaskennanKaynnistajaActor.class, () -> {
            return new LaskennanKaynnistajaActor(laskentaSupervisor, maxWorkers);
        });
    }

    @Override
    public void onReceive(Object message){
        if (WorkAvailable.class.isInstance(message))
            process();
        else if (WorkerAvailable.class.isInstance(message)){
            workerCount--;
            process();
        }
    }

    void process() {
        while(workerCount < maxWorkers) {
            String uuid = laskentaSupervisor.haeJaKaynnistaLaskenta();
            if (null == uuid)
                return;
            workerCount++;
        }
    }
}
