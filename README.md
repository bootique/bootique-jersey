<!--
  Licensed to ObjectStyle LLC under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ObjectStyle LLC licenses
  this file to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
  -->

[![build test deploy](https://github.com/bootique/bootique-jersey/actions/workflows/maven.yml/badge.svg)](https://github.com/bootique/bootique-jersey/actions/workflows/maven.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.bootique.jersey/bootique-jersey.svg?colorB=brightgreen)](https://search.maven.org/artifact/io.bootique.jersey/bootique-jersey/)

# bootique-jersey
Provides [Jersey](https://jersey.java.net/) JAX-RS server and client integration with [Bootique](http://bootique.io).
This provides the necessary API to create REST services and consume someone else's REST services.

## Jersey Server

Integrates JAX-RS server as a servlet in Bootique. See usage example
[bootique-rest-demo](https://github.com/bootique-examples/bootique-rest-demo). A few quick tips:


_Jakarta vs JavaEE: Bootique 3.x supports both the legacy JavaEE and the newer Jakarta versions of Jersey. Each 
Bootique Jersey module is shipped in two flavors (with and without `-jakarta` in the name). The examples below a based 
on the newer Jakarta modules. But It is your choice which one to use. The API of both is identical (except for the 
import package)._

Add Jersey server capabilities to your Bootique app:
```xml
<dependency>
	<groupId>io.bootique.jersey</groupId>
	<artifactId>bootique-jersey-jakarta</artifactId>
</dependency>
```

Publish individual endpoints:
```java
public void configure(Binder binder) {
    JerseyModule.extend(binder)
        .addResource(MyApi1.class)
        .addResource(MyApi2.class);
}
```

Add JSON serialization capabilities:
```xml
<dependency>
	<groupId>io.bootique.jersey</groupId>
	<artifactId>bootique-jersey-jakarta-jackson</artifactId>
</dependency>
```

Exclude null properties from JSON responses:
```java
public void configure(Binder binder) {
    JerseyJacksonModule.extend(binder).skipNullProperties();
}
```

Enable debug output of the application resources:

```yaml
log:
  loggers:
    io.bootique.jersey:
      level: debug # or "trace" for even more detailed output
```

## Jersey Client

Integrates JAX-RS-based HTTP client in Bootique with support for various types of
server authentication (BASIC, OAuth2, etc.). Allows to configure multiple
client runtime parameters, as well as define server URL endpoints.
Implementation is built on top of Jersey and Grizzly connector.

### Basic Client Usage

Add the client module to your Bootique app:

```xml
<dependency>
	<groupId>io.bootique.jersey</groupId>
	<artifactId>bootique-jersey-jakarta-client</artifactId>
</dependency>
```

Or if you need clients with health checks and metrics:

```xml
<dependency>
	<groupId>io.bootique.jersey</groupId>
	<artifactId>bootique-jersey-jakarta-client-instrumented</artifactId>
</dependency>
```

Inject `HttpClientFactory` and create client instances:

```java
@Inject
private HttpClientFactory clientFactory;

public void doSomething() {

    Client client = clientFactory.newClient();
    Response response = client
        .target("https://example.org")
        .request()
        .get();
}
```

Optionally, configure client parameters:
```yml
jerseyclient:
  followRedirects: true
  readTimeoutMs: 2000
  connectTimeoutMs: 2000
  asyncThreadPoolSize: 10
```

### URL Targets

In the example above we injected `HttpClientFactory` (that produced instances
of JAX RS `Client`), and hardcoded the endpoint URL in Java. Instead you
can map multiple URLs in the ```.yml```, assigning each URL a symbolic
name and optionally providing URL-specific runtime parameters:

```yml
jerseyclient:
  targets:
    google:
      url: "https://google.com"
    bootique:
      url: "https://bootique.io"
      followRedirects: false
```
Now you can inject `HttpTargets` and acquire instances of `WebTarget`
by name:
```java
@Inject
private HttpTargets targets;

public void doSomething() {

    Response response = targets.newTarget("bootique").request().get();
}
```
This not only reduces the amount of code, but more importantly allows
to manage your URLs (and their runtime parameters) via configuration.
E.g. you might use a different URL between test and production environments
without changing the code.

### BASIC Authentication

If your server endpoint requires BASIC authentication, you can associate
your Clients and WebTargets with a named auth configuration. One or more
named configurations are set up like this:

```yml
jerseyclient:
  auth:
    myauth:
      type: basic
      username: myuser
      password: mypassword
```
When creating a client in the Java code you can reference auth name ("myauth"):
```java
@Inject
private HttpClientFactory clientFactory;

public void doSomething() {

    Client client = clientFactory.newBuilder().auth("myauth").build();
    Response response = client
        .target("https://example.org")
        .request()
        .get();
}
```
Or you can associate a target with it:
```yml
jerseyclient:
  ...
  targets:
    secret:
      url: "https://example.org"
      auth: myauth
```

### OAuth2 Authentication

OAuth2 authenticator supports an ability for the app to obtain tokens transparently using an OAuth2 token endpoint at 
the specified URL, and then pass the tokens to the client requests. Here is an example auth for a Twitter client:

```yml
jerseyclient:
  auth:
    twitter:
      type: oauth2
      tokenUrl: https://api.twitter.com/oauth2/token
      username: sdfjkdferefxfkdsf
      password: Efcdsfdsflkurecdsfj
```

### Header-Based Authentication

Sometimes the token is known upfront and doesn't expire, so the app can use it for all requests. For this there is
another type of authenticator called `apiKeyHeader`. Here is an example with a configuration that uses an OAuth2-like 
bearer token config:

```yml
jerseyclient:
  auth:
    myauth:
      type: apiKeyHeader
      name: "Authorization"
      key: "Bearer XXXXXXXXXXXXXXXXXX"
```
Of course, the name of the header is configurable and can be something else if needed.

### Query Key-Based Authentication

Although not as common, a security token can be passed as a URL query parameter. The setup is similar to the header-based 
auth above. Just a different type of authenticator:

```yml
jerseyclient:
  auth:
    myauth:
      type: apiKeyParameter
      name: "apiKey"
      key: "XXXXXXXXXXXXXXXXXX"
```
