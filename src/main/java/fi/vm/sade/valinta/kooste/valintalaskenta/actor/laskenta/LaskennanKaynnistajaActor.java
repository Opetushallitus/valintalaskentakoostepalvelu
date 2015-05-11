package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta;

import akka.actor.Props;
import akka.actor.UntypedActor;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaSupervisor;

public class LaskennanKaynnistajaActor extends UntypedActor {

    LaskentaSupervisor laskentaSupervisor;
    int workerCount = 0;
    int maxWorkers = 8;

    public LaskennanKaynnistajaActor(final LaskentaSupervisor laskentaSupervisor){
        this.laskentaSupervisor = laskentaSupervisor;
    }

    public static Props props(final LaskentaSupervisor laskentaSupervisor) {
        return Props.create(() -> {
            return new LaskennanKaynnistajaActor(laskentaSupervisor);
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
        if (workerCount > maxWorkers)
            return;
        String uuid = null;

        if (null == uuid)
            return;

        //laskentaSupervisor.luoJaKaynnistaLaskenta();
        workerCount++;
    }
}
