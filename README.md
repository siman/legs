Legs.io
====

scraping and job orchestration library

[![Build Status](https://travis-ci.org/uniformlyrandom/legs.png)](https://travis-ci.org/uniformlyrandom/legs)


### Project goal

Eliminate the cruft (boiler plate) and minimize time to completion of writing a scraper. Ideally only the business logic (moving parts) need to be written, all supporting machinery needs to be already in place and provide simple intuitive APIs

### Installation

`Legs.io` is published to Sonatype, so first make sure `SBT` is checking Sonatype for jars

	resolvers += Resolver.sonatypeRepo("releases")

Adding the dependecy to `build.sbt`

	libraryDependencies += "io.legs" %% "legs" % "0.8.1"


Currently I publish the project against Scala `2.11`, if `2.10.*` is required, please let me know

### Changelog

 * `0.8.1` add randomized user agent to PhantomJS
 * `0.8.2` improve logging + docs
 * `0.8.3` refactor signature of `RoutableFuture` to `Future[T]` instead of `Future[Try[T]]`

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
 * finally it will evaluate and if successful will assign a state value with the name `s_google_news` (the `s_` is just convention to denote a string value)
 * the next instructions block will call the `io.legs.specialized.Tools.ECHO` with parameter by that name
 * you should be able to see the source HTML in your console now

Several build in functions are available (see the `Library functions` section below)

Library functions
===

Scraping
----

##### `FETCH/x` - downloads and "cleans" HTML page content

|parameter|type|description|
|:--------|:--:|:----------|
|x|`String`|HTML page url|
|yields|`String`|the HTML source|

##### `FETCH_RAW/x` - Downloads any URL

|parameter|type|description|
|:--------|:--:|:----------|
|x|`String`|some URL|
|yields|`String`|the String value of that resource|


##### `EXTRACT_HTML_XPATH/x1/x2/x3` - run XPATH against some HTML string

|parameter|type|description|
|:--------|:--:|:----------|
|x1|`String`|some HTML string|
|x2|`String`|XPATH selector to run against the HTML|
|x3|`String`|will be supported in the future use `.*`|
|yields|`List[String]`|list of matched elements|

##### `CHECK_EXIST_CREATE/x1/x2` - check if `x2` value exist in `x1` name-space

|parameter|type|description|
|:--------|:--:|:----------|
|x1|`String`|the namespace|
|x2|`String`|specific value|
|yields|`Boolean`|true if it exists|
	
Active page scraping
----

##### `WD_VISIT/x` start a selenium phantom js headless driver session

|parameter|type|description|
|:--------|:--:|:----------|
|x|`String`|url to load into Selenium|
|yields|`PhantomJSDriver`|an instance of selenium phantom driver|

##### `WD_SELECT_DROPDOWN/x1/x2/x3` select item from dropdown

|parameter|type|description|
|:--------|:--:|:----------|
|x1|`PhantomJSDriver`|an instance of selenium phantom driver|
|x2|`String`|xpath selector for finding a select element|
|x3|`String`|the specific select value for selection|

##### `WD_CLICK/x1/x2` click on value found via selector

|parameter|type|description|
|:--------|:--:|:----------|
|x1|`PhantomJSDriver`|an instance of selenium phantom driver|
|x2|`String`|XPATH selector|

##### `WD_WAIT_UNTIL_SELECTOR/x1/x2` wait for selector to return something before continuing

|parameter|type|description|
|:--------|:--:|:----------|
|x1|`PhantomJSDriver`|an instance of selenium phantom driver|
|x2|`String`|XPATH selector|

##### `WD_GET_HTML/x` get HTML source of current document

|parameter|type|description|
|:--------|:--:|:----------|
|x|`PhantomJSDriver`|an instance of selenium phantom driver|
|yields|`String`|current document source|

String Operations
----

##### `REPLACE_REGEX/x2/x2/x3` replace value in string using regex

|parameter|type|description|
|:--------|:--:|:----------|
|x1|`String`|input string|
|x2|`String`|regex match expression|
|x3|`String`|regex replace expression, where `$1`,`$2` etc denote the matched groups|
|yields|`String`|the result of the replace|
	
Queue/Jobs Operations
----

##### `ADD_JOB/x1/x2/x3/x4` add a job to the queue

|parameter|type|description|
|:--------|:--:|:----------|
|x1|`String`|instructions file name|
|x2|`String`|job description|
|x3|`JsArray[String]`|labels associated with job (`["label1","label2"]`)|
|x4|`JsArray[String]`|list of parameters to be passed as input to job|

Misc
----

##### `MAP_PAR/x1/x2/x3` iterate over a collection

|parameter|type|description|
|:--------|:--:|:----------|
|x1|`List`|input collection|
|x2|`String`|parameter name|
|x3|`JsArray`|further steps to be executed|
|yields|`List[Any]`|list of yields inside the map|

##### `IF/x1/x2/x3` if else block

|parameter|type|description|
|:--------|:--:|:----------|
|x1|`Boolean`|true or false|
|x2|`JsArray`|instructions for true condition|
|x3|`JsArray`|instructions for false condition|
|yields|`Any`|the last nested expression yield is forwarded|

##### `TO_FILE/x1/x2` persist values file

|parameter|type|description|
|:--------|:--:|:----------|
|x1|`JsArray[String]`|array of keys of values to persist (simple concat of values)|
|x2|`String`|path for where to store the file|


##### `VERIFY_VALUES/x1` verify values exist in the state (by name), fail job if not found

|parameter|type|description|
|:--------|:--:|:----------|
|x1|`JsArray[String]`|name of parameters to check for existence in the state|

Map-Reduce
----

##### `MAP_REDUCE/x1/x2/x3` mongo style map reduce, executed by JVM-8's "Nashorn" JavaScript engine

|parameter|type|description|
|:--------|:--:|:----------|
|x1|`List`|a collection of some values to map on|
|x2|`String`|JSON escaped Javascript snippet with a `map` function which calls `emitter.emit(key,value`|
|x2|`String`|JSON escaped Javascript snippet with a `reduce` function which reduces values per every key in the map stage|
|yields|`Map[String,Any]`|result of map and reduce|

##### `GET_MAP_KEY/x1/x2` retrieve a key from a map

|parameter|type|description|
|:--------|:--:|:----------|
|x1|`Map`|some map|
|x2|`String`|map key to get|
|yields|`Any`|the value for that key|

