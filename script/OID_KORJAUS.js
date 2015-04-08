// Erillishaun 'null'-OID:ien korjaus skripti
// BUG-243
function() {

    cursor = db.Valintatulos.find({oid_korjaus:true});
    cursor.forEach(function(otus) {
        hakukohdeAndHakuOid =
            otus.hakukohdeOid.split('.').join("") +
            otus.hakuOid.split('').reverse().join("").split('.').join("");
        lopullinenOid = hakukohdeAndHakuOid.substring(0, 32);
        printjson(lopullinenOid);
        otus.oid_korjaus = true;
        otus.valintatapajonoOid = lopullinenOid;
        db.Valintatulos.save(otus);
    });
}