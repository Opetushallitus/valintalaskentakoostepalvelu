package fi.vm.sade.valinta.kooste.viestintapalvelu;

import com.google.common.collect.ImmutableList;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.KirjeetUtil;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KirjeetUtilTest {

    @Test
    public void testSortShouldNotFailOnNullData() {
        HakutoiveenValintatapajonoDTO v1 = new HakutoiveenValintatapajonoDTO();
        Arrays.asList(v1).sort(KirjeetUtil.sort());
    }

    @Test
    public void testShouldSortByPriorityAscending() {
        List<Integer> priot =
                flatten(jono().map(setPriority(5)).map(setTila(HakemuksenTila.HYVAKSYTTY)),
                        jono().map(setPriority(6)).map(setTila(HakemuksenTila.HYVAKSYTTY)),
                        jono().map(setPriority(7)).map(setTila(HakemuksenTila.HYVAKSYTTY)))
                        .sorted(KirjeetUtil.sort()).map(prioriteetit).collect(Collectors.toList());

        Assert.assertTrue(priot.equals(ImmutableList.of(5,6,7)));
    }

    @Test
    public void testShouldSortByTilaAfterPriority() {
        List<HakemuksenTila> tilas =
                flatten(jono().map(setPriority(4)).map(setTila(HakemuksenTila.HYLATTY)),
                        jono().map(setPriority(5)).map(setTila(HakemuksenTila.HYVAKSYTTY)),
                        jono().map(setPriority(6)).map(setTila(HakemuksenTila.VARALLA)),
                        jono().map(setPriority(6)).map(setTila(HakemuksenTila.HYVAKSYTTY)),
                        jono().map(setPriority(7)).map(setTila(HakemuksenTila.HYLATTY)))
                        .sorted(KirjeetUtil.sort()).map(tilat).collect(Collectors.toList());

        Assert.assertTrue(tilas.equals(
                ImmutableList.of(HakemuksenTila.HYLATTY,HakemuksenTila.HYVAKSYTTY,HakemuksenTila.HYVAKSYTTY,HakemuksenTila.VARALLA,HakemuksenTila.HYLATTY)));
    }
    @Test
    public void testShouldSortByVarasijaNumero() {
        List<HakutoiveenValintatapajonoDTO> jonos =
        flatten(jono().map(setPriority(5)).map(setTila(HakemuksenTila.HYVAKSYTTY)),
                jono().map(setPriority(6)).map(setTila(HakemuksenTila.VARALLA)).map(setVarasijaNumero(2)),
                jono().map(setPriority(6)).map(setTila(HakemuksenTila.VARALLA)).map(setVarasijaNumero(1)),
                jono().map(setPriority(7)).map(setTila(HakemuksenTila.HYVAKSYTTY)))
                .sorted(KirjeetUtil.sort()).collect(Collectors.toList());
        List<HakemuksenTila> tilas =jonos.stream().map(tilat).collect(Collectors.toList());

        Assert.assertTrue(tilas.equals(
                ImmutableList.of(HakemuksenTila.HYVAKSYTTY,HakemuksenTila.VARALLA,HakemuksenTila.VARALLA,HakemuksenTila.HYVAKSYTTY)));

        List<Integer> varasijas =jonos.stream().map(varasijanumerot).collect(Collectors.toList());
        Assert.assertTrue(varasijas.equals(
                ImmutableList.of(-1, 1,2, -1)));
    }

    private final Function<HakutoiveenValintatapajonoDTO, Integer> varasijanumerot = (jono) -> Optional.ofNullable(jono.getVarasijanNumero()).orElse(-1);
    private final Function<HakutoiveenValintatapajonoDTO, HakemuksenTila> tilat = (jono) -> jono.getTila();
    private final Function<HakutoiveenValintatapajonoDTO, Integer> prioriteetit = (jono) -> jono.getValintatapajonoPrioriteetti();
    private Stream<HakutoiveenValintatapajonoDTO> flatten(Stream<HakutoiveenValintatapajonoDTO>... jonot) {
        return Stream.of(jonot).flatMap(j -> j);
    }
    private List<HakutoiveenValintatapajonoDTO> asList(Stream<HakutoiveenValintatapajonoDTO>... jonot) {
        return Stream.of(jonot).flatMap(j -> j).collect(Collectors.toList());
    }

    private Stream<HakutoiveenValintatapajonoDTO> jono() {
        return Stream.of(new HakutoiveenValintatapajonoDTO());
    }
    private Function<HakutoiveenValintatapajonoDTO,HakutoiveenValintatapajonoDTO> setPriority(Integer priority) {
        return (jono) -> {
            jono.setValintatapajonoPrioriteetti(priority);
            return jono;
        };
    }
    private Function<HakutoiveenValintatapajonoDTO,HakutoiveenValintatapajonoDTO> setTila(HakemuksenTila tila) {
        return (jono) -> {
            jono.setTila(tila);
            return jono;
        };
    }
    private Function<HakutoiveenValintatapajonoDTO,HakutoiveenValintatapajonoDTO> setVarasijaNumero(Integer varasijaNumero) {
        return (jono) -> {
            jono.setVarasijanNumero(varasijaNumero);
            return jono;
        };
    }
}
