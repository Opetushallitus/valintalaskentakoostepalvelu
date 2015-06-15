package fi.vm.sade.valinta.kooste.util;

import java.util.Map;

import com.google.common.collect.Maps;

public class KonversioBuilder {

    private final Map<String, String> konversiotaulu;

    public KonversioBuilder() {
        this.konversiotaulu = Maps.newHashMap();
    }

    public KonversioBuilder addKonversio(Object[] objs) {
        return this;
    }

    public KonversioBuilder addKonversio(String arvo, String nimike) {
        konversiotaulu.put(arvo, nimike);
        return this;
    }

    public Map<String, String> build() {
        return konversiotaulu;
    }

}
