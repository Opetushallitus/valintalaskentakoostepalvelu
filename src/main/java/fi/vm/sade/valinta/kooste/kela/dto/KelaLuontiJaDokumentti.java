package fi.vm.sade.valinta.kooste.kela.dto;

import java.io.InputStream;

public class KelaLuontiJaDokumentti {
    private final KelaLuonti luonti;
    private final byte[] dokumentti;

    public KelaLuontiJaDokumentti(KelaLuonti luonti, byte[] dokumentti) {
        this.luonti = luonti;
        this.dokumentti = dokumentti;
    }

    public byte[] getDokumentti() {
        return dokumentti;
    }

    public KelaLuonti getLuonti() {
        return luonti;
    }
}
