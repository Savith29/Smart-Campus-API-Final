package com.smartcampus.resource;

import com.smartcampus.JsonUtil;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Part 3 — Sensor Operations
 * Part 4 — Sub-Resource Locator
 * Handles /api/v1/sensors
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    // GET /api/v1/sensors?type=XX
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> list = new ArrayList<>(store.getSensors().values());

        if (type != null && !type.trim().isEmpty()) {
            list = list.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", "/api/v1/sensors");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("count",   list.size());
        if (type != null && !type.trim().isEmpty())
            body.put("filteredByType", type);
        body.put("sensors", list);
        body.put("_links",  links);

        return Response.ok(JsonUtil.toJson(body), MediaType.APPLICATION_JSON).build();
    }

    // POST /api/v1/sensors
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(String requestBody) {
        Sensor sensor;
        try {
            sensor = JsonUtil.fromJson(requestBody, Sensor.class);
        } catch (Exception e) {
            return error(400, "Invalid JSON body: " + e.getMessage());
        }

        if (sensor == null || blank(sensor.getId()))
            return error(400, "Sensor 'id' is required.");
        if (blank(sensor.getType()))
            return error(400, "Sensor 'type' is required.");
        if (blank(sensor.getRoomId()))
            return error(400, "Sensor 'roomId' is required.");
        if (store.getSensors().containsKey(sensor.getId()))
            return error(409, "Sensor with id '" + sensor.getId() + "' already exists.");

        // Dependency validation — roomId must exist → 422
        if (!store.getRooms().containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                "Cannot register sensor: roomId '" + sensor.getRoomId()
                + "' does not exist in the system. Please create the room first."
            );
        }

        if (blank(sensor.getStatus())) sensor.setStatus("ACTIVE");

        store.getSensors().put(sensor.getId(), sensor);
        store.getRooms().get(sensor.getRoomId()).getSensorIds().add(sensor.getId());
        store.getReadings().put(sensor.getId(), new ArrayList<>());

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",     "/api/v1/sensors/" + sensor.getId());
        links.put("readings", "/api/v1/sensors/" + sensor.getId() + "/readings");
        links.put("room",     "/api/v1/rooms/"   + sensor.getRoomId());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Sensor registered successfully.");
        body.put("sensor",  sensor);
        body.put("_links",  links);

        return Response.status(201)
                .entity(JsonUtil.toJson(body))
                .type(MediaType.APPLICATION_JSON).build();
    }

    // GET /api/v1/sensors/{sensorId}
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null)
            return error(404, "Sensor with id '" + sensorId + "' not found.");

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",     "/api/v1/sensors/" + sensorId);
        links.put("readings", "/api/v1/sensors/" + sensorId + "/readings");
        links.put("room",     "/api/v1/rooms/"   + sensor.getRoomId());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sensor",  sensor);
        body.put("_links",  links);

        return Response.ok(JsonUtil.toJson(body), MediaType.APPLICATION_JSON).build();
    }

    // DELETE /api/v1/sensors/{sensorId}
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null)
            return error(404, "Sensor with id '" + sensorId + "' not found.");

        if (sensor.getRoomId() != null && store.getRooms().containsKey(sensor.getRoomId())) {
            store.getRooms().get(sensor.getRoomId()).getSensorIds().remove(sensorId);
        }
        store.getSensors().remove(sensorId);
        store.getReadings().remove(sensorId);

        Map<String, String> links = new LinkedHashMap<>();
        links.put("all", "/api/v1/sensors");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Sensor '" + sensorId + "' deleted successfully.");
        body.put("_links",  links);

        return Response.ok(JsonUtil.toJson(body), MediaType.APPLICATION_JSON).build();
    }

    // Part 4 — Sub-Resource Locator
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }

    private Response error(int status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",  "error");
        body.put("message", message);
        return Response.status(status)
                .entity(JsonUtil.toJson(body))
                .type(MediaType.APPLICATION_JSON).build();
    }

    private boolean blank(String s) { return s == null || s.trim().isEmpty(); }
}
