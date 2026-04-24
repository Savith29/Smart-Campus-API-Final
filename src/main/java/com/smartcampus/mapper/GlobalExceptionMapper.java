package com.smartcampus.mapper;

import com.smartcampus.JsonUtil;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        LOG.log(Level.SEVERE, "Unexpected error: " + ex.getMessage(), ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",     "error");
        body.put("httpStatus", 500);
        body.put("error",      "Internal Server Error");
        body.put("message",    "An unexpected error occurred. Please contact the administrator.");
        return Response.status(500).entity(JsonUtil.toJson(body))
                .type(MediaType.APPLICATION_JSON).build();
    }
}
