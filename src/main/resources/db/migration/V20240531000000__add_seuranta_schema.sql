CREATE TABLE IF NOT EXISTS dokumentinseuranta (
    uuid            UUID PRIMARY KEY,
    kuvaus          TEXT NOT NULL,
    valmis          BOOLEAN NOT NULL,
    aloitettu       TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS dokumentinseuranta_virheet (
    dokumentti_uuid UUID NOT NULL,
    ilmoitus        TEXT NOT NULL,
    CONSTRAINT fk_dokumentti_uuid FOREIGN KEY (dokumentti_uuid) REFERENCES dokumentinseuranta(uuid) ON DELETE CASCADE
);


