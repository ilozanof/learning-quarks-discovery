package com.ilozanof.learning.microservices.ChatService;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.ServiceOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ChatServiceApp {

    private static Logger LOG = LoggerFactory.getLogger(ChatServiceApp.class);

    @ConfigProperty(name="chat-service.id")
    private String serviceId;

    @ConfigProperty(name = "quarkus.http.port")
    int servicePort;

    @ConfigProperty(name = "consul.host")
    String consultHost;

    @ConfigProperty(name = "consul.port")
    int consultPort;

    public void init(@Observes StartupEvent event, Vertx vertx) {
        LOG.atInfo().log("Registering App with Consul...");
        ConsulClient client = ConsulClient.create(vertx, new ConsulClientOptions().setHost(consultHost).setPort(consultPort));
        LOG.atInfo().log("Consul Registration: Address: {}, Id: {}, Port: {}", serviceId, serviceId, this.servicePort);
        client.registerService(new ServiceOptions().setPort(this.servicePort).setAddress(serviceId).setName("chat-service").setId(serviceId));
        LOG.atInfo().log("Registration with Consul complete.");
    }
}
