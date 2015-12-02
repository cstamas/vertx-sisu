# Vert.x Eclipse Sisu Extension

Adds [Eclipse Sisu](https://www.eclipse.org/sisu/) enabled DI to Vert.x.

## `vertx-sisu-local` module

Heavily inspired by Vert.x `vertx-service-factory`. Provides following prefixes:

* `sisu` - for loading up single verticle by name or FQCN
* `sisu-local` - for bootstrapping a complete set of verticles

### `sisu` prefix

The `sisu` prefix uses following syntax:

```
sisu:verticleName
```

where

```
verticleName := <fully qualified class name> | <name as applied by @Named>
```

Usable when Verticle you want to deploy is located in same classloader as your caller is.

Examples:

```
sisu:org.cstamas.vertx.sisu.examples.ExampleNamedVerticle
sisu:myExample // if implementation has applied @Named("myExample")
```

### `sisu-local` prefix

The `sisu-local` prefix currently uses following syntax:

```
sisu-local:bootstrap[::filter]
```
where the "bootstrap" part is required and optional filter currently supports following modes
```
filter := <not present> | <term + '*'> | <'*' + term> | <term>
```
Meanings of those above are "all", "begins with term", "ends with term" and "equals with term" consecutively.

Usable when you want to "bootstrap" a set of verticles from same "class space" (sisu term, basically all Verticles
discovered by sisu in given class loader).

Examples:
```
sisu-local:bootstrap // all
sisu-local:bootstrap::ExampleNamed* // begins with 'ExampleNamed'
sisu-local:bootstrap::*NamedVerticle // ends with 'NamedVerticle'
sisu-local:bootstrap::ExampleNamedVerticle // equals with 'ExampleNamedVerticle'
```

## `vertx-sisu-remote` module

Heavily inspired by Vert.x `vertx-maven-service-factory`. Provides following prefixes:

* `sisu-remote` - for downloading and bootstrapping a complete set of verticles from deployed module

### `sisu-remote` prefix

The `sisu-remote` prefix currently uses following syntax:

```
sisu-local:artifactCoordinate[::filter]
```
Where `artifactCoordinate` is the usual Maven artifact "one liner":

```
artifactCoordinate := groupId:artifactId[:extension[:classifier]]:version
```
and optional filter currently supports same modes as `sisu-local` filter.

Usable when you want to "bootstrap" a set of verticles from module downloaded from Maven2 repository.

Examples:
```
sisu-remote:org.cstamas.vertx:vertx-sisu-example:1.0.0
sisu-remote:org.cstamas.vertx:vertx-sisu-example:1.0.0::*NamedVerticle
sisu-remote:org.cstamas.vertx:vertx-sisu-example:1.0.0::ExampleNamed*
sisu-remote:org.cstamas.vertx:vertx-sisu-example:1.0.0::ExampleNamedVerticle
```

Improvements compared to `vertx-maven-service-factory`:
* no need for embedded module descriptor, sisu will discover named components
* uses Takari concurrent local repository (multiple processes may share same local repository)
* uses OkHttp HTTP/2 enabled transport

# Using it on application side (where module is consumed)

To use any of these with Vert.x, all you need is add the dependency like this and start using the prefixes to deploy verticles:

```
    <dependency>
      <groupId>org.cstamas.vertx</groupId>
      <artifactId>vertx-sisu-remote</artifactId>
      <version>1.0.0</version>
    </dependency>
```

and then in some of your code just do

```
vertx.deployVerticle("sisu-remote:org.cstamas.vertx:vertx-sisu-example:1.0.0");
```

# Producing Sisu-enabled modules

You need to take care of following:
* annotate your Verticles with javax.inject:javax.inject `@Named` annotation (in this case their "name" will be FQCN).
You might want to add specific names, in that case use `@Named("myName")`. Do NOT mark your Verticle implementations
with `@Singleton`, as that might result in unexpected situation for Vert.x itself.
* make sure sisu index is generated for your JAR. You can have that either by using org.eclipse.sisu:sisu-maven-plugin
maven plugin, or, by having org.eclipse.sisu:org.eclipse.sisu.inject dependency on compile classpath (latter is not quite
usual, as sisu is more a runtime dependency as compile time)

# Building it

For simplicity sake, tests in the build will use the `example` module. This means that on _very first build_ you need
to _install the module_, so simplest is to do this after initial checkout:

```
$ mvn clean install
```

And subsequent executions will just work.