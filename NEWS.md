
# Sparklyr 0.6.4

- Added support for `spark_apply_bundle()` to support `spark_apply()` under
  `yarn-cluster` mode.

- Added support for HTTPS for `yarn-cluster` which is activated by setting
  `yarn.http.policy` to `HTTPS_ONLY` in `yarn-site.xml`.

- Added support for `sparklyr.yarn.cluster.accepted.timeout` under `yarn-cluster`
  to allow users to wait for resources under cluster with high waiting times.

- Fix to `spark_apply()` when package distribution deadlock triggers in 
  environments where multiple executors run under the same node.

- Added support in `spark_apply()` for specifying  a list of `packages` to
  distribute to each worker node.

- Added support in`yarn-cluster` for `sparklyr.yarn.cluster.lookup.prefix`,
  `sparklyr.yarn.cluster.lookup.username` and `sparklyr.yarn.cluster.lookup.byname`
  to control the new application lookup behavior.

# Sparklyr 0.6.3

- Enabled support for Java 9 for clusters configured with 
  Hadoop 2.8. Java 9 blocked on 'master=local' unless
  'options(sparklyr.java9 = TRUE)' is set.

- Fixed issue in `spark_connect()` where using `set.seed()`
  before connection would cause session ids to be duplicates
  and connections to be reused.

- Fixed issue in `spark_connect()` blocking gateway port when
  connection was never started to the backend, for isntasnce,
  while interrupting the r session while connecting.
  
- Performance improvement for quering field names from tables
  impacting tables and `dplyr` queries, most noticeable in
  `na.omit` with several columns.

- Fix to `spark_apply()` when closure returns a `data.frame`
  that contains no rows and has one or more columns.

- Fix to `spark_apply()` while using `tryCatch()` within
  closure and increased callstack printed to logs when
  error triggers within closure.

- Added support for the `SPARKLYR_LOG_FILE` environment 
  variable to specify the file used for log output.
  
- Fixed regression for `union_all()` affecting Spark 1.6.X.

- Added support for `na.omit.cache` option that when set to
  `FALSE` will prevent `na.omit` from caching results when
  rows are dropped.

- Added support in `spark_connect()` for `yarn-cluster` with
  hight-availability enabled.
  
- Added support for `spark_connect()` with `master="yarn-cluster"`
  to query YARN resource manager API and retrieve the correct
  container host name.

- Fixed issue in `invoke()` calls while using integer arrays
  that contain `NA` which can be commonly experienced
  while using `spark_apply()`.

- Added `topics.description` under `ml_lda()` result.

- Added support for `ft_stop_words_remover()` to strip out
  stop words from tokens.

- Feature transformers (`ft_*` functions) now explicitly
  require `input.col` and `output.col` to be specified.

- Added support for `spark_apply_log()` to enable logging in
  worker nodes while using `spark_apply()`.

- Fix to `spark_apply()` for `SparkUncaughtExceptionHandler`
  exception while running over large jobs that may overlap
  during an, now unnecesary, unregister operation.

- Fix race-condition first time `spark_apply()` is run when more
  than one partition runs in a worker and both processes try to
  unpack the packages bundle at the same time.

- `spark_apply()` now adds generic column names when needed and 
  validates `f` is a `function`.

- Improved documentation and error cases for `metric` argument in
  `ml_classification_eval()` and `ml_binary_classification_eval()`.

- Fix to `spark_install()` to use the `/logs` subfolder to store local
  `log4j` logs.

- Fix to `spark_apply()` when R is used from a worker node since worker
  node already contains packages but still might be triggering different
  R session.

- Fix connection from closing when `invoke()` attempts to use a class
  with a method that contains a reference to an undefined class.

- Implemented all tuning options from Spark ML for `ml_random_forest()`, `ml_gradient_boosted_trees()`, and `ml_decision_tree()`.

- Avoid tasks failing under `spark_apply()` and multiple  concurrent
  partitions running while selecting backend port.

- Added support for numeric arguments for `n` in `lead()` for dplyr.

- Added unsupported error message to `sample_n()` and `sample_frac()`
  when Spark is not 2.0 or higher.

- Fixed `SIGPIPE` error under `spark_connect()` immediately after
  a `spark_disconnect()` operation.

