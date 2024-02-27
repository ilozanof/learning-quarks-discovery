package com.ilozanof.learning.microservices.ChatService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@ApplicationScoped
@RegisterRestClient(baseUri = "stork://chat-service")
@RegisterClientHeaders(ChatServiceClientHeaders.class)
public interface ChatServiceClient {
    @GET
    @Path(("/showFriends"))
    List<String> showFriends();

    @GET
    @Path("/sayYourName")
    String sayYourName();
}
