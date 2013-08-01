package fi.vm.sade.valinta.kooste.filter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

public class CORSFilter implements ContainerResponseFilter {
    /**
     * Add the cross domain data to the output if needed
     * 
     * @param creq
     *            The container request (input)
     * @param cres
     *            The container request (output)
     * @return The output request with cross domain if needed
     */
    @Override
    public ContainerResponse filter(ContainerRequest creq, ContainerResponse cres) {
        cres.getHttpHeaders().add("Access-Control-Allow-Origin", "*");
        cres.getHttpHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
        cres.getHttpHeaders().add("Access-Control-Allow-Credentials", "true");
        cres.getHttpHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD, ACCEPTED");
        cres.getHttpHeaders().add("Access-Control-Max-Age", "1209600");
        return cres;
    }
}
