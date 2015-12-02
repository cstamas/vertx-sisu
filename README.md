# Vert.x Eclipse Sisu Extension

Adds (Eclipse Sisu)[https://www.eclipse.org/sisu/] enabled DI to Vert.x.

## "Local" mode

Heavily inspired by Vert.x vertx-service-factory.
Usable when Verticle you want is located in same classloader as your caller is, accepts FQ class names and `@Named` names.

Prefixes:
```
sisu:org.cstamas.vertx.sisu.examples.ExampleVerticle
```
or in case your Verticle has `@Named("example")` applied:
```
sisu:example
```

## "Remote" mode

Heavily inspired by Vert.x vertx-maven-service-factory.
Usable to deploy module downloaded from Maven2 repository, but without need to create service descriptors.

Prefixes:
```
sisu-remote:artifactCoordinate[::filter]
```
Where `artifactCoordinate` is the usual Artifact "one liner":

```
groupId:artifactId[:extension[:classifier]]:version
```
Filter is any-pattern like (just like!) filter, currently with very rudimentary support. If no filter present,
all discovered Verticles from module will get deployed. If filter present, only matched ones. Filter examples:
```
*Foo // "ends with"
Foo* // "starts with"
Foo // equals, basically one verticle will be matched
```

Example identifiers:
```
sisu-remote:org.cstamas.vertx:vertx-sisu-local:jar:tests:1.0.0-SNAPSHOT  // would match both verticle from tests
sisu-remote:org.cstamas.vertx:vertx-sisu-local:jar:tests:1.0.0-SNAPSHOT::ExampleNamed*  // matches ExampleNamedVerticle only
sisu-remote:org.cstamas.vertx:vertx-sisu-local:jar:tests:1.0.0-SNAPSHOT::*NamedVerticle // matches ExampleNamedVerticle only
sisu-remote:org.cstamas.vertx:vertx-sisu-local:jar:tests:1.0.0-SNAPSHOT::ExampleNamedVerticle // matches ExampleNamedVerticle only
```