- Added support for `sparklyr.apply.env.` under `spark_config()` to
  allow `spark_apply()` to initializae environment varaibles.

- Added support for `spark_read_text()` and `spark_write_text()` to
  read from and to plain text files.

- Addesd support for RStudio project templates to create an 
  "R Package using sparklyr".
  
- Fix `compute()` to trigger refresh of the connections view.

- Added a `k` argument to `ml_pca()` to enable specification of number of principal
  components to extract. Also implemented `sdf_project()` to project datasets using
  the results of `ml_pca()` models.

- Added support for additional livy session creation parameters using
  the `livy_config()` function.

# Sparklyr 0.6.1

- Fixed error in `spark_apply()` that may triggered when multiple CPUs
  are used in a single node due to race conditions while accesing the
  gateway service and another in the `JVMObjectTracker`.

- `spark_apply()` now supports explicit column types using the `columns`
  argument to avoid sampling types.

- `spark_apply()` with `group_by` no longer requires persisting to disk
  nor memory.

- Added support for Spark 1.6.3 under `spark_install()`.

- Added support for Spark 1.6.3 under `spark_install()`

- `spark_apply()` now logs the current callstack when it fails.

- Fixed error triggered while processing empty partitions in `spark_apply()`.

- Fixed slow printing issue caused by `print` calculating the total row count, 
  which is expensive for some tables.

- Fixed `sparklyr 0.6` issue blocking concurrent `sparklyr` connections, which
 required to set `config$sparklyr.gateway.remote = FALSE` as workaround.

# Sparklyr 0.6.0

### Distributed R

- Added `packages` parameter to `spark_apply()` to distribute packages
  across worker nodes automatically.

- Added `sparklyr.closures.rlang` as a `spark_config()` value to support
  generic closures provided by the `rlang` package.
  
- Added config options `sparklyr.worker.gateway.address` and
  `sparklyr.worker.gateway.port` to configure gateway used under
  worker nodes.

- Added `group_by` parameter to `spark_apply()`, to support operations
  over groups of dataframes.
  
- Added `spark_apply()`, allowing users to use R code to directly
  manipulate and transform Spark DataFrames.

### External Data

- Added `spark_write_source()`. This function writes data into a
  Spark data source which can be loaded through an Spark package.

- Added `spark_write_jdbc()`. This function writes from a Spark DataFrame
  into a JDBC connection.
  
- Added `columns` parameter to `spark_read_*()` functions to load data with
  named columns or explicit column types.

- Added `partition_by` parameter to `spark_write_csv()`, `spark_write_json()`,
  `spark_write_table()` and `spark_write_parquet()`.

- Added `spark_read_source()`. This function reads data from a
  Spark data source which can be loaded through an Spark package.

- Added support for `mode = "overwrite"` and `mode = "append"` to
  `spark_write_csv()`.

- `spark_write_table()` now supports saving to default Hive path.

- Improved performance of `spark_read_csv()` reading remote data when
  `infer_schema = FALSE`.

- Added `spark_read_jdbc()`. This function reads from a JDBC connection
  into a Spark DataFrame.

- Renamed `spark_load_table()` and `spark_save_table()` into `spark_read_table()`
  and `spark_write_table()` for consistency with existing `spark_read_*()` and
  `spark_write_*()` functions.

- Added support to specify a vector of column names in `spark_read_csv()` to
  specify column names without having to set the type of each column.

- Improved `copy_to()`, `sdf_copy_to()` and `dbWriteTable()` performance under
  `yarn-client` mode.

### dplyr

- Support for `cumprod()` to calculate cumulative products.

- Support for `cor()`, `cov()`, `sd()` and `var()` as window functions.

- Support for Hive built-in operators `%like%`, `%rlike%`, and
  `%regexp%` for matching regular expressions in `filter()` and `mutate()`.

- Support for dplyr (>= 0.6) which among many improvements, increases
  performance in some queries by making use of a new query optimizer.

- `sample_frac()` takes a fraction instead of a percent to match dplyr.

- Improved performance of `sample_n()` and `sample_frac()` through the use of
  `TABLESAMPLE` in the generated query.
  
### Databases

- Added `src_databases()`. This function list all the available databases.

- Added `tbl_change_db()`. This function changes current database.

### DataFrames

- Added `sdf_len()`, `sdf_seq()` and `sdf_along()` to help generate numeric
  sequences as Spark DataFrames.

