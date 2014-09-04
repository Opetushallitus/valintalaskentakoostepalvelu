package fi.vm.sade.valinta.kooste;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class KoostepalveluResourceConfig extends ResourceConfig {
	public KoostepalveluResourceConfig() {
		// packages("fi.vm.sade.valinta.dokumenttipalvelu.resource.impl.DokumenttiResourceImpl");
		// json output and input
		/**
		 * CORS Filter
		 */
		register(new ContainerResponseFilter() {

			@Override
			public void filter(ContainerRequestContext requestContext,
					ContainerResponseContext responseContext)
					throws IOException {
				responseContext.getHeaders().add("Access-Control-Allow-Origin",
						"*");
			}
		});

		packages("fi.vm.sade.valinta.kooste");

		register(com.fasterxml.jackson.jaxrs.annotation.JacksonFeatures.class);

		registerInstances(
				new com.wordnik.swagger.jaxrs.listing.ResourceListingProvider(),
				new com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider());
		register(com.wordnik.swagger.jaxrs.listing.ApiListingResourceJSON.class);
	}
}
