package fi.vm.sade.valinta.kooste.valvomo.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;

public class OsaTyo extends Tyo {
    private final AtomicInteger kokonaismaara;
    private final AtomicInteger ohitettu;
    private final String nimi;
    private final AtomicInteger tehty;
    private final AtomicInteger epaonnistunut;
    private final Collection<Exception> poikkeukset;

    public OsaTyo(String nimi, int kokonaismaara) {
        this.kokonaismaara = new AtomicInteger(kokonaismaara);
        this.ohitettu = new AtomicInteger(0);
        this.tehty = new AtomicInteger(0);
        this.epaonnistunut = new AtomicInteger(0);
        this.poikkeukset = Collections.synchronizedCollection(Lists.<Exception>newArrayList());
        this.nimi = nimi;
    }

    public OsaTyo(String nimi) {
        this(nimi, -1);
    }

    public int getOhitettu() {
        return ohitettu.get();
    }

    public int getJaljellaOlevienToidenMaara() {
        if (kokonaismaara.get() == -1) { // kokonaismaaraa ei tiedeta
            return -1;
        }
        return kokonaismaara.get() - tehty.get();
    }

    public void setKokonaismaara(int kokonaismaara) {
        this.kokonaismaara.set(kokonaismaara);
    }

    /**
     * Inkrementoi -1:stä suoraan 1:een. Olettaen että -1 tarkoittaa ettei
     * työmääräarviota ollut vielä tehty
     */
    public void inkrementoiKokonaismaaraa() {
        if (!this.kokonaismaara.compareAndSet(-1, 1)) {
            this.kokonaismaara.incrementAndGet();
        }
    }

    public void inkrementoiKokonaismaaraa(int delta) {
        if (!this.kokonaismaara.compareAndSet(-1, delta)) {
            this.kokonaismaara.addAndGet(delta);
        }
    }

    public int getKokonaismaara() {
        return kokonaismaara.get();
    }

    public String getNimi() {
        return nimi;
    }

    public int getTehty() {
        return tehty.get();
    }

    public int tyoValmistui(long kesto) {
        return tehty.incrementAndGet();
    }

    public void tyoOhitettu() {
        tehty.incrementAndGet();
        ohitettu.incrementAndGet();
    }

    public boolean isValmis() {
        if (kokonaismaara.get() == -1) {
            return false;
        }
        return kokonaismaara.get() <= tehty.get();
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public Collection<Exception> getPoikkeukset() {
        return poikkeukset;
    }

    public void tyoEpaonnistui(long kesto, Exception e) {
        epaonnistunut.incrementAndGet();
        poikkeukset.add(e);
    }
}
