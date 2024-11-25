CREATE TABLE IF NOT EXISTS parametrit (
                                      nimi  TEXT PRIMARY KEY,
                                      arvo  TEXT NOT NULL
);

INSERT INTO parametrit (nimi, arvo) VALUES ('parametritLimiter', '1000') ON CONFLICT DO NOTHING;
INSERT INTO parametrit (nimi, arvo) VALUES ('hakuLimiter', '1000') ON CONFLICT DO NOTHING;
INSERT INTO parametrit (nimi, arvo) VALUES ('hakukohderyhmatLimiter', '1000') ON CONFLICT DO NOTHING;
INSERT INTO parametrit (nimi, arvo) VALUES ('valintapisteetLimiter', '1000') ON CONFLICT DO NOTHING;
INSERT INTO parametrit (nimi, arvo) VALUES ('hakijaryhmatLimiter', '1000') ON CONFLICT DO NOTHING;
INSERT INTO parametrit (nimi, arvo) VALUES ('koskioppijatLimiter', '16') ON CONFLICT DO NOTHING;
INSERT INTO parametrit (nimi, arvo) VALUES ('ataruhakemuksetLimiter', '16') ON CONFLICT DO NOTHING;
INSERT INTO parametrit (nimi, arvo) VALUES ('valintaperusteetLimiter', '16') ON CONFLICT DO NOTHING;

-- käytetään eri lailla, permittejä varataan per hakija
INSERT INTO parametrit (nimi, arvo) VALUES ('suorituksetLimiter', '1000') ON CONFLICT DO NOTHING;