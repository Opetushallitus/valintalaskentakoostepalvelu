package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Koska sijoittelulla ei ole omaa API:a!
 */
public enum ValintatuloksenTila {
    ILMOITETTU, // Hakijalle on ilmoitettu, sijoittelun tulos ei voi muuttaa
                // paikkaa peruuntuneeksi
    VASTAANOTTANUT_LASNA, // Hakija ottanut paikan vastaan ja on lasna
    VASTAANOTTANUT_POISSAOLEVA, // Hakija ottanut paikan vastaan ja
                                // ilmoittautunut poissaolevaksi
    EI_VASTAANOTETTU_MAARA_AIKANA, // Hakija ei ole ilmoittanut paikkaa
                                   // vastaanotetuksi maaraaikana ja on nain
                                   // ollen hylatty
    PERUNUT; // Hakija ei ota paikkaa vastaan
}
