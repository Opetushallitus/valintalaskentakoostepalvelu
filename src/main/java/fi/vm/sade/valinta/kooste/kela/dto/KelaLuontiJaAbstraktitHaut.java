package fi.vm.sade.valinta.kooste.kela.dto;

import java.util.Collection;

public class KelaLuontiJaAbstraktitHaut {
    private final KelaLuonti luonti;
    private final Collection<KelaAbstraktiHaku> haut;

    public KelaLuontiJaAbstraktitHaut(KelaLuonti luonti, Collection<KelaAbstraktiHaku> haut) {
        this.haut = haut;
        this.luonti = luonti;
    }

    public Collection<KelaAbstraktiHaku> getHaut() {
        return haut;
    }

    public KelaLuonti getLuonti() {
        return luonti;
    }
}
