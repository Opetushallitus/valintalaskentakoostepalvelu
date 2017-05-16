package fi.vm.sade.valinta.kooste.url;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Usage:
 *
 * FakeCommonProperties.setProperties(ImmutableMap.of("host.virkailija", "testi.virkailija.opintopolku.fi"));
 *
 */
public class FakeCommonProperties {

    public static void setProperties(Map<String, String> values) {
        setProperties(values.entrySet().stream().map(entry -> entry.getKey()+"="+entry.getValue()).collect(Collectors.toList()));
    }

    public static void setProperties(List<String> lines) {
        final Path home;
        final Path ophConfiguration;
        final File commonProperties;
        try {
            home = Files.createTempDirectory("tmphome");
            ophConfiguration = Paths.get(home.toString(), "oph-configuration");
            ophConfiguration.toFile().mkdir();
            commonProperties = new File(ophConfiguration.toFile(), "common.properties");
            commonProperties.deleteOnExit();
            System.setProperty("user.home", home.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            OutputStream output = new FileOutputStream(commonProperties);
            IOUtils.writeLines(lines, System.lineSeparator(), output, Charset.defaultCharset());
            IOUtils.closeQuietly(output);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
