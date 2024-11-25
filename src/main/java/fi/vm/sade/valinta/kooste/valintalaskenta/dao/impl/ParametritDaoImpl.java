package fi.vm.sade.valinta.kooste.valintalaskenta.dao.impl;

import fi.vm.sade.valinta.kooste.valintalaskenta.dao.ParametritDao;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ParametritDaoImpl implements ParametritDao {

  private final JdbcTemplate jdbcTemplate;

  public ParametritDaoImpl(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Map<String, String> lueParametrit() {
    return this.jdbcTemplate
        .query(
            "SELECT nimi, arvo FROM parametrit",
            (rs, rowNum) -> Pair.of(rs.getString("nimi"), rs.getString("arvo")))
        .stream()
        .collect(Collectors.toMap(p -> p.getLeft(), p -> p.getRight()));
  }
}
