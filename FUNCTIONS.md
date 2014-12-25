Legs.io - Functions Library
====

Below is an auto generated list of supported library functions in no particular order

Methods
------
* `CHECK_EXIST_CREATE/domain : String/uri : String` : `scala.Boolean` [details](#CHECK_EXIST_CREATE)
* `JSONPATH/json : String/jsonPath : String` : `immutable.this.List.empty[String]` [details](#JSONPATH)
* `GENERATE/start : BigDecimal/end : BigDecimal` : `immutable.this.List.empty[scala.Int]` [details](#GENERATE)
* `TO_FILE_AS_JSON/keys : List/filePath : String` : `scala.None` [details](#TO_FILE_AS_JSON)
* `TO_FILE/keys : List/filePath : String` : `scala.None` [details](#TO_FILE)
* `QUEUE_ALL/` : `scala.None` [details](#QUEUE_ALL)
* `QUEUE/jobId : String` : `scala.None` [details](#QUEUE)
* `PLAN/schedule : String/jobId : String` : `scala.None` [details](#PLAN)
* `ADD_JOB/instructions : String/description : String/labels : List/inputIndices : List` : `"String"` [details](#ADD_JOB)
* `EXTRACT_HTML_XPATH_FIRST/inputString : String/selector : String/validator : String` : `"String"` [details](#EXTRACT_HTML_XPATH_FIRST)
* `EXTRACT_HTML_XPATH/inputString : String/selector : String/validator : String` : `immutable.this.List.empty[String]` [details](#EXTRACT_HTML_XPATH)
* `EXTRACT_XML_XPATH/inputString : String/selector : String/validator : String` : `immutable.this.List.empty[String]` [details](#EXTRACT_XML_XPATH)
* `EXTRACT_JSOUP/inputString : String/selector : String/validator : String` : `immutable.this.List.empty[String]` [details](#EXTRACT_JSOUP)
* `FETCH_RAW/url : String` : `"String"` [details](#FETCH_RAW)
* `FETCH/url : String` : `"String"` [details](#FETCH)
* `TRIM/input : String` : `"String"` [details](#TRIM)
* `SPLIT/input : String/splitBy : String` : `immutable.this.List.empty[String]` [details](#SPLIT)
* `REPLACE_REGEX/input : String/matchRegex : String/replaceExp : String` : `"String"` [details](#REPLACE_REGEX)
* `EXTRACT_REGEX/input : String/regex : String` : `"String"` [details](#EXTRACT_REGEX)
* `AS_JSON/keys : List` : `"String"` [details](#AS_JSON)
* `VERIFY_VALUES/keys : List` : `scala.None` [details](#VERIFY_VALUES)
* `ECHO/value : Any` : `scala.`package`.AnyRef` [details](#ECHO)
* `LOOP_WHILE/checkInstructions : JsArray/overInstructions : JsArray` : `scala.None` [details](#LOOP_WHILE)
* `GET_MAP_KEY/map : Map/key : String` : `scala.`package`.AnyRef` [details](#GET_MAP_KEY)
* `MAP_PAR/inputList : List/toValueName : String/furtherInstructions : JsArray` : `immutable.this.List.empty[scala.Any]` [details](#MAP_PAR)
* `IF/value : Any/trueInstructions : JsArray/falseInstructions : JsArray` : `scala.`package`.AnyRef` [details](#IF)
* `IS_STRING_DIFFERENT/left : Any/right : Any` : `scala.Boolean` [details](#IS_STRING_DIFFERENT)
* `IS_STRINGS_EQUAL/left : Any/right : Any` : `scala.Boolean` [details](#IS_STRINGS_EQUAL)
* `DEBUG/` : `scala.None` [details](#DEBUG)
* `CAST/input : Any/toType : String` : `"Int/String"` [details](#CAST)
* `WD_CLOSE/driver : Any` : `scala.None` [details](#WD_CLOSE)
* `WD_GET_HTML/driver : Any` : `"String"` [details](#WD_GET_HTML)
* `WD_XPATH_CHECK/driver : Any/xpath : String` : `scala.Boolean` [details](#WD_XPATH_CHECK)
* `WD_WAIT_UNTIL_SELECTOR/driver : Any/xpath : String` : `scala.None` [details](#WD_WAIT_UNTIL_SELECTOR)
* `WD_CLICK/driver : Any/xpath : String` : `scala.None` [details](#WD_CLICK)
* `WD_SELECT_DROPDOWN/driver : Any/xpath : String/elementValue : String` : `scala.None` [details](#WD_SELECT_DROPDOWN)
* `WD_VISIT/url : String` : `"WebDriver"` [details](#WD_VISIT)
* `MAP_REDUCE/collection : List/map : String/reduce : String` : `scala.this.Predef.Map.empty[String, scala.Any]` [details](#MAP_REDUCE)


Details
-----
#### <a name="CHECK_EXIST_CREATE"></a>`CHECK_EXIST_CREATE/domain : String/uri : String` : `scala.Boolean`
check for existing UUID or create one if not previously existed
Yields : `scala.Boolean` - returns true if already exists, false if just created

 * domain : String - the prefix (or domain) to be used for this resource
 * uri : String - the resource URI (UID)

#### <a name="JSONPATH"></a>`JSONPATH/json : String/jsonPath : String` : `immutable.this.List.empty[String]`
basically implementing https://github.com/gatling/jsonpath. Examples: https://github.com/gatling/jsonpath/blob/master/src/test/scala/io/gatling/jsonpath/JsonPathSpec.scala
Yields : `immutable.this.List.empty[String]` - returns list of matching evaluated XPATH expression

 * json : String - json string value
 * jsonPath : String - json path expression

#### <a name="GENERATE"></a>`GENERATE/start : BigDecimal/end : BigDecimal` : `immutable.this.List.empty[scala.Int]`
generated a list of numbers
Yields : `immutable.this.List.empty[scala.Int]` - list of numbers

 * start : BigDecimal - start value
 * end : BigDecimal - end value

#### <a name="TO_FILE_AS_JSON"></a>`TO_FILE_AS_JSON/keys : List/filePath : String` : `scala.None`
persist state key contents to file as a json object
Yields : `scala.None` - nothing is yielded

 * keys : List - list of keys to be extracted from state to be serialized
 * filePath : String - the full file path to be used for persisting the contents

#### <a name="TO_FILE"></a>`TO_FILE/keys : List/filePath : String` : `scala.None`
persist content to file
Yields : `scala.None` - nothing is yielded

 * keys : List - list of keys to be extracted from state to be serialized
 * filePath : String - the full file path to be used for persisting the contents

#### <a name="QUEUE_ALL"></a>`QUEUE_ALL/` : `scala.None`
queue all scheduled jobs according to their schedule
Yields : `scala.None` -

#### <a name="QUEUE"></a>`QUEUE/jobId : String` : `scala.None`
queue a scheduled job for execution
Yields : `scala.None` - 

 * jobId : String - the job ID to be queued according to its schedule plan

#### <a name="PLAN"></a>`PLAN/schedule : String/jobId : String` : `scala.None`
create a schedule plan for a job
Yields : `scala.None` - nothing is returned

 * schedule : String - the schedule, in cron format
 * jobId : String - job id to be used

#### <a name="ADD_JOB"></a>`ADD_JOB/instructions : String/description : String/labels : List/inputIndices : List` : `"String"`
Add a job to the FIFO queue
Yields : `"String"` - new job id is yielded

 * instructions : String - name of instructions file
 * description : String - job description
 * labels : List - list of labels to be used to target specific queues
 * inputIndices : List - values to be taken from the state as input for the new job

#### <a name="EXTRACT_HTML_XPATH_FIRST"></a>`EXTRACT_HTML_XPATH_FIRST/inputString : String/selector : String/validator : String` : `"String"`
execute XPATH expression over valid HTML formatted string
Yields : `"String"` - single matching value as String

 * inputString : String - valid HTML
 * selector : String - XPATH expression
 * validator : String - REGEX result validation

#### <a name="EXTRACT_HTML_XPATH"></a>`EXTRACT_HTML_XPATH/inputString : String/selector : String/validator : String` : `immutable.this.List.empty[String]`
execute XPATH expression over valid HTML formatted string
Yields : `immutable.this.List.empty[String]` - list of matching values

 * inputString : String - valid HTML
 * selector : String - XPATH expression
 * validator : String - REGEX result validation

#### <a name="EXTRACT_XML_XPATH"></a>`EXTRACT_XML_XPATH/inputString : String/selector : String/validator : String` : `immutable.this.List.empty[String]`
extract values from correctly structured XML input value using XPATH selector
Yields : `immutable.this.List.empty[String]` - resulting list of values

 * inputString : String - valid XML
 * selector : String - XPATH expression
 * validator : String - REGEX result validation

#### <a name="EXTRACT_JSOUP"></a>`EXTRACT_JSOUP/inputString : String/selector : String/validator : String` : `immutable.this.List.empty[String]`
Uses Jsoup selector syntax for extraction of values from structured HTML/XMLdocs - docs - http://jsoup.org/cookbook/extracting-data/selector-syntaxplayground - playground - http://try.jsoup.org/
Yields : `immutable.this.List.empty[String]` - list of matching values

 * inputString : String - input HTML/XML as String
 * selector : String - JSOUP style selector
 * validator : String - a validation REGEX

#### <a name="FETCH_RAW"></a>`FETCH_RAW/url : String` : `"String"`
fetch raw resource
Yields : `"String"` - string value of fetched resource

 * url : String - web url

#### <a name="FETCH"></a>`FETCH/url : String` : `"String"`
fetch HTML web resource, while fixing the underlying HTML then turn to String
Yields : `"String"` - returns the resource as string

 * url : String - web url

#### <a name="TRIM"></a>`TRIM/input : String` : `"String"`
trim some input string
Yields : `"String"` - string without white space characters around it

 * input : String - input string

#### <a name="SPLIT"></a>`SPLIT/input : String/splitBy : String` : `immutable.this.List.empty[String]`
split input value into list of strings
Yields : `immutable.this.List.empty[String]` - list of resulting values after the split

 * input : String - input string
 * splitBy : String - character to split by

#### <a name="REPLACE_REGEX"></a>`REPLACE_REGEX/input : String/matchRegex : String/replaceExp : String` : `"String"`
evaluate a regex expression over an input string, matching groups would be $1,$2.. etc in the replace 
Yields : `"String"` - extracted value

 * input : String - input string
 * matchRegex : String - REGEX pattern for matching in groups $1, $2 etc..
 * replaceExp : String - REGEX replace pattern, use $1,$2 as place holders

#### <a name="EXTRACT_REGEX"></a>`EXTRACT_REGEX/input : String/regex : String` : `"String"`
extract value from input string using Regex
Yields : `"String"` - extracted value

 * input : String - input string
 * regex : String - REGEX pattern to match against for extraction

#### <a name="AS_JSON"></a>`AS_JSON/keys : List` : `"String"`
take state values and put them into a JSON map
Yields : `"String"` - string representation of JSON value put togather

 * keys : List - list of keys to take from state

#### <a name="VERIFY_VALUES"></a>`VERIFY_VALUES/keys : List` : `scala.None`
check that all provided keys are defined in state, otherwise fail the job
Yields : `scala.None` - nothing is yielded

 * keys : List - list of keys to check for

#### <a name="ECHO"></a>`ECHO/value : Any` : `scala.`package`.AnyRef`
yield and print (STDOUT) some given value
Yields : `scala.`package`.AnyRef` - state value

 * value : Any - some provided value

#### <a name="LOOP_WHILE"></a>`LOOP_WHILE/checkInstructions : JsArray/overInstructions : JsArray` : `scala.None`
loop while given instructions yield true
Yields : `scala.None` - nothing is yielded

 * checkInstructions : JsArray - instructions to evaluate for stop condition - should yield boolean
 * overInstructions : JsArray - perform instructions inside the while loop if the checkInstructions yielded true

#### <a name="GET_MAP_KEY"></a>`GET_MAP_KEY/map : Map/key : String` : `scala.`package`.AnyRef`
get a single entry form a map by given key
Yields : `scala.`package`.AnyRef` - value produced by key from the map

 * map : Map - a map data structure to query
 * key : String - key to use for querying the map

#### <a name="MAP_PAR"></a>`MAP_PAR/inputList : List/toValueName : String/furtherInstructions : JsArray` : `immutable.this.List.empty[scala.Any]`
iterate over a collection of values and produce a new resulting list from applying further instructions on each input vlaue
Yields : `immutable.this.List.empty[scala.Any]` - list of resulting transformed values

 * inputList : List - input list of values
 * toValueName : String - each item is assigned this name when iterating
 * furtherInstructions : JsArray - instructions set to use when manipulating a single value

#### <a name="IF"></a>`IF/value : Any/trueInstructions : JsArray/falseInstructions : JsArray` : `scala.`package`.AnyRef`
evaluates input value as boolean string true or 1, otherwise false. executes relevant block 
Yields : `scala.`package`.AnyRef` - what ever the evaluated block is returning

 * value : Any - Boolean input value
 * trueInstructions : JsArray - instrucitons block to be executed when true
 * falseInstructions : JsArray - instrucitons block to be executed when false

#### <a name="IS_STRING_DIFFERENT"></a>`IS_STRING_DIFFERENT/left : Any/right : Any` : `scala.Boolean`
check if two inputs stringified are different
Yields : `scala.Boolean` - true if different, false otherwise

 * left : Any - left value
 * right : Any - right value

#### <a name="IS_STRINGS_EQUAL"></a>`IS_STRINGS_EQUAL/left : Any/right : Any` : `scala.Boolean`
check if two inputs stringified are equal
Yields : `scala.Boolean` - true of they are, false otherwise

 * left : Any - left value
 * right : Any - right value

#### <a name="DEBUG"></a>`DEBUG/` : `scala.None`
helper function to debug (print to STDOUT) the state
Yields : `scala.None` - nothing is yielded

#### <a name="CAST"></a>`CAST/input : Any/toType : String` : `"Int/String"`
convert given input value to Int/String
Yields : `"Int/String"` - the type as provided

 * input : Any - either String/Int
 * toType : String - can be either Int/String

#### <a name="WD_CLOSE"></a>`WD_CLOSE/driver : Any` : `scala.None`
shutdown and cleanup WebDriver instance
Yields : `scala.None` - nothing is yielded

 * driver : Any - WebDriver instance

#### <a name="WD_GET_HTML"></a>`WD_GET_HTML/driver : Any` : `"String"`
get current HTML value of given WebDriver instance
Yields : `"String"` - string value of HTML

 * driver : Any - WebDriver instance

#### <a name="WD_XPATH_CHECK"></a>`WD_XPATH_CHECK/driver : Any/xpath : String` : `scala.Boolean`
execute XPATH and return true if something is returned
Yields : `scala.Boolean` - true if found something, false otherwise

 * driver : Any - instance of WebDriver
 * xpath : String - XPATH

#### <a name="WD_WAIT_UNTIL_SELECTOR"></a>`WD_WAIT_UNTIL_SELECTOR/driver : Any/xpath : String` : `scala.None`
wait until XPATH returns vlaue inside a live page
Yields : `scala.None` - nothing is yielded

 * driver : Any - instance of WebDriver
 * xpath : String - XPATH

#### <a name="WD_CLICK"></a>`WD_CLICK/driver : Any/xpath : String` : `scala.None`
click on item
Yields : `scala.None` - nothing is yielded

 * driver : Any - instance of WebDriver
 * xpath : String - XPATH

#### <a name="WD_SELECT_DROPDOWN"></a>`WD_SELECT_DROPDOWN/driver : Any/xpath : String/elementValue : String` : `scala.None`
select value from dropdown
Yields : `scala.None` - nothing is yielded

 * driver : Any - instance of a WebDriver
 * xpath : String - XPATH
 * elementValue : String - element value to select from the dropdown items

#### <a name="WD_VISIT"></a>`WD_VISIT/url : String` : `"WebDriver"`
start a WebDriver session 
Yields : `"WebDriver"` - a WebDriver instance

 * url : String - web url

#### <a name="MAP_REDUCE"></a>`MAP_REDUCE/collection : List/map : String/reduce : String` : `scala.this.Predef.Map.empty[String, scala.Any]`
execute a JavaScript Map and Reduce functions over input collection
Yields : `scala.this.Predef.Map.empty[String, scala.Any]` - indices and values after map reduce

 * collection : List - list of input values
 * map : String - needs to contain \"function map(item, collection,emitter) {...} \" call `emitter.emit(key,valie)` to emit values
 * reduce : String - needs to contain \"function reduce(key, values){ ... }\" returned value is reduces to the resulting map for that key