package com.smartcampus.mapper;

import com.smartcampus.JsonUtil;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {
    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",     "error");
        body.put("httpStatus", 422);
        body.put("error",      "Unprocessable Entity - Referenced Resource Not Found");
        body.put("message",    ex.getMessage());
        body.put("hint",       "Ensure the roomId exists before registering a sensor.");
        return Response.status(422).entity(JsonUtil.toJson(body))
                .type(MediaType.APPLICATION_JSON).build();
    }
}
