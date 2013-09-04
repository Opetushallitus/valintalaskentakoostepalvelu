package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Koska sijoittelulla ei ole omaa API:a!
 */
public enum HakemuksenTila {

    HYLATTY, // hakija ei voi koskaan tulla valituksi kohteeseen

    VARALLA, // Hakija voi tulla kohteeseen valituksi (jossain vaiheessa)

    PERUUNTUNUT, // Hakija on tullut valituksi parempaan paikkaan (korkeampi
                 // hakutoive)

    HYVAKSYTTY, // Hakija voi ottaa paikan vastaan

    PERUNUT; // Hakija ei ole vastaanottanut paikkaa. Hakija ei voi tulla enää
             // valituksi matalamman prioriteetin kohteissa
}
