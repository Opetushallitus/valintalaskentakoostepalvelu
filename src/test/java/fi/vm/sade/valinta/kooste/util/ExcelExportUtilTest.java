package fi.vm.sade.valinta.kooste.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.GenericType;

import org.apache.poi.util.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.Valintatulos;
import fi.vm.sade.valinta.kooste.sijoittelu.resource.TilaResource;
import fi.vm.sade.valinta.kooste.util.excel.Span;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.SijoittelunTulosExcelKomponentti;

public class ExcelExportUtilTest {
	private SijoitteluResource sijoitteluajoResource = Mockito.mock(SijoitteluResource.class);
	private TilaResource tilaResource = Mockito.mock(TilaResource.class);
	private ApplicationResource applicationResource = Mockito.mock(ApplicationResource.class);
	
	// SijoittelunTulosExcelKomponentti
	@Ignore
	@Test
	public void testExport() throws IOException {
		List<Valintatulos> valintatulokset = new GsonBuilder()
				.registerTypeAdapter(Date.class, new JsonDeserializer() {
					@Override
					public Object deserialize(JsonElement json, Type typeOfT,
											  JsonDeserializationContext context)
							throws JsonParseException {
						// TODO Auto-generated method stub
						return new Date(json.getAsJsonPrimitive().getAsLong());
					}

				})
				.create().fromJson(new InputStreamReader(new ClassPathResource("util/tilat.json").getInputStream()), new TypeToken<List<Valintatulos>>() {
				}.getType());
		
		HakukohdeDTO sijoittelu =  new GsonBuilder()
		.registerTypeAdapter(Date.class, new JsonDeserializer() {
			@Override
			public Object deserialize(JsonElement json, Type typeOfT,
					JsonDeserializationContext context)
					throws JsonParseException {
				// TODO Auto-generated method stub
				return new Date(json.getAsJsonPrimitive().getAsLong());
			}

		})
		.create().fromJson(new InputStreamReader(new ClassPathResource("util/sijoittelu.json").getInputStream()),  new TypeToken<HakukohdeDTO>() {
		}.getType());
		
		List<Hakemus> listfull = new Gson().fromJson(new InputStreamReader(new ClassPathResource("util/listfull.json").getInputStream()),  new TypeToken<List<Hakemus>>() {
		}.getType());
		
		FileOutputStream out = new FileOutputStream("f.xls");
		
		SijoittelunTulosExcelKomponentti sijoittelunTulosExcelKomponentti =
		new SijoittelunTulosExcelKomponentti(sijoitteluajoResource, tilaResource, applicationResource);
		
		Mockito.when(applicationResource.getApplicationsByOid(Mockito.anyString(), Mockito.anyString(), Mockito.anyList(), Mockito.anyInt())).thenReturn(listfull);
		Mockito.when(sijoitteluajoResource.getHakukohdeBySijoitteluajoPlainDTO(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(sijoittelu);
		
		InputStream inp = 
				sijoittelunTulosExcelKomponentti.luoXls(valintatulokset, "latest", KieliUtil.SUOMI, "", "", sijoittelu.getOid(), "hakuOid");
				//ExcelExportUtil.exportGridAsXls(grid.toArray(new Object[][]{}));
		IOUtils.copy(inp, out);
	}
}
