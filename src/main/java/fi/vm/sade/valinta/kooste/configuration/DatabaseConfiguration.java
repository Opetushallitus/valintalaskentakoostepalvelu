package fi.vm.sade.valinta.kooste.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Properties;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseConfiguration {

  @Bean
  public DataSource dataSource(
      @Value("${valintalaskentakoostepalvelu.postgresql.maxactive}") final String maxPoolSize,
      @Value("${valintalaskentakoostepalvelu.postgresql.maxwait}") final String maxWait,
      @Value("${valintalaskentakoostepalvelu.postgresql.url}") final String url,
      @Value("${valintalaskentakoostepalvelu.postgresql.username}") final String user,
      @Value("${valintalaskentakoostepalvelu.postgresql.password}") final String password,
      @Value("${valintalaskentakoostepalvelu.postgresql.driver}") final String driverClassName) {
    final HikariConfig config = new HikariConfig();
    config.setConnectionTestQuery("SELECT 1");
    config.setJdbcUrl(url);
    config.setMaximumPoolSize(Integer.parseInt(maxPoolSize));
    config.setMaxLifetime(Long.parseLong(maxWait));
    final Properties dsProperties = new Properties();
    dsProperties.setProperty("url", url);
    dsProperties.setProperty("user", user);
    dsProperties.setProperty("password", password);
    config.setDataSourceProperties(dsProperties);
    if (!driverClassName.equals("")) config.setDriverClassName(driverClassName);
    return new HikariDataSource(config);
  }
}
