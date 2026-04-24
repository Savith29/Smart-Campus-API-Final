# Smart Campus Sensor & Room Management API

### 5COSC022W - Client Server Architectures Coursework 2025/26
      Author: Savith Ransika 
      UOW ID: w2121663
      IIT ID: 20240931
---

## Overview

This project is a RESTful API built using JAX-RS (Jersey) deployed as a Web Application
on Apache Tomcat. The API manages Rooms and Sensors across a university Smart Campus.
All data is stored in-memory using ConcurrentHashMap. No database is used.

**Base URL:** `http://localhost:8080/smartcampus-api/api/v1/`

## API Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET | /api/v1/ | Discovery - API metadata |
| GET | /api/v1/rooms | Get all rooms |
| POST | /api/v1/rooms | Create a new room |
| GET | /api/v1/rooms/{roomId} | Get room by ID |
| DELETE | /api/v1/rooms/{roomId} | Delete a room |
| GET | /api/v1/sensors | Get all sensors |
| GET | /api/v1/sensors?type=Temperature | Filter sensors by type |
| POST | /api/v1/sensors | Register a new sensor |
| GET | /api/v1/sensors/{sensorId} | Get sensor by ID |
| DELETE | /api/v1/sensors/{sensorId} | Delete a sensor |
| GET | /api/v1/sensors/{sensorId}/readings | Get reading history |
| POST | /api/v1/sensors/{sensorId}/readings | Add a new reading |
| GET | /api/v1/sensors/{sensorId}/readings/{id} | Get single reading |

---

## Sample curl Commands

**1. Discovery**
```bash
curl -X GET http://localhost:8080/smartcampus-api/api/v1/
```

**2. Create a Room**
```bash
curl -X POST http://localhost:8080/smartcampus-api/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"LIB-301\",\"name\":\"Library Quiet Study\",\"capacity\":80}"
```

**3. Get All Rooms**
```bash
curl -X GET http://localhost:8080/smartcampus-api/api/v1/rooms
```

**4. Get Room by ID**
```bash
curl -X GET http://localhost:8080/smartcampus-api/api/v1/rooms/LIB-301
```

**5. Create a Sensor**
```bash
curl -X POST http://localhost:8080/smartcampus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"TEMP-001\",\"type\":\"Temperature\",\"status\":\"ACTIVE\",\"currentValue\":22.5,\"roomId\":\"LIB-301\"}"
```

**6. Filter Sensors by Type**
```bash
curl -X GET "http://localhost:8080/smartcampus-api/api/v1/sensors?type=Temperature"
```

**7. Add a Sensor Reading**
```bash
curl -X POST http://localhost:8080/smartcampus-api/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d "{\"value\":25.6}"
```

**8. Get Reading History**
```bash
curl -X GET http://localhost:8080/smartcampus-api/api/v1/sensors/TEMP-001/readings
```

**9. Delete Room with Sensors - expect 409 Conflict**
```bash
curl -X DELETE http://localhost:8080/smartcampus-api/api/v1/rooms/LIB-301
```

**10. Create Sensor with invalid roomId - expect 422**
```bash
curl -X POST http://localhost:8080/smartcampus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"TEMP-002\",\"type\":\"Temperature\",\"status\":\"ACTIVE\",\"currentValue\":20,\"roomId\":\"INVALID\"}"
```

---
# Project Structure
scfinal/
├── pom.xml
└── src/main/java/com/smartcampus/
    ├── Main.java
    ├── JsonUtil.java
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   └── SensorReading.java
    ├── store/
    │   └── DataStore.java
    ├── resource/
    │   ├── DiscoveryResource.java
    │   ├── RoomResource.java
    │   ├── SensorResource.java
    │   └── SensorReadingResource.java
    ├── exception/
    │   ├── RoomNotEmptyException.java
    │   ├── LinkedResourceNotFoundException.java
    │   └── SensorUnavailableException.java
    ├── mapper/
    │   ├── RoomNotEmptyExceptionMapper.java
    │   ├── LinkedResourceNotFoundExceptionMapper.java
    │   ├── SensorUnavailableExceptionMapper.java
    │   └── GlobalExceptionMapper.java
    └── filter/
        └── LoggingFilter.java
        └── webapp/
        └── WEB-INF/
        └── web.xml

---

## HTTP Status Codes

| Code | Meaning | When Used |
|------|---------|-----------|
| 200 | OK | Successful GET or DELETE |
| 201 | Created | Successful POST |
| 400 | Bad Request | Missing required fields |
| 403 | Forbidden | Reading from MAINTENANCE sensor |
| 404 | Not Found | Room or Sensor ID does not exist |
| 409 | Conflict | Deleting room that still has sensors |
| 415 | Unsupported Media Type | Wrong Content-Type header |
| 422 | Unprocessable Entity | Sensor POST with invalid roomId |
| 500 | Internal Server Error | Unexpected server error |

