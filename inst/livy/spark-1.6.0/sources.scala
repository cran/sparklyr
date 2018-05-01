//
// This file was automatically generated using livy_sources_refresh()
// Changes to this file will be reverted.
//

class Sources {
  def sources: String = """
#' A helper function to retrieve values from \code{spark_config()}
#'
#' @param config The configuration list from \code{spark_config()}
#' @param name The name of the configuration entry
#' @param default The default value to use when entry is not present
#'
#' @keywords internal
#' @export
spark_config_value <- function(config, name, default = NULL) {
  if (!name %in% names(config)) default else config[[name]]
}
#' Check whether the connection is open
#'
#' @param sc \code{spark_connection}
#'
#' @keywords internal
#'
#' @export
connection_is_open <- function(sc) {
  UseMethod("connection_is_open")
}
readObject <- function(con) {
  # Read type first
  type <- readType(con)
  readTypedObject(con, type)
}

readTypedObject <- function(con, type) {
  switch (type,
          "i" = readInt(con),
          "c" = readString(con),
          "b" = readBoolean(con),
          "d" = readDouble(con),
          "r" = readRaw(con),
          "D" = readDate(con),
          "t" = readTime(con),
          "a" = readArray(con),
          "l" = readList(con),
          "e" = readEnv(con),
          "s" = readStruct(con),
          "n" = NULL,
          "j" = getJobj(con, readString(con)),
          stop(paste("Unsupported type for deserialization", type)))
}

readString <- function(con) {
  stringLen <- readInt(con)
  raw <- readBin(con, raw(), stringLen, endian = "big")
  string <- rawToChar(raw)
  Encoding(string) <- "UTF-8"
  string
}

readDateArray <- function(con, n = 1) {
  r <- readTime(con, n)
  if (getOption("sparklyr.collect.datechars", FALSE)) r else as.Date(r)
}

readInt <- function(con, n = 1) {
  readBin(con, integer(), n = n, endian = "big")
}

readDouble <- function(con, n = 1) {
  readBin(con, double(), n = n, endian = "big")
}

readBoolean <- function(con, n = 1) {
  as.logical(readInt(con, n = n))
}

readType <- function(con) {
  rawToChar(readBin(con, "raw", n = 1L))
}

readDate <- function(con) {
  as.Date(readString(con))
}

readTime <- function(con, n = 1) {
  t <- readDouble(con, n)
  timeNA <- as.POSIXct(0, origin = "1970-01-01", tz = "UTC")

  r <- as.POSIXct(t, origin = "1970-01-01", tz = "UTC")
  if (getOption("sparklyr.collect.datechars", FALSE)) as.character(r) else {
    r[r == timeNA] <- as.POSIXct(NA)
    r
  }
}

readArray <- function(con) {
  type <- readType(con)
  len <- readInt(con)

  if (type == "d") {
    return(readDouble(con, n = len))
  } else if (type == "i") {
    return(readInt(con, n = len))
  } else if (type == "b") {
    return(readBoolean(con, n = len))
  } else if (type == "t") {
    return(readTime(con, n = len))
  } else if (type == "D") {
    return(readDateArray(con, n = len))
  }

  if (len > 0) {
    l <- vector("list", len)
    for (i in 1:len) {
      l[[i]] <- readTypedObject(con, type)
    }
    l
  } else {
    list()
  }
}

# Read a list. Types of each element may be different.
# Null objects are read as NA.
readList <- function(con) {
  len <- readInt(con)
  if (len > 0) {
    l <- vector("list", len)
    for (i in 1:len) {
      elem <- readObject(con)
      if (is.null(elem)) {
        elem <- NA
      }
      l[[i]] <- elem
    }
    l
  } else {
    list()
  }
}

readEnv <- function(con) {
  env <- new.env()
  len <- readInt(con)
  if (len > 0) {
    for (i in 1:len) {
      key <- readString(con)
      value <- readObject(con)
      env[[key]] <- value
    }
  }
  env
}

# Convert a named list to struct so that
# SerDe won't confuse between a normal named list and struct
listToStruct <- function(list) {
  stopifnot(class(list) == "list")
  stopifnot(!is.null(names(list)))
  class(list) <- "struct"
  list
}

# Read a field of StructType from DataFrame
# into a named list in R whose class is "struct"
readStruct <- function(con) {
  names <- readObject(con)
  fields <- readObject(con)
  names(fields) <- names
  listToStruct(fields)
}

readRaw <- function(con) {
  dataLen <- readInt(con)
  readBin(con, raw(), as.integer(dataLen), endian = "big")
}
wait_connect_gateway <- function(gatewayAddress, gatewayPort, config, isStarting) {
  waitSeconds <- if (isStarting)
    spark_config_value(config, "sparklyr.gateway.start.timeout", 60)
  else
    spark_config_value(config, "sparklyr.gateway.connect.timeout", 1)

  gateway <- NULL
  commandStart <- Sys.time()

  while (is.null(gateway) && Sys.time() < commandStart + waitSeconds) {
    tryCatch({
      suppressWarnings({
        timeout <- spark_config_value(config, "sparklyr.monitor.timeout", 1)
        gateway <- socketConnection(host = gatewayAddress,
                                    port = gatewayPort,
                                    server = FALSE,
                                    blocking = TRUE,
                                    open = "rb",
                                    timeout = timeout)
      })
    }, error = function(err) {
    })

    startWait <- spark_config_value(config, "sparklyr.gateway.start.wait", 50 / 1000)
    Sys.sleep(startWait)
  }

  gateway
}

spark_gateway_commands <- function() {
  list(
    "GetPorts" = 0,
    "RegisterInstance" = 1
  )
}

query_gateway_for_port <- function(gateway, sessionId, config, isStarting) {
  waitSeconds <- if (isStarting)
    spark_config_value(config, "sparklyr.gateway.start.timeout", 60)
  else
    spark_config_value(config, "sparklyr.gateway.connect.timeout", 1)

  writeInt(gateway, spark_gateway_commands()[["GetPorts"]])
  writeInt(gateway, sessionId)
  writeInt(gateway, if (isStarting) waitSeconds else 0)

  backendSessionId <- NULL
  redirectGatewayPort <- NULL

  commandStart <- Sys.time()
  while(length(backendSessionId) == 0 && commandStart + waitSeconds > Sys.time()) {
    backendSessionId <- readInt(gateway)
    Sys.sleep(0.1)
  }

  redirectGatewayPort <- readInt(gateway)
  backendPort <- readInt(gateway)

  if (length(backendSessionId) == 0 || length(redirectGatewayPort) == 0 || length(backendPort) == 0) {
    if (isStarting)
      stop("Sparklyr gateway did not respond while retrieving ports information after ", waitSeconds, " seconds")
    else
      return(NULL)
  }

  list(
    gateway = gateway,
    backendPort = backendPort,
    redirectGatewayPort = redirectGatewayPort
  )
}

spark_connect_gateway <- function(
  gatewayAddress,
  gatewayPort,
  sessionId,
  config,
  isStarting = FALSE) {

  # try connecting to existing gateway
  gateway <- wait_connect_gateway(gatewayAddress, gatewayPort, config, isStarting)

  if (is.null(gateway)) {
    if (isStarting)
      stop(
        "Gateway in port (", gatewayPort, ") did not respond.")

    NULL
  }
  else {
    worker_log("is querying ports from backend using port ", gatewayPort)

    gatewayPortsQuery <- query_gateway_for_port(gateway, sessionId, config, isStarting)
    if (is.null(gatewayPortsQuery) && !isStarting) {
      close(gateway)
      return(NULL)
    }

    redirectGatewayPort <- gatewayPortsQuery$redirectGatewayPort
    backendPort <- gatewayPortsQuery$backendPort

    worker_log("found redirect gateway port ", redirectGatewayPort)

    if (redirectGatewayPort == 0) {
      close(gateway)

      if (isStarting)
        stop("Gateway in port (", gatewayPort, ") does not have the requested session registered")

      NULL
    } else if(redirectGatewayPort != gatewayPort) {
      close(gateway)

      spark_connect_gateway(gatewayAddress, redirectGatewayPort, sessionId, config, isStarting)
    }
    else {
      list(
        gateway = gateway,
        backendPort = backendPort
      )
    }
  }
}
core_invoke_method <- function(sc, static, object, method, ...)
{
  if (is.null(sc)) {
    stop("The connection is no longer valid.")
  }

  # if the object is a jobj then get it's id
  if (inherits(object, "spark_jobj"))
    object <- object$id

  rc <- rawConnection(raw(), "r+")
  writeString(rc, object)
  writeBoolean(rc, static)
  writeString(rc, method)

  args <- list(...)
  writeInt(rc, length(args))
  writeArgs(rc, args)
  bytes <- rawConnectionValue(rc)
  close(rc)

  rc <- rawConnection(raw(0), "r+")
  writeInt(rc, length(bytes))
  writeBin(bytes, rc)
  con <- rawConnectionValue(rc)
  close(rc)

  backend <- sc$backend
  writeBin(con, backend)

  if (identical(object, "Handler") &&
      (identical(method, "terminateBackend") || identical(method, "stopBackend"))) {
    # by the time we read response, backend might be already down.
    return(NULL)
  }

  returnStatus <- readInt(backend)

  if (length(returnStatus) == 0) {
    # read the spark log
    msg <- core_read_spark_log_error(sc)
    close(sc$backend)
    close(sc$monitor)
    withr::with_options(list(
      warning.length = 8000
    ), {
      stop(
        "Unexpected state in sparklyr backend, terminating connection: ",
        msg,
        call. = FALSE)
    })
  }

  if (returnStatus != 0) {
    # get error message from backend and report to R
    msg <- readString(backend)
    withr::with_options(list(
      warning.length = 8000
    ), {
      if (nzchar(msg)) {
        core_handle_known_errors(sc, msg)

        stop(msg, call. = FALSE)
      } else {
        # read the spark log
        msg <- core_read_spark_log_error(sc)
        stop(msg, call. = FALSE)
      }
    })
  }

  class(backend) <- c(class(backend), "shell_backend")

  object <- readObject(backend)
  attach_connection(object, sc)
}

jobj_subclass.shell_backend <- function(con) {
  "shell_jobj"
}

core_handle_known_errors <- function(sc, msg) {
  # Some systems might have an invalid hostname that Spark <= 2.0.1 fails to handle
  # gracefully and triggers unexpected errors such as #532. Under these versions,
  # we proactevely test getLocalHost() to warn users of this problem.
  if (grepl("ServiceConfigurationError.*tachyon", msg, ignore.case = TRUE)) {
    warning(
      "Failed to retrieve localhost, please validate that the hostname is correctly mapped. ",
      "Consider running `hostname` and adding that entry to your `/etc/hosts` file."
    )
  }
  else if (grepl("check worker logs for details", msg, ignore.case = TRUE) &&
           spark_master_is_local(sc$master)) {
    abort_shell(
      "sparklyr worker rscript failure, check worker logs for details",
      NULL, NULL, sc$output_file, sc$error_file)
  }
}

core_read_spark_log_error <- function(sc) {
  # if there was no error message reported, then
  # return information from the Spark logs. return
  # all those with most recent timestamp
  msg <- "failed to invoke spark command (unknown reason)"
  try(silent = TRUE, {
    log <- readLines(sc$output_file)
    splat <- strsplit(log, "\\s+", perl = TRUE)
    n <- length(splat)
    timestamp <- splat[[n]][[2]]
    regex <- paste("\\b", timestamp, "\\b", sep = "")
    entries <- grep(regex, log, perl = TRUE, value = TRUE)
    pasted <- paste(entries, collapse = "\n")
    msg <- paste("failed to invoke spark command", pasted, sep = "\n")
  })
  msg
}
#' Retrieve a Spark JVM Object Reference
#'
#' This S3 generic is used for accessing the underlying Java Virtual Machine
#' (JVM) Spark objects associated with \R objects. These objects act as
#' references to Spark objects living in the JVM. Methods on these objects
#' can be called with the \code{\link{invoke}} family of functions.
#'
#' @param x An \R object containing, or wrapping, a \code{spark_jobj}.
#' @param ... Optional arguments; currently unused.
#'
#' @seealso \code{\link{invoke}}, for calling methods on Java object references.
#'
#' @exportClass spark_jobj
#' @export
spark_jobj <- function(x, ...) {
  UseMethod("spark_jobj")
}


#' @export
spark_jobj.default <- function(x, ...) {
  stop("Unable to retrieve a spark_jobj from object of class ",
       paste(class(x), collapse = " "), call. = FALSE)
}

#' @export
spark_jobj.spark_jobj <- function(x, ...) {
  x
}

#' @export
print.spark_jobj <- function(x, ...) {
  print_jobj(spark_connection(x), x, ...)
}

#' Generic method for print jobj for a connection type
#'
#' @param sc \code{spark_connection} (used for type dispatch)
#' @param jobj Object to print
#'
#' @keywords internal
#'
#' @export
print_jobj <- function(sc, jobj, ...) {
  UseMethod("print_jobj")
}


# Maintain a reference count of Java object references
# This allows us to GC the java object when it is safe
.validJobjs <- new.env(parent = emptyenv())

# List of object ids to be removed
.toRemoveJobjs <- new.env(parent = emptyenv())

# Check if jobj was created with the current SparkContext
isValidJobj <- function(jobj) {
  TRUE
}

getJobj <- function(con, objId) {
  newObj <- jobj_create(con, objId)
  if (exists(objId, .validJobjs)) {
    .validJobjs[[objId]] <- .validJobjs[[objId]] + 1
  } else {
    .validJobjs[[objId]] <- 1
  }
  newObj
}

jobj_subclass <- function(con) {
  UseMethod("jobj_subclass")
}

# Handler for a java object that exists on the backend.
jobj_create <- function(con, objId) {
  if (!is.character(objId)) {
    stop("object id must be a character")
  }
  # NOTE: We need a new env for a jobj as we can only register
  # finalizers for environments or external references pointers.
  obj <- structure(new.env(parent = emptyenv()), class = c("spark_jobj", jobj_subclass(con)))
  obj$id <- objId

  # Register a finalizer to remove the Java object when this reference
  # is garbage collected in R
  reg.finalizer(obj, cleanup.jobj)
  obj
}

jobj_info <- function(jobj) {
  if (!inherits(jobj, "spark_jobj"))
    stop("'jobj_info' called on non-jobj")

  class <- NULL
  repr <- NULL

  tryCatch({
    class <- invoke(jobj, "getClass")
    if (inherits(class, "spark_jobj"))
      class <- invoke(class, "getName")
  }, error = function(e) {
  })
  tryCatch({
    repr <- invoke(jobj, "toString")
  }, error = function(e) {
  })
  list(
    class = class,
    repr  = repr
  )
}

jobj_inspect <- function(jobj) {
  print(jobj)
  if (!connection_is_open(spark_connection(jobj)))
    return(jobj)

  class <- invoke(jobj, "getClass")

  cat("Fields:\n")
  fields <- invoke(class, "getDeclaredFields")
  lapply(fields, function(field) { print(field) })

  cat("Methods:\n")
  methods <- invoke(class, "getDeclaredMethods")
  lapply(methods, function(method) { print(method) })

  jobj
}

cleanup.jobj <- function(jobj) {
  if (isValidJobj(jobj)) {
    objId <- jobj$id
    # If we don't know anything about this jobj, ignore it
    if (exists(objId, envir = .validJobjs)) {
      .validJobjs[[objId]] <- .validJobjs[[objId]] - 1

      if (.validJobjs[[objId]] == 0) {
        rm(list = objId, envir = .validJobjs)
        # NOTE: We cannot call removeJObject here as the finalizer may be run
        # in the middle of another RPC. Thus we queue up this object Id to be removed
        # and then run all the removeJObject when the next RPC is called.
        .toRemoveJobjs[[objId]] <- 1
      }
    }
  }
}

clearJobjs <- function() {
  valid <- ls(.validJobjs)
  rm(list = valid, envir = .validJobjs)

  removeList <- ls(.toRemoveJobjs)
  rm(list = removeList, envir = .toRemoveJobjs)
}

attach_connection <- function(jobj, connection) {

  if (inherits(jobj, "spark_jobj")) {
    jobj$connection <- connection
  }
  else if (is.list(jobj) || inherits(jobj, "struct")) {
    jobj <- lapply(jobj, function(e) {
      attach_connection(e, connection)
    })
  }
  else if (is.environment(jobj)) {
    jobj <- eapply(jobj, function(e) {
      attach_connection(e, connection)
    })
  }

  jobj
}
# Utility functions to serialize R objects so they can be read in Java.

# nolint start
# Type mapping from R to Java
#
# NULL -> Void
# integer -> Int
# character -> String
# logical -> Boolean
# double, numeric -> Double
# raw -> Array[Byte]
# Date -> Date
# POSIXct,POSIXlt -> Timestamp
#
# list[T] -> Array[T], where T is one of above mentioned types
# environment -> Map[String, T], where T is a native type
# jobj -> Object, where jobj is an object created in the backend
# nolint end

getSerdeType <- function(object) {
  type <- class(object)[[1]]

  if (type != "list") {
    type
  } else {
    # Check if all elements are of same type
    elemType <- unique(sapply(object, function(elem) { getSerdeType(elem) }))
    if (length(elemType) <= 1) {

      # Check that there are no NAs in arrays since they are unsupported in scala
      hasNAs <- any(is.na(object))

      if (hasNAs) {
        "list"
      } else {
        "array"
      }
    } else {
      "list"
    }
  }
}

writeObject <- function(con, object, writeType = TRUE) {
  type <- class(object)[[1]]

  if (type %in% c("integer", "character", "logical", "double", "numeric", "factor", "Date", "POSIXct")) {
    if (is.na(object)) {
      object <- NULL
      type <- "NULL"
    }
  }

  serdeType <- getSerdeType(object)
  if (writeType) {
    writeType(con, serdeType)
  }
  switch(serdeType,
         NULL = writeVoid(con),
         integer = writeInt(con, object),
         character = writeString(con, object),
         logical = writeBoolean(con, object),
         double = writeDouble(con, object),
         numeric = writeDouble(con, object),
         raw = writeRaw(con, object),
         array = writeArray(con, object),
         list = writeList(con, object),
         struct = writeList(con, object),
         spark_jobj = writeJobj(con, object),
         environment = writeEnv(con, object),
         Date = writeDate(con, object),
         POSIXlt = writeTime(con, object),
         POSIXct = writeTime(con, object),
         factor = writeFactor(con, object),
         `data.frame` = writeList(con, object),
         stop(paste("Unsupported type for serialization", type)))
}

writeVoid <- function(con) {
  # no value for NULL
}

writeJobj <- function(con, value) {
  if (!isValidJobj(value)) {
    stop("invalid jobj ", value$id)
  }
  writeString(con, value$id)
}

writeString <- function(con, value) {
  utfVal <- enc2utf8(value)
  writeInt(con, as.integer(nchar(utfVal, type = "bytes") + 1))
  writeBin(utfVal, con, endian = "big", useBytes = TRUE)
}

writeInt <- function(con, value) {
  writeBin(as.integer(value), con, endian = "big")
}

writeDouble <- function(con, value) {
  writeBin(value, con, endian = "big")
}

writeBoolean <- function(con, value) {
  # TRUE becomes 1, FALSE becomes 0
  writeInt(con, as.integer(value))
}

writeRawSerialize <- function(outputCon, batch) {
  outputSer <- serialize(batch, ascii = FALSE, connection = NULL)
  writeRaw(outputCon, outputSer)
}

writeRowSerialize <- function(outputCon, rows) {
  invisible(lapply(rows, function(r) {
    bytes <- serializeRow(r)
    writeRaw(outputCon, bytes)
  }))
}

serializeRow <- function(row) {
  rawObj <- rawConnection(raw(0), "wb")
  on.exit(close(rawObj))
  writeList(rawObj, row)
  rawConnectionValue(rawObj)
}

writeRaw <- function(con, batch) {
  writeInt(con, length(batch))
  writeBin(batch, con, endian = "big")
}

writeType <- function(con, class) {
  type <- switch(class,
                 NULL = "n",
                 integer = "i",
                 character = "c",
                 logical = "b",
                 double = "d",
                 numeric = "d",
                 raw = "r",
                 array = "a",
                 list = "l",
                 struct = "s",
                 spark_jobj = "j",
                 environment = "e",
                 Date = "D",
                 POSIXlt = "t",
                 POSIXct = "t",
                 factor = "c",
                 `data.frame` = "l",
                 stop(paste("Unsupported type for serialization", class)))
  writeBin(charToRaw(type), con)
}

# Used to pass arrays where all the elements are of the same type
writeArray <- function(con, arr) {
  # TODO: Empty lists are given type "character" right now.
  # This may not work if the Java side expects array of any other type.
  if (length(arr) == 0) {
    elemType <- class("somestring")
  } else {
    elemType <- getSerdeType(arr[[1]])
  }

  writeType(con, elemType)
  writeInt(con, length(arr))

  if (length(arr) > 0) {
    for (a in arr) {
      writeObject(con, a, FALSE)
    }
  }
}

# Used to pass arrays where the elements can be of different types
writeList <- function(con, list) {
  writeInt(con, length(list))
  for (elem in list) {
    writeObject(con, elem)
  }
}

# Used to pass in hash maps required on Java side.
writeEnv <- function(con, env) {
  len <- length(env)

  writeInt(con, len)
  if (len > 0) {
    writeArray(con, as.list(ls(env)))
    vals <- lapply(ls(env), function(x) { env[[x]] })
    writeList(con, as.list(vals))
  }
}

writeDate <- function(con, date) {
  writeString(con, as.character(date))
}

writeTime <- function(con, time) {
  writeDouble(con, as.double(time))
}

writeFactor <- function(con, factor) {
  writeString(con, as.character(factor))
}

# Used to serialize in a list of objects where each
# object can be of a different type. Serialization format is
# <object type> <object> for each object
writeArgs <- function(con, args) {
  if (length(args) > 0) {
    for (a in args) {
      writeObject(con, a)
    }
  }
}
core_get_package_function <- function(packageName, functionName) {
  if (packageName %in% rownames(installed.packages()) &&
      exists(functionName, envir = asNamespace(packageName)))
    get(functionName, envir = asNamespace(packageName))
  else
    NULL
}
worker_config_serialize <- function(config) {
  paste(
    if (isTRUE(config$debug)) "TRUE" else "FALSE",
    spark_config_value(config, "sparklyr.worker.gateway.port", "8880"),
    spark_config_value(config, "sparklyr.worker.gateway.address", "localhost"),
    if (isTRUE(config$profile)) "TRUE" else "FALSE",
    sep = ";"
  )
}

worker_config_deserialize <- function(raw) {
  parts <- strsplit(raw, ";")[[1]]

  list(
    debug = as.logical(parts[[1]]),
    sparklyr.gateway.port = as.integer(parts[[2]]),
    sparklyr.gateway.address = parts[[3]],
    profile = as.logical(parts[[4]])
  )
}
spark_worker_apply <- function(sc) {
  hostContextId <- worker_invoke_method(sc, FALSE, "Handler", "getHostContext")
  worker_log("retrieved worker context id ", hostContextId)

  context <- structure(
    class = c("spark_jobj", "shell_jobj"),
    list(
      id = hostContextId,
      connection = sc
    )
  )

  worker_log("retrieved worker context")

  bundlePath <- worker_invoke(context, "getBundlePath")
  if (nchar(bundlePath) > 0) {
    bundleName <- basename(bundlePath)
    worker_log("using bundle name ", bundleName)

    workerRootDir <- worker_invoke_static(sc, "org.apache.spark.SparkFiles", "getRootDirectory")
    sparkBundlePath <- file.path(workerRootDir, bundleName)

    worker_log("using bundle path ", normalizePath(sparkBundlePath))

    if (!file.exists(sparkBundlePath)) {
      stop("failed to find bundle under SparkFiles root directory")
    }

    unbundlePath <- worker_spark_apply_unbundle(
      sparkBundlePath,
      workerRootDir,
      tools::file_path_sans_ext(bundleName)
    )

    .libPaths(unbundlePath)
    worker_log("updated .libPaths with bundle packages")
  }

  grouped_by <- worker_invoke(context, "getGroupBy")
  grouped <- !is.null(grouped_by) && length(grouped_by) > 0
  if (grouped) worker_log("working over grouped data")

  length <- worker_invoke(context, "getSourceArrayLength")
  worker_log("found ", length, " rows")

  groups <- worker_invoke(context, if (grouped) "getSourceArrayGroupedSeq" else "getSourceArraySeq")
  worker_log("retrieved ", length(groups), " rows")

  closureRaw <- worker_invoke(context, "getClosure")
  closure <- unserialize(closureRaw)

  funcContextRaw <- worker_invoke(context, "getContext")
  funcContext <- unserialize(funcContextRaw)

  closureRLangRaw <- worker_invoke(context, "getClosureRLang")
  if (length(closureRLangRaw) > 0) {
    worker_log("found rlang closure")
    closureRLang <- spark_worker_rlang_unserialize()
    if (!is.null(closureRLang)) {
      closure <- closureRLang(closureRLangRaw)
      worker_log("created rlang closure")
    }
  }

  columnNames <- worker_invoke(context, "getColumns")

  if (!grouped) groups <- list(list(groups))

  all_results <- NULL

  for (group_entry in groups) {
    # serialized groups are wrapped over single lists
    data <- group_entry[[1]]

    df <- do.call(rbind.data.frame, c(data, list(stringsAsFactors = FALSE)))

    # rbind removes Date classes so we re-assign them here
    if (length(data) > 0 && ncol(df) > 0 && nrow(df) > 0 &&
        any(sapply(data[[1]], function(e) class(e)[[1]]) %in% c("Date", "POSIXct"))) {
      first_row <- data[[1]]
      for (idx in seq_along(first_row)) {
        first_class <- class(first_row[[idx]])[[1]]
        if (identical(first_class, "Date")) {
          df[[idx]] <- as.Date(df[[idx]], origin = "1970-01-01")
        } else if (identical(first_class, "POSIXct")) {
          df[[idx]] <- as.POSIXct(df[[idx]], origin = "1970-01-01")
        }
      }
    }

    result <- NULL

    if (nrow(df) == 0) {
      worker_log("found that source has no rows to be proceesed")
    }
    else {
      colnames(df) <- columnNames[1: length(colnames(df))]

      closure_params <- length(formals(closure))
      closure_args <- c(
        list(df),
        if (!is.null(funcContext)) list(funcContext) else NULL,
        as.list(
          if (nrow(df) > 0)
            lapply(grouped_by, function(group_by_name) df[[group_by_name]][[1]])
          else
            NULL
        )
      )[0:closure_params]

      worker_log("computing closure")
      result <- do.call(closure, closure_args)
      worker_log("computed closure")

      if (!identical(class(result), "data.frame")) {
        worker_log("data.frame expected but ", class(result), " found")
        result <- data.frame(result)
      }

      if (!is.data.frame(result)) stop("Result from closure is not a data.frame")
    }

    if (grouped) {
      new_column_values <- lapply(grouped_by, function(grouped_by_name) df[[grouped_by_name]][[1]])
      names(new_column_values) <- grouped_by

      result <- do.call("cbind", list(new_column_values, result))
    }

    all_results <- rbind(all_results, result)
  }

  if (!is.null(all_results) && nrow(all_results) > 0) {
    worker_log("updating ", nrow(all_results), " rows")
    all_data <- lapply(1:nrow(all_results), function(i) as.list(all_results[i,]))

    worker_invoke(context, "setResultArraySeq", all_data)
    worker_log("updated ", nrow(all_results), " rows")
  } else {
    worker_log("found no rows in closure result")
  }

  worker_log("finished apply")
}

spark_worker_rlang_unserialize <- function() {
  rlang_unserialize <- core_get_package_function("rlang", "bytes_unserialise")
  if (is.null(rlang_unserialize))
    core_get_package_function("rlanglabs", "bytes_unserialise")
  else
    rlang_unserialize
}

spark_worker_unbundle_path <- function() {
  file.path("sparklyr-bundle")
}

#' Extracts a bundle of dependencies required by \code{spark_apply()}
#'
#' @param bundle_path Path to the bundle created using \code{spark_apply_bundle()}
#' @param base_path Base path to use while extracting bundles
#'
#' @keywords internal
#' @export
worker_spark_apply_unbundle <- function(bundle_path, base_path, bundle_name) {
  extractPath <- file.path(base_path, spark_worker_unbundle_path(), bundle_name)
  lockFile <- file.path(extractPath, "sparklyr.lock")

  if (!dir.exists(extractPath)) dir.create(extractPath, recursive = TRUE)

  if (length(dir(extractPath)) == 0) {
    worker_log("found that the unbundle path is empty, extracting:", extractPath)

    writeLines("", lockFile)
    system2("tar", c("-xf", bundle_path, "-C", extractPath))
    unlink(lockFile)
  }

  if (file.exists(lockFile)) {
    worker_log("found that lock file exists, waiting")
    while (file.exists(lockFile)) {
      Sys.sleep(1)
    }
    worker_log("completed lock file wait")
  }

  extractPath
}
spark_worker_connect <- function(
  sessionId,
  backendPort = 8880,
  config = list()) {

  gatewayPort <- spark_config_value(config, "sparklyr.worker.gateway.port", backendPort)

  gatewayAddress <- spark_config_value(config, "sparklyr.worker.gateway.address", "localhost")
  config <- list()

  worker_log("is connecting to backend using port ", gatewayPort)

  gatewayInfo <- spark_connect_gateway(gatewayAddress,
                                       gatewayPort,
                                       sessionId,
                                       config = config,
                                       isStarting = TRUE)

  worker_log("is connected to backend")
  worker_log("is connecting to backend session")

  tryCatch({
    # set timeout for socket connection
    timeout <- spark_config_value(config, "sparklyr.backend.timeout", 30 * 24 * 60 * 60)
    backend <- socketConnection(host = "localhost",
                                port = gatewayInfo$backendPort,
                                server = FALSE,
                                blocking = TRUE,
                                open = "wb",
                                timeout = timeout)
  }, error = function(err) {
    close(gatewayInfo$gateway)

    stop(
      "Failed to open connection to backend:", err$message
    )
  })

  worker_log("is connected to backend session")

  sc <- structure(class = c("spark_worker_connection"), list(
    # spark_connection
    master = "",
    method = "shell",
    app_name = NULL,
    config = NULL,
    # spark_shell_connection
    spark_home = NULL,
    backend = backend,
    monitor = gatewayInfo$gateway,
    output_file = NULL
  ))

  worker_log("created connection")

  sc
}

connection_is_open.spark_worker_connection <- function(sc) {
  bothOpen <- FALSE
  if (!identical(sc, NULL)) {
    tryCatch({
      bothOpen <- isOpen(sc$backend) && isOpen(sc$monitor)
    }, error = function(e) {
    })
  }
  bothOpen
}

worker_connection <- function(x, ...) {
  UseMethod("worker_connection")
}

worker_connection.spark_jobj <- function(x, ...) {
  x$connection
}
worker_invoke_method <- function(sc, static, object, method, ...)
{
  core_invoke_method(sc, static, object, method, ...)
}

worker_invoke <- function(jobj, method, ...) {
  UseMethod("worker_invoke")
}

worker_invoke.shell_jobj <- function(jobj, method, ...) {
  worker_invoke_method(worker_connection(jobj), FALSE, jobj, method, ...)
}

worker_invoke_static <- function(sc, class, method, ...) {
  worker_invoke_method(sc, TRUE, class, method, ...)
}

worker_invoke_new <- function(sc, class, ...) {
  invoke_method(sc, TRUE, class, "<init>", ...)
}
worker_log_env <- new.env()

worker_log_session <- function(sessionId) {
  assign('sessionId', sessionId, envir = worker_log_env)
}

worker_log_format <- function(message, level = "INFO", component = "RScript") {
  paste(
    format(Sys.time(), "%y/%m/%d %H:%M:%S"),
    " ",
    level,
    " sparklyr: ",
    component,
    " (",
    worker_log_env$sessionId,
    ") ",
    message,
    sep = "")
}

worker_log_level <- function(..., level) {
  if (is.null(worker_log_env$sessionId)) return()

  args = list(...)
  message <- paste(args, sep = "", collapse = "")
  formatted <- worker_log_format(message, level)
  cat(formatted, "\n")
}

worker_log <- function(...) {
  worker_log_level(..., level = "INFO")
}

worker_log_warning<- function(...) {
  worker_log_level(..., level = "WARN")
}

worker_log_error <- function(...) {
  worker_log_level(..., level = "ERROR")
}
.worker_globals <- new.env(parent = emptyenv())

spark_worker_main <- function(
  sessionId,
  backendPort = 8880,
  configRaw = NULL) {

  spark_worker_hooks()

  tryCatch({
    worker_log_session(sessionId)

    if (is.null(configRaw)) configRaw <- worker_config_serialize(list())

    config <- worker_config_deserialize(configRaw)

    if (identical(config$profile, TRUE)) {
      profile_name <- paste("spark-apply-", as.numeric(Sys.time()), ".Rprof", sep = "")
      worker_log("starting new profile in ", file.path(getwd(), profile_name))
      utils::Rprof(profile_name)
    }

    if (config$debug) {
      worker_log("exiting to wait for debugging session to attach")

      # sleep for 1 day to allow long debugging sessions
      Sys.sleep(60*60*24)
      return()
    }

    worker_log("is starting")

    sc <- spark_worker_connect(sessionId, backendPort, config)
    worker_log("is connected")

    spark_worker_apply(sc)

    if (identical(config$profile, TRUE)) {
      # utils::Rprof(NULL)
      worker_log("closing profile")
    }

  }, error = function(e) {
    worker_log_error("terminated unexpectedly: ", e$message)
    if (exists(".stopLastError", envir = .GlobalEnv)) {
      worker_log_error("collected callstack: \n", get(".stopLastError", envir = .worker_globals))
    }
    quit(status = -1)
  })

  worker_log("finished")
}

spark_worker_hooks <- function() {
  unlock <- get("unlockBinding")
  lock <- get("lockBinding")

  originalStop <- stop
  unlock("stop",  as.environment("package:base"))
  assign("stop", function(...) {
    frame_names <- list()
    frame_start <- max(1, sys.nframe() - 5)
    for (i in frame_start:sys.nframe()) {
      current_call <- sys.call(i)
      frame_names[[1 + i - frame_start]] <- paste(i, ": ", paste(head(deparse(current_call), 5), collapse = "\n"), sep = "")
    }

    assign(".stopLastError", paste(rev(frame_names), collapse = "\n"), envir = .worker_globals)
    originalStop(...)
  }, as.environment("package:base"))
  lock("stop",  as.environment("package:base"))
}
do.call(spark_worker_main, as.list(commandArgs(trailingOnly = TRUE)))
    """
}