- Added `spark_set_checkpoint_dir()`, `spark_get_checkpoint_dir()`, and 
  `sdf_checkpoint()` to enable checkpointing.
  
- Added `sdf_broadcast()` which can be used to hint the query
  optimizer to perform a broadcast join in cases where a shuffle
  hash join is planned but not optimal.

- Added `sdf_repartition()`, `sdf_coalesce()`, and `sdf_num_partitions()` 
  to support repartitioning and getting the number of partitions of Spark 
  DataFrames.

- Added `sdf_bind_rows()` and `sdf_bind_cols()` -- these functions
  are the `sparklyr` equivalent of `dplyr::bind_rows()` and 
  `dplyr::bind_cols()`.
  
- Added `sdf_separate_column()` -- this function allows one to separate
  components of an array / vector column into separate scalar-valued
  columns.

- `sdf_with_sequential_id()` now supports `from` parameter to choose the
  starting value of the id column.

- Added `sdf_pivot()`. This function provides a mechanism for constructing
  pivot tables, using Spark's 'groupBy' + 'pivot' functionality, with a
  formula interface similar to that of `reshape2::dcast()`.
  
### MLlib

- Added `vocabulary.only` to `ft_count_vectorizer()` to retrieve the 
  vocabulary with ease.

- GLM type models now support `weights.column` to specify weights in model
  fitting. (#217)

- `ml_logistic_regression()` now supports multinomial regression, in
  addition to binomial regression [requires Spark 2.1.0 or greater]. (#748)

- Implemented `residuals()` and `sdf_residuals()` for Spark linear 
  regression and GLM models. The former returns a R vector while 
  the latter returns a `tbl_spark` of training data with a `residuals`
  column added.
  
- Added `ml_model_data()`, used for extracting data associated with
  Spark ML models.

- The `ml_save()` and `ml_load()` functions gain a `meta` argument, allowing
  users to specify where R-level model metadata should be saved independently
  of the Spark model itself. This should help facilitate the saving and loading
  of Spark models used in non-local connection scenarios.

- `ml_als_factorization()` now supports the implicit matrix factorization
   and nonnegative least square options.

- Added `ft_count_vectorizer()`. This function can be used to transform
  columns of a Spark DataFrame so that they might be used as input to `ml_lda()`.
  This should make it easier to invoke `ml_lda()` on Spark data sets.
  
### Broom
  
- Implemented `tidy()`, `augment()`, and `glance()` from tidyverse/broom for
  `ml_model_generalized_linear_regression` and `ml_model_linear_regression`
  models.
  
### R Compatibility

- Implemented `cbind.tbl_spark()`. This method works by first generating
  index columns using `sdf_with_sequential_id()` then performing `inner_join()`.
  Note that dplyr `_join()` functions should still be used for DataFrames 
  with common keys since they are less expensive.

### Connections

- Increased default number of concurrent connections by setting default for
  `spark.port.maxRetries` from 16 to 128.

- Support for gateway connections `sparklyr://hostname:port/session` and using
  `spark-submit --class sparklyr.Shell sparklyr-2.1-2.11.jar <port> <id> --remote`.

- Added support for `sparklyr.gateway.service` and `sparklyr.gateway.remote` to
  enable/disable the gateway in service and to accept remote connections required
  for Yarn Cluster mode.

- Added support for Yarn Cluster mode using `master = "yarn-cluster"`. Either,
  explicitly set `config = list(sparklyr.gateway.address = "<driver-name>")` or
  implicitly `sparklyr` will read the `site-config.xml` for the `YARN_CONF_DIR`
  environment variable.
  
- Added `spark_context_config()` and `hive_context_config()` to retrieve
  runtime configurations for the Spark and Hive contexts.

- Added `sparklyr.log.console` to redirect logs to console, useful
  to troubleshooting `spark_connect`.

- Added `sparklyr.backend.args` as config option to enable passing
  parameters to the `sparklyr` backend.

- Improved logging while establishing connections to `sparklyr`.

- Improved `spark_connect()` performance.

- Implemented new configuration checks to proactively report connection errors
  in Windows.

- While connecting to spark from Windows, setting the `sparklyr.verbose` option
  to `TRUE` prints detailed configuration steps.

- Added `custom_headers` to `livy_config()` to add custom headers to the REST call
  to the Livy server
  
### Compilation

- Added support for `jar_dep` in the compilation specification to
  support additional `jars` through `spark_compile()`.

- `spark_compile()` now prints deprecation warnings.

- Added `download_scalac()` to assist downloading all the Scala compilers
  required to build using `compile_package_jars` and provided support for
  using any `scalac` minor versions while looking for the right compiler.

### Backend

- Improved backend logging by adding type and session id prefix.

### Miscellaneous

- `copy_to()` and `sdf_copy_to()` auto generate a `name` when an expression
  can't be transformed into a table name.

- Implemented `type_sum.jobj()` (from tibble) to enable better printing of jobj
  objects embedded in data frames.

- Added the `spark_home_set()` function, to help facilitate the setting of the
  `SPARK_HOME` environment variable. This should prove useful in teaching
  environments, when teaching the basics of Spark and sparklyr.

- Added support for the `sparklyr.ui.connections` option, which adds additional
  connection options into the new connections dialog. The
  `rstudio.spark.connections` option is now deprecated.

- Implemented the "New Connection Dialog" as a Shiny application to be able to
  support newer versions of RStudio that deprecate current connections UI.

### Bug Fixes

- When using `spark_connect()` in local clusters, it validates that `java` exists
  under `JAVA_HOME` to help troubleshoot systems that have an incorrect `JAVA_HOME`.

- Improved `argument is of length zero` error triggered while retrieving data 
  with no columns to display.

- Fixed `Path does not exist` referencing `hdfs` exception during `copy_to` under
  systems configured with `HADOOP_HOME`.

- Fixed session crash after "No status is returned" error by terminating
  invalid connection and added support to print log trace during this error.

- `compute()` now caches data in memory by default. To revert this beavior use
  `sparklyr.dplyr.compute.nocache` set to `TRUE`.

- `spark_connect()` with `master = "local"` and a given `version` overrides
  `SPARK_HOME` to avoid existing installation mismatches.

- Fixed `spark_connect()` under Windows issue when `newInstance0` is present in 
  the logs.

- Fixed collecting `long` type columns when NAs are present (#463).
  
- Fixed backend issue that affects systems where `localhost` does
  not resolve properly to the loopback address.

- Fixed issue collecting data frames containing newlines `\n`.

- Spark Null objects (objects of class NullType) discovered within numeric
  vectors are now collected as NAs, rather than lists of NAs.

- Fixed warning while connecting with livy and improved 401 message.

- Fixed issue in `spark_read_parquet()` and other read methods in which
  `spark_normalize_path()` would not work in some platforms while loading
  data using custom protocols like `s3n://` for Amazon S3.

- Resolved issue in `spark_save()` / `load_table()` to support saving / loading
  data and added path parameter in `spark_load_table()` for consistency with
  other functions.

# Sparklyr 0.5.5

- Implemented support for `connectionViewer` interface required in RStudio 1.1
  and `spark_connect` with `mode="databricks"`.

# Sparklyr 0.5.4

- Implemented support for `dplyr 0.6` and Spark 2.1.x.
  
# Sparklyr 0.5.3

- Implemented support for `DBI 0.6`.

# Sparklyr 0.5.2

- Fix to `spark_connect` affecting Windows users and Spark 1.6.x.

- Fix to Livy connections which would cause connections to fail while connection is on 'waiting' state.

# Sparklyr 0.5.0

- Implemented basic authorization for Livy connections using
  `livy_config_auth()`.

- Added support to specify additional `spark-submit` parameters using the
  `sparklyr.shell.args` environment variable.

- Renamed `sdf_load()` and `sdf_save()` to `spark_read()` and `spark_write()`
  for consistency.

- The functions `tbl_cache()` and `tbl_uncache()` can now be using without
  requiring the `dplyr` namespace to be loaded.

- `spark_read_csv(..., columns = <...>, header = FALSE)` should now work as
  expected -- previously, `sparklyr` would still attempt to normalize the
  column names provided.

- Support to configure Livy using the `livy.` prefix in the `config.yml` file.

- Implemented experimental support for Livy through: `livy_install()`,
  `livy_service_start()`, `livy_service_stop()` and
  `spark_connect(method = "livy")`.

- The `ml` routines now accept `data` as an optional argument, to support
  calls of the form e.g. `ml_linear_regression(y ~ x, data = data)`. This
  should be especially helpful in conjunction with `dplyr::do()`.

- Spark `DenseVector` and `SparseVector` objects are now deserialized as
  R numeric vectors, rather than Spark objects. This should make it easier
  to work with the output produced by `sdf_predict()` with Random Forest
  models, for example.

- Implemented `dim.tbl_spark()`. This should ensure that `dim()`, `nrow()`
  and `ncol()` all produce the expected result with `tbl_spark`s.

- Improved Spark 2.0 installation in Windows by creating `spark-defaults.conf`
  and configuring `spark.sql.warehouse.dir`.

- Embedded Apache Spark package dependencies to avoid requiring internet 
  connectivity while connecting for the first through `spark_connect`. The
  `sparklyr.csv.embedded` config setting was added to configure a regular
  expression to match Spark versions where the embedded package is deployed.

- Increased exception callstack and message length to include full 
  error details when an exception is thrown in Spark.

- Improved validation of supported Java versions.

- The `spark_read_csv()` function now accepts the `infer_schema` parameter,
  controlling whether the columns schema should be inferred from the underlying
  file itself. Disabling this should improve performance when the schema is
  known beforehand.

- Added a `do_.tbl_spark` implementation, allowing for the execution of
  `dplyr::do` statements on Spark DataFrames. Currently, the computation is
  performed in serial across the different groups specified on the Spark
  DataFrame; in the future we hope to explore a parallel implementation.
  Note that `do_` always returns a `tbl_df` rather than a `tbl_spark`, as
  the objects produced within a `do_` query may not necessarily be Spark
  objects.

- Improved errors, warnings and fallbacks for unsupported Spark versions.

- `sparklyr` now defaults to `tar = "internal"` in its calls to `untar()`.
  This should help resolve issues some Windows users have seen related to
  an inability to connect to Spark, which ultimately were caused by a lack
  of permissions on the Spark installation.

- Resolved an issue where `copy_to()` and other R => Spark data transfer
  functions could fail when the last column contained missing / empty values.
  (#265)

- Added `sdf_persist()` as a wrapper to the Spark DataFrame `persist()` API.

- Resolved an issue where `predict()` could produce results in the wrong
  order for large Spark DataFrames.

- Implemented support for `na.action` with the various Spark ML routines. The
  value of `getOption("na.action")` is used by default. Users can customize the
  `na.action` argument through the `ml.options` object accepted by all ML
  routines.

- On Windows, long paths, and paths containing spaces, are now supported within
  calls to `spark_connect()`.

- The `lag()` window function now accepts numeric values for `n`. Previously,
  only integer values were accepted. (#249)

- Added support to configure Ppark environment variables using `spark.env.*` config.

- Added support for the `Tokenizer` and `RegexTokenizer` feature transformers.
  These are exported as the `ft_tokenizer()` and `ft_regex_tokenizer()` functions.

- Resolved an issue where attempting to call `copy_to()` with an R `data.frame`
  containing many columns could fail with a Java StackOverflow. (#244)

- Resolved an issue where attempting to call `collect()` on a Spark DataFrame
  containing many columns could produce the wrong result. (#242)

- Added support to parameterize network timeouts using the
  `sparklyr.backend.timeout`, `sparklyr.gateway.start.timeout` and
  `sparklyr.gateway.connect.timeout` config settings.

- Improved logging while establishing connections to `sparklyr`.

- Added `sparklyr.gateway.port` and `sparklyr.gateway.address` as config settings.

- The `spark_log()` function now accepts the `filter` parameter. This can be used
  to filter entries within the Spark log.

- Increased network timeout for `sparklyr.backend.timeout`.

- Moved `spark.jars.default` setting from options to Spark config.

- `sparklyr` now properly respects the Hive metastore directory with the
  `sdf_save_table()` and `sdf_load_table()` APIs for Spark < 2.0.0.

- Added `sdf_quantile()` as a means of computing (approximate) quantiles
  for a column of a Spark DataFrame.

- Added support for `n_distinct(...)` within the `dplyr` interface, based on
  call to Hive function `count(DISTINCT ...)`. (#220)

# Sparklyr 0.4.0

- First release to CRAN.
