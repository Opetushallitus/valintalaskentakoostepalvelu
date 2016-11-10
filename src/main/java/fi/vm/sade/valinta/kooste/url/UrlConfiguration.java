package fi.vm.sade.valinta.kooste.url;

import fi.vm.sade.properties.OphProperties;

import java.nio.file.Paths;

public class UrlConfiguration extends OphProperties {

    private volatile static UrlConfiguration instance;

    private UrlConfiguration() {
        addFiles("/valintalaskentakoostepalvelu-oph.properties");
        addOptionalFiles(Paths.get(System.getProperties().getProperty("user.home"), "/oph-configuration/common.properties").toString());
    }

    /**
     * Get instance of UrlConfiguration.
     *
     * This method initializes a new instance of UrlConfiguration, if one has not yet been initialized.
     * Initialization occurs lazily and uses double checked locking for thread safety
     * (see Joshua Bloch: Effective Java, Item 71: Use lazy initialization judiciously).
     *
     * @return UrlConfiguration
     */
    public static UrlConfiguration getInstance() {
        UrlConfiguration i = instance;
        if (i == null) {
            synchronized(UrlConfiguration.class) {
                i = instance;
                if (i == null) {
                    instance = i = new UrlConfiguration();
                }
            }
        }
        return i;
    }
}
