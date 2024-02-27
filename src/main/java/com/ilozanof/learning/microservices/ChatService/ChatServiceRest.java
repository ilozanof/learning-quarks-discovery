package com.ilozanof.learning.microservices.ChatService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Path("")
public class ChatServiceRest {

    private static final Logger LOG = LoggerFactory.getLogger(ChatServiceRest.class);

    @RestClient
    private ChatServiceClient restClient;

    @ConfigProperty(name="chat-service.id")
    private String serviceId;

    @GET
    @Path("/showFriends")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> showFriends(@QueryParam("showFriends") int numFriends) {
        int NUM_FRIENDS_TO_LOOK_UP = 20;
        List<String> result = IntStream
                .range(1, NUM_FRIENDS_TO_LOOK_UP)
                .mapToObj(i -> restClient.sayYourName())
                .collect(Collectors.toList());
        return result;
    }

    @GET
    @Path("/sayYourName")
    @Produces(MediaType.APPLICATION_JSON)
    public String sayYourName() {
        return serviceId;
    }
}
