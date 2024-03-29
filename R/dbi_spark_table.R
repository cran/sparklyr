
setMethod(
  "dbWriteTable", "spark_connection",
  function(conn, name, value, temporary = getOption("sparklyr.dbwritetable.temp", FALSE), overwrite = FALSE, append = FALSE, repartition = 0, serializer = NULL) {
    found <- dbExistsTable(conn, name) && !overwrite
    if (found) {
      stop("Table ", name, " already exists")
    }

    if (append && overwrite) {
      stop("append and overwrite parameters cannot both be setted to TRUE.")
    }

    temp_name <- if (identical(temporary, FALSE)) random_string("sparklyr_tmp_") else name

    if (identical(append, TRUE)) {
        mode <- "append"
    } else if (identical(overwrite, TRUE)) {
        mode <- "overwrite"
    } else {
        mode <- NULL
    }

    spark_data_copy(conn, value, temp_name, repartition, serializer = serializer)

    if (identical(temporary, FALSE)) {
      spark_write_table(
        tbl(conn, temp_name),
        name,
        mode
      )
    }

    invisible(TRUE)
  }
)

setMethod(
  "dbReadTable", c("spark_connection", "character"),
  function(conn, name) {
    name <- dbQuoteIdentifier(conn, name)
    dbGetQuery(conn, paste("SELECT * FROM ", name))
  }
)


setMethod("dbListTables", "spark_connection", function(conn, database = NULL) {
  query <- "SHOW TABLES"
  if (!is.null(database)) {
    query <- paste(query, "FROM", database, sep = " ")
  }
  df <- df_from_sql(conn, query)

  if (nrow(df) <= 0) {
    character(0)
  }
  else {
    tableNames <- df$tableName
    filtered <- grep("^sparklyr_tmp_", tableNames, invert = TRUE, value = TRUE)
    sort(filtered)
  }
})


setMethod("dbExistsTable", c("spark_connection", "character"), function(conn, name) {
  tolower(name) %in% tolower(dbListTables(conn))
})


setMethod(
  "dbRemoveTable", c("spark_connection", "character"),
  function(conn, name) {
    dbi_ensure_no_backtick(name)

    dbSendQuery(conn, paste0("DROP TABLE `", name, "`"))
    invisible(TRUE)
  }
)
