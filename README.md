Legs.io
====

Scraping and job orchestration library.

[![Build Status](https://travis-ci.org/uniformlyrandom/legs.png)](https://travis-ci.org/uniformlyrandom/legs)


### Project goal

Eliminate the cruft, boilerplate and minimize time needed to write a scraper. Ideally only the business logic (moving parts) need to be written, all supporting machinery needs to be already in place and provide simple intuitive APIs

### Installation

Java 8 "Nashorn" is used as the map-reduce engine, you will need to have Java 8 installed to use `Legs`.

`Legs.io` is published to Sonatype, so first make sure `SBT` is checking Sonatype for jars

	resolvers += Resolver.sonatypeRepo("releases")

Adding the dependecy to `build.sbt`

	libraryDependencies += "io.legs" %% "legs" % "0.8.4+"


Currently I publish the project against Scala `2.11`, if `2.10.*` is required, please let me know

### Changelog

 * `0.8.1` add randomized user agent to PhantomJS
 * `0.8.2` improve logging + docs
 * `0.8.3.*` refactoring internals, adding `AS_JSON`, some work around queues
 * `0.8.4.*` experimental `transform` feature for every step of the chain

### Library contents

The library was originally built for scraping & crawling and requires additional functionality other then simply downloading pages. Some additional machinery is provided to support an easy and effective tool 

 - scraping (HTML,XML)
 - tracking visited pages
 - "live client" (selenium)
 - string operations (regex etc)
 - queue management / jobs
 - iterating collections 
 - persistence to file / remote 
 - map reduce operations (JavaScript)

*For queues to work, Redis needs to be installed and configured*

### Job instructions

The language to connect the different parts requires that:

 - minimal language friction
 - portable and sharable
 - future compatible
 - allow enough flexibility but still very simple and basic

I ended up using JSON as the storage medium, which seemed to fit the requirements nicely.

Job instructions are sequential and will only continue to the next block when the current finishes, it needs to have an `action` member with full path to specific function (if its one of the library provided functions, you can call the specific function name directly without specifying its path), parameter keys are then separated by slashes which will be looked up first in the `values` member and then in the current state

```json
[
	{
		"action" : "FETCH/url",
		"values" : {
			"url" : "https://news.google.com/"
		},
		"yields" : "s_google_news"
	},{
		"action" : "ECHO/s_google_news"
	}
]
```

 * it will resolve `FETCH` to `io.legs.specialized.Scraper.FETCH`
 * the engine will try to resolve the `url` parameter from the `values` first and then from the current "state" (inherited from its preceding sibling)
 * finally it will evaluate and if successful will assign a state value with the name `s_google_news` (the `s_` is just convention to denote  a string value)
 * the next instructions block will call the `io.legs.specialized.Tools.ECHO` with parameter by that name
 * you should be able to see the source HTML in your console now

Several build in functions are available (see the `Library functions` section below)

Library functions
===

To generate a fresh `FUNCTIONS.md` execute `sbt gendocs`

Latest automatically generated are [here](./FUNCTIONS.md)

