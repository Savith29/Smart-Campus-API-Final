package com.smartcampus.resource;

import com.smartcampus.JsonUtil;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * Part 4.2 — Readings Sub-Resource
 * Handles /api/v1/sensors/{sensorId}/readings
 */
@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // GET /api/v1/sensors/{sensorId}/readings
    @GET
    public Response getReadings() {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null)
            return error(404, "Sensor with id '" + sensorId + "' not found.");

        List<SensorReading> list =
                store.getReadings().getOrDefault(sensorId, Collections.emptyList());

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",   "/api/v1/sensors/" + sensorId + "/readings");
        links.put("sensor", "/api/v1/sensors/" + sensorId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sensorId", sensorId);
        body.put("count",    list.size());
        body.put("readings", list);
        body.put("_links",   links);

        return Response.ok(JsonUtil.toJson(body), MediaType.APPLICATION_JSON).build();
    }

    // POST /api/v1/sensors/{sensorId}/readings
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addReading(String requestBody) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null)
            return error(404, "Sensor with id '" + sensorId + "' not found.");

        // Part 5.3 — Block MAINTENANCE sensors → 403
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                "Sensor '" + sensorId + "' is in MAINTENANCE mode and cannot accept new readings."
            );
        }

        SensorReading reading;
        try {
            reading = JsonUtil.fromJson(requestBody, SensorReading.class);
        } catch (Exception e) {
            return error(400, "Invalid JSON body: " + e.getMessage());
        }

        if (reading == null)
            return error(400, "Request body with 'value' is required.");

        if (reading.getId() == null || reading.getId().trim().isEmpty())
            reading.setId(UUID.randomUUID().toString());
        if (reading.getTimestamp() == 0)
            reading.setTimestamp(System.currentTimeMillis());

        store.getReadings()
                .computeIfAbsent(sensorId, k -> new ArrayList<>())
                .add(reading);

        // Side-effect: update parent sensor currentValue
        sensor.setCurrentValue(reading.getValue());

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",   "/api/v1/sensors/" + sensorId + "/readings");
        links.put("sensor", "/api/v1/sensors/" + sensorId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message",                  "Reading recorded successfully.");
        body.put("reading",                  reading);
        body.put("updatedSensorCurrentValue", sensor.getCurrentValue());
        body.put("_links",                   links);

        return Response.status(201)
                .entity(JsonUtil.toJson(body))
                .type(MediaType.APPLICATION_JSON).build();
    }

    // GET /api/v1/sensors/{sensorId}/readings/{readingId}
    @GET
    @Path("/{readingId}")
    public Response getReadingById(@PathParam("readingId") String readingId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null)
            return error(404, "Sensor with id '" + sensorId + "' not found.");

        List<SensorReading> list =
                store.getReadings().getOrDefault(sensorId, Collections.emptyList());

        Optional<SensorReading> found = list.stream()
                .filter(r -> readingId.equals(r.getId())).findFirst();

        if (!found.isPresent())
            return error(404, "Reading '" + readingId + "' not found.");

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",   "/api/v1/sensors/" + sensorId + "/readings/" + readingId);
        links.put("all",    "/api/v1/sensors/" + sensorId + "/readings");
        links.put("sensor", "/api/v1/sensors/" + sensorId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reading", found.get());
        body.put("_links",  links);

        return Response.ok(JsonUtil.toJson(body), MediaType.APPLICATION_JSON).build();
    }

    private Response error(int status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",  "error");
        body.put("message", message);
        return Response.status(status)
                .entity(JsonUtil.toJson(body))
                .type(MediaType.APPLICATION_JSON).build();
    }
}
