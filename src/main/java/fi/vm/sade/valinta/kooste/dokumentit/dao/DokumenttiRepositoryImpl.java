package fi.vm.sade.valinta.kooste.dokumentit.dao;

import fi.vm.sade.valinta.kooste.dokumentit.dto.DokumenttiDto;
import fi.vm.sade.valinta.kooste.dokumentit.dto.VirheilmoitusDto;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DokumenttiRepositoryImpl implements DokumenttiRepository {
  private static final Logger LOG = LoggerFactory.getLogger(DokumenttiRepositoryImpl.class);

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public DokumenttiRepositoryImpl(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  @Transactional
  public DokumenttiDto hae(UUID uuid) {
    // haetaan ilmoitukset mappiin, avain laskennan uuid + hakuoid
    RowMapper<VirheilmoitusDto> ilmoitusRowmapper =
        (rs, rownum) -> new VirheilmoitusDto("", rs.getString("ilmoitus"));
    List<VirheilmoitusDto> ilmoitukset =
        this.jdbcTemplate.query(
            "SELECT * FROM dokumentinseuranta_virheet WHERE dokumentti_uuid=?::uuid",
            ilmoitusRowmapper,
            uuid.toString());

    RowMapper<DokumenttiDto> dokumenttiDtoRowMapper =
        (rs, rowNum) ->
            new DokumenttiDto(
                rs.getString("uuid"),
                rs.getString("kuvaus"),
                rs.getBoolean("valmis"),
                ilmoitukset.size() == 0 ? null : ilmoitukset);

    return this.jdbcTemplate.queryForObject(
        "SELECT * FROM dokumentinseuranta WHERE uuid=?::uuid",
        dokumenttiDtoRowMapper,
        uuid.toString());
  }

  @Override
  public UUID luoDokumentti(String kuvaus) {
    UUID uuid = UUID.randomUUID();
    this.jdbcTemplate.update(
        "INSERT INTO dokumentinseuranta (uuid, kuvaus, valmis, aloitettu) VALUES (?::uuid, ?, false, ?::timestamptz)",
        uuid.toString(),
        kuvaus,
        Instant.now().toString());
    return uuid;
  }

  @Override
  public void paivitaKuvaus(final UUID uuid, String kuvaus) {
    this.jdbcTemplate.update(
        "UPDATE dokumentinseuranta SET kuvaus=? WHERE uuid=?::uuid", kuvaus, uuid.toString());
  }

  @Override
  public void merkkaaValmiiksi(UUID uuid) {
    this.jdbcTemplate.update(
        "UPDATE dokumentinseuranta SET valmis=true WHERE uuid=?::uuid", uuid.toString());
  }

  @Override
  public void lisaaVirheilmoitus(UUID uuid, String ilmoitus) {
    this.jdbcTemplate.update(
        "INSERT INTO dokumentinseuranta_virheet (dokumentti_uuid, ilmoitus) VALUES (?::uuid, ?)",
        uuid.toString(),
        ilmoitus);
  }
}
