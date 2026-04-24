package com.smartcampus.resource;

import com.smartcampus.JsonUtil;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Part 2 — Room Management
 * Handles /api/v1/rooms
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    // GET /api/v1/rooms
    @GET
    public Response getAllRooms() {
        List<Room> list = new ArrayList<>(store.getRooms().values());

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", "/api/v1/rooms");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("count",  list.size());
        body.put("rooms",  list);
        body.put("_links", links);

        return Response.ok(JsonUtil.toJson(body), MediaType.APPLICATION_JSON).build();
    }

    // POST /api/v1/rooms
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRoom(String requestBody) {
        Room room;
        try {
            room = JsonUtil.fromJson(requestBody, Room.class);
        } catch (Exception e) {
            return error(400, "Invalid JSON body: " + e.getMessage());
        }

        if (room == null || blank(room.getId()))
            return error(400, "Room 'id' is required.");
        if (blank(room.getName()))
            return error(400, "Room 'name' is required.");
        if (store.getRooms().containsKey(room.getId()))
            return error(409, "Room with id '" + room.getId() + "' already exists.");

        store.getRooms().put(room.getId(), room);

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", "/api/v1/rooms/" + room.getId());
        links.put("all",  "/api/v1/rooms");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Room created successfully.");
        body.put("room",    room);
        body.put("_links",  links);

        return Response.status(201)
                .entity(JsonUtil.toJson(body))
                .type(MediaType.APPLICATION_JSON).build();
    }

    // GET /api/v1/rooms/{roomId}
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null)
            return error(404, "Room with id '" + roomId + "' not found.");

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",    "/api/v1/rooms/" + roomId);
        links.put("sensors", "/api/v1/sensors");
        links.put("all",     "/api/v1/rooms");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("room",   room);
        body.put("_links", links);

        return Response.ok(JsonUtil.toJson(body), MediaType.APPLICATION_JSON).build();
    }

    // DELETE /api/v1/rooms/{roomId}
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null)
            return error(404, "Room with id '" + roomId + "' not found.");

        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                "Cannot delete room '" + roomId + "' because sensors are still assigned to it: "
                + room.getSensorIds() + ". Please remove all sensors first."
            );
        }

        store.getRooms().remove(roomId);

        Map<String, String> links = new LinkedHashMap<>();
        links.put("all", "/api/v1/rooms");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Room '" + roomId + "' deleted successfully.");
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

    private boolean blank(String s) { return s == null || s.trim().isEmpty(); }
}
