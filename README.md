# learning-quarks-discovery
POC using Microservices with Quarkus/Consul/Stork

* Consul is a Service Registry and Service Discovery for Microservices
* Stork is a Load-Balancer that allows to choose a different instance of a Microservice using different balancing strategies


In this *POC* we'll develop a very simple *Chat-Service* that represents a person talking in a virtual Chat, 
and it only provides 2 REST endpoints:

* */sayYourName*: It returns the name of this Person
* */showFriends*: It returns a list of some of the People connected to the Chat, including it self. 
So it goes on a loop, calling their "/sayYourName" Endpoint, and collecting them all in a list.

## Structure of the POC

* Our *Chat-Service* is a standalone WebSerivce running on its own container and exposing an HTTP port so we can check 
its endpoints with a Browser.
* The */sayYourName* endpoint just prints out the name of this Service/person. 
Its value is taking from `application.properties` but it can be overriden with the Environment variable *ID*
* The */showFriends* will collect a List of different Services/Persons also connected. We'll achieve this by 
running multiple instances of the same *Chat-service*, each one with a different *ID*. For the sake of
simplicity, our *Chat-service* will make a *loop*, making a call to another *Chat-Service*. This *call* will 
be *load-balanced*, so each time we'll be calling a different Chat-Service.
* In order to achieve this load-balancing, each Chat-service will register itself on startup on the *Consul* 
server. The *Rest Client* will be configured to delegate the Service resolution to *Stork*.

Example of architecture:
* We have *Consul* running in a Container
* We run 3 instance sof *Chat-Service*:
  * "Alice", listening to port 9100
  * "Bob", listening to port 9101
  * "John", listening to port 9102
* We can reach any of these services individually and get their names:
  * http://localhost:9100/sayYourName
  * http://localhost:9101/sayYourName
  * http://localhost:912/sayYourName
* We can also reach any of them and call */showFriends*. In this case, for example, if we 
call "http://localhost:9100/showFriends" (Alice), this service will make a Loop (say 10 times), and 
on each iteration it will make a HTTP Call to the *Chat-Service*. So, it's making a call to itself. But 
since we have multiple instances of the SAME service running, the RestClient will make use of 
*Consul* and *Stork* to pick a different one on each iteration. So the final result of this call will be 
a list of different services running.

> For this to work, every Service needs to register itself to Consul on Startup


## First Step: Create the Project.

First we create a GitHub repo in our Account (this Repo)

Then we initialize the maven project using the mvn command line:

```
mvn archetype:generate -DgroupId=com.ilozanof.learning.github -DartifactId=actions -DarchetypeArtifactId=
maven-archetype-quickstart -DinteractiveMode=false
```

The App is a very simple Rest Endpoint using *Quarkus*. So first we define the dependencies in
our *pom.xml* :

```
<properties>
    ...
    <quarkus.platform.group-id>io.quarkus.platform</quarkus.platform.group-id>
    <quarkus.platform.version>3.2.3.Final</quarkus.platform.version>
    <quarkus.platform.artifact-id>quarkus-bom</quarkus.platform.artifact-id>
    ...
  </properties>
  ...
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>io.quarkus.platform</groupId>
        <artifactId>${quarkus.platform.artifact-id}</artifactId>
        <version>${quarkus.platform.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  ...
  <dependencies>
    ...
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-resteasy-reactive-jackson</artifactId>
    </dependency>
  </dependencies>
  ...
  <build>
    <plugins>
      ...
      <plugin>
        <groupId>${quarkus.platform.group-id}</groupId>
        <artifactId>quarkus-maven-plugin</artifactId>
        <version>${quarkus.platform.version}</version>
        <extensions>true</extensions>
        <executions>
          <execution>
            <goals>
              <goal>build</goal>
              <goal>generate-code</goal>
              <goal>generate-code-tests</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      ...
    </plugins>
  </build>
```

We'll aso need to add the *Consul* and *Stork* Libraries:

```
<dependency>
  <groupId>io.smallrye.stork</groupId>
  <artifactId>stork-service-discovery-consul</artifactId>
</dependency>
<dependency>
  <groupId>io.smallrye.reactive</groupId>
  <artifactId>smallrye-mutiny-vertx-consul-client</artifactId>
</dependency>
```
## Rest Client configuration

When executing the */showFriends* endpoint, our service will make several calls to the Chat-service 
(itself) via HTTP. On every call, a different name will be returned since every call will reach a 
different instance of the service. 
Bu default, the Quarkus Rest Client only reaches one instance of a Service, which is determined by a Url and 
other properties like these:

```
quarkus.rest-client.chat-service.url=http://localhost:9100
quarkus.rest-client.chat-service.scope=jakarta.inject.Singleton
```

But this configuration only works with static Ips. To achieve the load balancing, we remove the previous 
config and use this one instead:
```
quarkus.stork.chat-service.service-discovery.type=consul
quarkus.stork.chat-service.service-discovery.consul-host=consul
quarkus.stork.chat-service.service-discovery.consul-port=8500
quarkus.stork.chat-service.load-balancer.type=round-robin
```

, where:

* ".consul-host": This is the Hostname of the server running *Consul*. In this case we use the docker service name 
since both *Consul* and the multiple instances of *Chat-Service* will be running in the same docker network, so they can be accessed by their *name*
* ".consul-port": Port of the Consul server. 8500 by default 

The Java code is almost exactly the same as a regular Rest client, which slightly differences:

```
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
```

## Running the POC

First you build the project:

```
mvn clean install
```

Then you build an image:

```
build -t chat-service .
```

Now we can start running the POC. 
First we create the network that *Consul* and the instances of *Chat-Service* will share:

```
docker network create chat-network
```

Now we start *Consul:
```
docker-compose -f docker-compose-conmsul.yml -d up
```

NOw we are starting multiple instances of our Chat-Service.

> NOTE: In order for our instances of *Chat-Service* to register to *Consul*, each Instance must be aware of its 
> own *Id*. Since there is no easy way to know this, each service is taking these values from 
> Environment Variables, which we can override when firing up each instance.

> Since both *Consul* and the instance sof our Service are running on teh same network, *Consul* only 
> needs to know the *internal* port of each *chat-service* (each instance will take it from 'quarkus.http.port')

We can trigger 3 instances with the following commands:

```
docker run -d -p 9100:8080 -e ID=Alice --name Alice --network chat-network chat-service
docker run -d -p 9101:8080 -e ID=Bob --name Bob --network chat-network chat-service
docker run -d -p 9102:8080 -e ID=John --name John --network chat-network chat-service
```

Now, if you access the *Consul* console (http://localhost:8500) you can see how all services 
are registered.


# Annexes

## Self-Registration problem

In this POC, the *Chat-Service* registers itself on startup. This it self is a problem, since we are making 
the service *aware" of the Self-registration process. There are other approaches to fix this, like using 
an external *Agent* that "listens" to the Docker environment and register automatically a service when its 
deployed. For example you can use Registrator:

* https://github.com/gliderlabs/registrator