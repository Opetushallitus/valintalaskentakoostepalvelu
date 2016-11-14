package fi.vm.sade.valinta.kooste.pistesyotto.service;

import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.OrganisaatioTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockValintalaskentaValintakoeAsyncResource;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class AbstractPistesyottoKoosteServiceTest {
    private static final List<String> OPPILAITOS = Collections.singletonList(AbstractPistesyottoKoosteService.OPPILAITOS);
    private ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource = new MockValintalaskentaValintakoeAsyncResource();
    private AbstractPistesyottoKoosteService service = new AbstractPistesyottoKoosteService(null, null, null, null, null, null, valintalaskentaValintakoeAsyncResource) {};
    private final String tarjoajaOid = "oidOfTarjoajaThatNeedsOppilaitos";
    private final String oppilaitostyyppi = "oppilaitostyyppi_61#1";

    /*
    * root
    * |
    * level2oid1, level2oid2
    * |
    * level3oid1, level3oid2
    * |
    * level4oid1, level4oid2
    * |
    * level5oid1, level5oid2
    * */

    private List<OrganisaatioTyyppi> fifthLevel = Arrays.asList(
        createOrganisation("level5oid1", nimi("Level 5 org 1"), Collections.emptyList(), oppilaitostyyppi, Collections.singletonList("TOIMIPISTE")),
        createOrganisation("level5oid2", nimi("Level 5 org 2"), Collections.emptyList(), oppilaitostyyppi, Collections.singletonList("TOIMIPISTE")));
    private List<OrganisaatioTyyppi> fourthLevel = Arrays.asList(
        createOrganisation("level4oid1", nimi("Level 4 org 1"), fifthLevel, oppilaitostyyppi, Collections.singletonList("TOIMIPISTE")),
        createOrganisation("level4oid2", nimi("Level 4 org 2"), Collections.emptyList(), oppilaitostyyppi, Collections.singletonList("TOIMIPISTE")));
    private List<OrganisaatioTyyppi> thirdLevel = Arrays.asList(
        createOrganisation("level3oid1", nimi("Level 3 org 1"), fourthLevel, oppilaitostyyppi, Collections.singletonList("TOIMIPISTE")),
        createOrganisation("level3oid2", nimi("Level 3 org 2"), Collections.emptyList(), oppilaitostyyppi, Collections.singletonList("TOIMIPISTE")));
    private List<OrganisaatioTyyppi> secondLevel = Arrays.asList(
        createOrganisation("level2oid1", nimi("Level 2 org 1"), thirdLevel, oppilaitostyyppi, Collections.singletonList("TOIMIPISTE")),
        createOrganisation("level2oid2", nimi("Level 2 org 2"), Collections.emptyList(), oppilaitostyyppi, Collections.singletonList("TOIMIPISTE")));
    private List<OrganisaatioTyyppi> root = Collections.singletonList(createOrganisation("rootOid", nimi("Root"), secondLevel, oppilaitostyyppi, Collections.singletonList("TOIMIPISTE")));

    private final AtomicReference<String> oppilaitosRef = new AtomicReference<>();

    @Test
    public void oppilaitosTypeTarjoajaIsReturned() {
        OrganisaatioTyyppi tarjoajaThatIsOppilaitos = thirdLevel.get(0);
        tarjoajaThatIsOppilaitos.setOid(tarjoajaOid);
        tarjoajaThatIsOppilaitos.setOrganisaatiotyypit(OPPILAITOS);

        service.etsiOppilaitosHierarkiasta(tarjoajaOid, root, oppilaitosRef);
        Assert.assertEquals(tarjoajaOid, oppilaitosRef.get());
    }

    @Test
    public void closestOppilaitosTypeOrganisationAboveTarjoajaIsReturnedIfFound() {
        OrganisaatioTyyppi tarjoaja = thirdLevel.get(0);
        tarjoaja.setOid(tarjoajaOid);

        OrganisaatioTyyppi closestOppilaitosAbove = secondLevel.get(0);
        closestOppilaitosAbove.setOrganisaatiotyypit(OPPILAITOS);
        OrganisaatioTyyppi oppilaitosHigherAbove = root.get(0);
        oppilaitosHigherAbove.setOrganisaatiotyypit(OPPILAITOS);
        fifthLevel.get(0).setOrganisaatiotyypit(OPPILAITOS);

        service.etsiOppilaitosHierarkiasta(tarjoajaOid, root, oppilaitosRef);
        Assert.assertEquals(closestOppilaitosAbove.getOid(), oppilaitosRef.get());
    }

    @Test
    public void ifThereIsNoOppilaitosTypeOrganisationAboveTarjoajaThenClosestBelowIsReturned() {
        OrganisaatioTyyppi tarjoaja = thirdLevel.get(0);
        tarjoaja.setOid(tarjoajaOid);
        OrganisaatioTyyppi closestOppilaitosBelow = fourthLevel.get(1);
        closestOppilaitosBelow.setOrganisaatiotyypit(OPPILAITOS);
        OrganisaatioTyyppi oppilaitosLowerBelow = fifthLevel.get(0);
        oppilaitosLowerBelow.setOrganisaatiotyypit(OPPILAITOS);

        service.etsiOppilaitosHierarkiasta(tarjoajaOid, root, oppilaitosRef);
        Assert.assertEquals(closestOppilaitosBelow.getOid(), oppilaitosRef.get());
    }

    @Test
    public void nullIsReturnedIfThereIsNoOppilaitosTypeTarjoajaInHierarhy() {
        service.etsiOppilaitosHierarkiasta(tarjoajaOid, root, oppilaitosRef);
        Assert.assertEquals(null, oppilaitosRef.get());
    }

    private static OrganisaatioTyyppi createOrganisation(String oid, Map<String, String> nimi, List<OrganisaatioTyyppi> children, String oppilaitostyyppi, List<String> organisaatiotyypit) {
        return new OrganisaatioTyyppi(oid, nimi, children, oppilaitostyyppi, organisaatiotyypit);
    }

    private static Map<String,String> nimi(String nimi) {
        return Collections.singletonMap("fi", nimi);
    }
}
