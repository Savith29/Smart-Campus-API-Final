package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Part 5.5 — Request and Response Logging Filter.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext req) throws IOException {
        LOG.info(String.format("[REQUEST]  %-7s %s",
                req.getMethod(),
                req.getUriInfo().getRequestUri()));
    }

    @Override
    public void filter(ContainerRequestContext req,
                       ContainerResponseContext res) throws IOException {
        LOG.info(String.format("[RESPONSE] %-7s %s  ->  %d",
                req.getMethod(),
                req.getUriInfo().getRequestUri(),
                res.getStatus()));
    }
}
