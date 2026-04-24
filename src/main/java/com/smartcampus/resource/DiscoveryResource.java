package com.smartcampus.resource;

import com.smartcampus.JsonUtil;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Part 1.2 — Discovery Endpoint
 * GET /api/v1/
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",    "/api/v1/");
        links.put("rooms",   "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("apiName",     "Smart Campus Sensor & Room Management API");
        body.put("version",     "1.0.0");
        body.put("description", "This endpoint provides API metadata and available resources.");
        body.put("contact",     "admin@smartcampus.ac.uk");
        body.put("status",      "RUNNING");
        body.put("resources",   links);

        return Response.ok(JsonUtil.toJson(body), MediaType.APPLICATION_JSON).build();
    }
}
