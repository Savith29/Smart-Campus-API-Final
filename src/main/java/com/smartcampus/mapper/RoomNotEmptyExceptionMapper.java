package com.smartcampus.mapper;

import com.smartcampus.JsonUtil;
import com.smartcampus.exception.RoomNotEmptyException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {
    @Override
    public Response toResponse(RoomNotEmptyException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",     "error");
        body.put("httpStatus", 409);
        body.put("error",      "Conflict - Room Not Empty");
        body.put("message",    ex.getMessage());
        body.put("hint",       "Remove all sensors before deleting this room.");
        return Response.status(409).entity(JsonUtil.toJson(body))
                .type(MediaType.APPLICATION_JSON).build();
    }
}
