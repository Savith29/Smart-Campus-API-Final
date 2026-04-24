package com.smartcampus.mapper;

import com.smartcampus.JsonUtil;
import com.smartcampus.exception.SensorUnavailableException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class SensorUnavailableExceptionMapper
        implements ExceptionMapper<SensorUnavailableException> {
    @Override
    public Response toResponse(SensorUnavailableException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",     "error");
        body.put("httpStatus", 403);
        body.put("error",      "Forbidden - Sensor Unavailable");
        body.put("message",    ex.getMessage());
        body.put("hint",       "Wait for the sensor to return to ACTIVE status.");
        return Response.status(403).entity(JsonUtil.toJson(body))
                .type(MediaType.APPLICATION_JSON).build();
    }
}