---

## Conceptual Report - Question Answers

---

### Part 1.1 - JAX-RS Resource Class Lifecycle

By default, JAX-RS creates a new instance of each resource class for every incoming
HTTP request. This is called the per-request lifecycle. Once the request is finished
and the response is sent, that instance is discarded by the runtime.

This means if I stored data inside the resource class as a normal instance field like
HashMap rooms = new HashMap(), that data would get reset on every single request
and nothing would be saved between calls. To fix this, I created a separate
DataStore.java class that holds all the data in static fields. Static fields belong
to the class itself rather than to any particular instance, so they survive across
all requests for the entire lifetime of the application.

Because multiple HTTP requests can arrive at the same time and are handled by
different threads, I used ConcurrentHashMap instead of a plain HashMap. A plain
HashMap can get corrupted if two threads try to write to it simultaneously.
ConcurrentHashMap handles concurrent access safely without needing manual
synchronization. Jersey also supports the @Singleton annotation on resource classes,
which tells the runtime to create only one shared instance for all requests, which
is an alternative way to manage shared state.

---

### Part 1.2 - HATEOAS

HATEOAS stands for Hypermedia As The Engine Of Application State. It represents
Level 3 of the Richardson Maturity Model, which is the highest level of REST API
design maturity.

In a HATEOAS-compliant API, every response includes links that tell the client what
actions are available next and where they can navigate. For example, when a client
calls GET /api/v1/, the response does not just return basic data but also provides
links to /api/v1/rooms and /api/v1/sensors so the client knows where to go next
without needing any separate documentation.

This approach is better than static documentation because documentation becomes
outdated quickly as the API evolves. If I change a URL path on the server, any
client that navigates by following hypermedia links will still work correctly because
it just follows whatever links the server provides in its responses. Clients that
hard-code URLs into their application code will break. HATEOAS also makes the API
easier to explore and understand because the API explains itself through the
responses it gives.

---

### Part 2.1 - Returning Full Objects vs IDs Only

If the API returns only IDs in a list response such as LIB-301 and LAB-102, the
initial response is very small and lightweight. However the client then needs to
send one separate GET request for every single room ID to retrieve the actual
room details. If there are one hundred rooms that means one hundred additional
HTTP requests. This is known as the N+1 problem and it significantly increases
total latency and places more load on the server.

If the API returns full room objects in the list response, the client receives all
the data it needs in a single request with no additional calls required. The
trade-off is that the response payload is larger. In production systems this is
typically managed using pagination such as adding page and size query parameters
so that large collections are returned in smaller manageable chunks.

For this Smart Campus API I chose to return full objects in list responses because
it is more practical for a facilities management system where users need to see
complete room details immediately without waiting for extra network calls.

---

### Part 2.2 - Is DELETE Idempotent?

Yes, DELETE is idempotent according to the HTTP specification. Idempotent means
that sending the same request multiple times produces the same final state on the
server as sending it once.

The first call to DELETE /api/v1/rooms/LIB-301 finds the room in the data store,
verifies that it has no sensors assigned, removes it, and returns HTTP 200 OK.
A second identical request finds no room with that ID and returns HTTP 404 Not Found.

The HTTP response codes are different between the two calls but the important point
is that the server state is the same after both calls because the room does not
exist in either case. The server is not modified a second time. This property makes
DELETE safe to retry in situations where a network timeout occurs and the client is
not sure whether the first request was processed. The client can send the request
again without risking accidentally deleting something twice.

POST is not idempotent because sending POST twice creates two separate resources.

---

### Part 3.1 - What Happens With Wrong Content-Type?

The @Consumes(MediaType.APPLICATION_JSON) annotation instructs the JAX-RS runtime
to only accept requests where the Content-Type header is set to application/json.

If a client sends a request with Content-Type set to text/plain or application/xml,
the JAX-RS runtime performs content negotiation before the resource method is even
invoked. It finds no method capable of consuming that media type and automatically
responds with HTTP 415 Unsupported Media Type. The request body is completely
rejected without any attempt to deserialize it and no business logic runs at all.

This behavior is beneficial for security because it prevents the server from
attempting to parse data in unexpected formats which could trigger errors or
unexpected behavior. It also clearly communicates to API clients exactly which
format they are required to use, making debugging integration issues much easier.

---

### Part 3.2 - @QueryParam vs Path Segment for Filtering

Using a path segment approach like /api/v1/sensors/type/CO2 is semantically
incorrect because it implies that type/CO2 is a distinct resource within the
hierarchy, which it is not. It is simply a filter criterion applied to the
existing sensors collection.

The correct REST convention is to use query parameters for filtering, searching,
and sorting operations on a collection. Using @QueryParam with a URL like
/api/v1/sensors?type=Temperature is better for several reasons.

Query parameters are optional by nature so /api/v1/sensors still works perfectly
without any filter and returns all sensors. Multiple filters can be combined
cleanly in a single URL such as ?type=Temperature&status=ACTIVE without changing
the resource path structure. The base resource URL remains clean and semantically
stable. HTTP caching infrastructure, reverse proxies, and API gateways all
understand and correctly handle query parameters for filtering purposes.

Using path segments for filtering would also make URL design increasingly complex
and difficult to maintain as filtering requirements grow over time.

---

### Part 4.1 - Sub-Resource Locator Pattern Benefits

The Sub-Resource Locator pattern in JAX-RS allows a resource method to return an
instance of another class that then handles the remaining portion of the request
URL. In this project, SensorResource contains a method annotated with
@Path("{sensorId}/readings") that returns a new instance of SensorReadingResource
constructed with the sensorId. All operations related to readings are then handled
entirely by that dedicated class.

The benefits of this pattern compared to defining all nested endpoints in one
large controller class are significant.

It enforces the Single Responsibility Principle because SensorResource only manages
sensors and SensorReadingResource only manages readings. Each class has one clear
purpose and is independently testable.

It improves scalability of the codebase because defining dozens of nested endpoints
in one massive class creates code that is very hard to read, debug, and maintain.
Keeping responsibilities in separate classes makes each one manageable.

It enables clean contextual initialization because the sensorId is passed to the
SensorReadingResource constructor, meaning all methods in that class automatically
know which sensor they are working with without needing to extract the path
parameter repeatedly.

It aligns the code structure with the real-world domain model because sensors
naturally own their readings, just as rooms naturally own their sensors.

---

### Part 5.2 - Why 422 Instead of 404 for Invalid roomId?

HTTP 404 Not Found is the correct response when the requested URL or endpoint
itself does not exist. For example calling GET /api/v1/rooms/NONEXISTENT correctly
returns 404 because there is no room resource at that specific URL path.

The situation is different when a client sends POST /api/v1/sensors with a
syntactically valid JSON body that contains a roomId field referencing a room that
does not exist in the system. The endpoint /api/v1/sensors clearly exists and is
reachable. The problem is not a missing URL but rather that the semantic content
of the request body is invalid because a foreign key reference inside it cannot
be resolved to an existing entity.

HTTP 422 Unprocessable Entity is the appropriate response for this situation
because it signals that the server received and successfully parsed the JSON but
cannot process the request due to the semantic content being invalid. Using 404
in this case would mislead the client into thinking the API endpoint itself is
broken or missing, rather than correctly communicating that their request body
contains an unresolvable dependency. Accurate error codes help client developers
implement correct error handling and diagnose problems efficiently.

---

### Part 5.4 - Security Risks of Exposing Stack Traces

Returning raw Java stack traces to external API clients is a serious security
vulnerability that should always be avoided in production systems.

Stack traces reveal internal class and package names such as
com.smartcampus.store.DataStore which exposes the complete internal architecture
of the application to potential attackers.

They also disclose the exact names and versions of libraries being used. An
attacker can search security vulnerability databases like the CVE database to
find known exploits that affect those specific library versions.

Stack traces sometimes include file system paths from the server such as full
directory paths, which reveals information about the operating system and
deployment environment that can be used to plan further attacks.

The method names and line numbers visible in stack traces reveal the internal
logic flow of the application, helping an attacker understand how to craft
malicious inputs that trigger specific vulnerabilities or bypass validation.

The GlobalExceptionMapper in this project addresses all of these risks by
catching every unhandled Throwable, logging the complete stack trace on the
server side only using java.util.logging.Logger, and returning only a generic
safe message to the client stating that an unexpected error occurred.

---

### Part 5.5 - Why Use Filters Instead of Manual Logging?

There are several important reasons why using a JAX-RS filter for logging is
superior to manually adding Logger.info() statements inside every resource method.

The first reason is the DRY principle which stands for Do Not Repeat Yourself.
If logging statements must be written manually inside every resource method and
the API grows to have thirty or forty endpoints, the same logging code must be
written and maintained thirty or forty times. Changing the log format later
requires updating every single method.

The second reason is separation of concerns. Resource methods should contain only
business logic related to their specific function. Mixing logging infrastructure
code into business logic makes methods harder to read, test, and maintain
independently.

The third reason is completeness. A developer adding a new endpoint might forget
to include a logging statement. A registered JAX-RS filter automatically intercepts
every request and response without any exceptions, ensuring a complete and
consistent audit trail across the entire API.

The fourth reason is flexibility. JAX-RS filters can be selectively applied to
specific endpoints using the @NameBinding annotation mechanism, providing precise
control without requiring any changes to the resource classes themselves.

This approach follows the established industry pattern of Aspect-Oriented
Programming which is the standard practice for handling cross-cutting concerns
such as logging, authentication, and rate limiting in enterprise-grade APIs.
