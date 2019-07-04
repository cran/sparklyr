sparklyr: R interface for Apache Spark
================

[![Build
Status](https://travis-ci.org/rstudio/sparklyr.svg?branch=master)](https://travis-ci.org/rstudio/sparklyr)
[![AppVeyor Build
Status](https://ci.appveyor.com/api/projects/status/github/rstudio/sparklyr?branch=master&svg=true)](https://ci.appveyor.com/project/JavierLuraschi/sparklyr)
[![CRAN\_Status\_Badge](https://www.r-pkg.org/badges/version/sparklyr)](https://cran.r-project.org/package=sparklyr)
<a href="https://www.r-pkg.org/pkg/sparklyr"><img src="https://cranlogs.r-pkg.org/badges/sparklyr?color=brightgreen" style=""></a>
[![codecov](https://codecov.io/gh/rstudio/sparklyr/branch/master/graph/badge.svg)](https://codecov.io/gh/rstudio/sparklyr)
[![Join the chat at
https://gitter.im/rstudio/sparklyr](https://badges.gitter.im/rstudio/sparklyr.svg)](https://gitter.im/rstudio/sparklyr?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

<img src="tools/readme/sparklyr-illustration.png" width="320" align="right" style="margin-left: 20px; margin-right: 20px"/>

  - Install and connect to [Spark](http://spark.apache.org/) using YARN,
    Mesos, Livy or Kubernetes.
  - Use [dplyr](https://spark.rstudio.com/dplyr/) to filter and
    aggregate Spark datasets and
    [streams](https://spark.rstudio.com/guides/streaming/) then bring
    them into R for analysis and visualization.
  - Use [MLlib](http://spark.apache.org/docs/latest/mllib-guide.html),
    [H2O](https://spark.rstudio.com/guides/h2o/),
    [XGBoost](https://github.com/rstudio/sparkxgb) and
    [GraphFrames](https://github.com/rstudio/graphframes) to train
    models at scale in Spark.
  - Create interoperable machine learning
    [pipelines](https://spark.rstudio.com/guides/pipelines/) and
    productionize them with
    [MLeap](https://spark.rstudio.com/guides/mleap/).
  - Create [extensions](http://spark.rstudio.com/extensions.html) that
    call the full Spark API or run [distributed
    R](https://spark.rstudio.com/guides/distributed-r/) code to support
    new functionality.

## Installation

You can install the **sparklyr** package from
[CRAN](https://CRAN.r-project.org) as follows:

``` r
install.packages("sparklyr")
```

You should also install a local version of Spark for development
purposes:

``` r
library(sparklyr)
spark_install()
```

To upgrade to the latest version of sparklyr, run the following command
and restart your r session:

``` r
install.packages("devtools")
devtools::install_github("rstudio/sparklyr")
```

If you use the RStudio IDE, you should also download the latest [preview
release](https://www.rstudio.com/products/rstudio/download/preview/) of
the IDE which includes several enhancements for interacting with Spark
(see the [RStudio IDE](#rstudio-ide) section below for more details).

## Connecting to Spark

You can connect to both local instances of Spark as well as remote Spark
clusters. Here we’ll connect to a local instance of Spark via the
[spark\_connect](https://spark.rstudio.com/reference/spark-connections/)
function:

``` r
library(sparklyr)
sc <- spark_connect(master = "local")
```

The returned Spark connection (`sc`) provides a remote dplyr data source
to the Spark cluster.

For more information on connecting to remote Spark clusters see the
[Deployment](https://spark.rstudio.com/deployment/) section of the
sparklyr website.

## Using dplyr

We can now use all of the available dplyr verbs against the tables
within the cluster.

We’ll start by copying some datasets from R into the Spark cluster (note
that you may need to install the nycflights13 and Lahman packages in
order to execute this code):

``` r
install.packages(c("nycflights13", "Lahman"))
```

``` r
library(dplyr)
iris_tbl <- copy_to(sc, iris)
flights_tbl <- copy_to(sc, nycflights13::flights, "flights")
batting_tbl <- copy_to(sc, Lahman::Batting, "batting")
src_tbls(sc)
```

    ## [1] "batting" "flights" "iris"

To start with here’s a simple filtering example:

``` r
# filter by departure delay and print the first few records
flights_tbl %>% filter(dep_delay == 2)
```

    ## # Source: spark<?> [?? x 19]
    ##     year month   day dep_time sched_dep_time dep_delay arr_time
    ##    <int> <int> <int>    <int>          <int>     <dbl>    <int>
    ##  1  2013     1     1      517            515         2      830
    ##  2  2013     1     1      542            540         2      923
    ##  3  2013     1     1      702            700         2     1058
    ##  4  2013     1     1      715            713         2      911
    ##  5  2013     1     1      752            750         2     1025
    ##  6  2013     1     1      917            915         2     1206
    ##  7  2013     1     1      932            930         2     1219
    ##  8  2013     1     1     1028           1026         2     1350
    ##  9  2013     1     1     1042           1040         2     1325
    ## 10  2013     1     1     1231           1229         2     1523
    ## # … with more rows, and 12 more variables: sched_arr_time <int>,
    ## #   arr_delay <dbl>, carrier <chr>, flight <int>, tailnum <chr>,
    ## #   origin <chr>, dest <chr>, air_time <dbl>, distance <dbl>, hour <dbl>,
    ## #   minute <dbl>, time_hour <dttm>

[Introduction to dplyr](https://CRAN.R-project.org/package=dplyr)
provides additional dplyr examples you can try. For example, consider
the last example from the tutorial which plots data on flight delays:

``` r
delay <- flights_tbl %>% 
  group_by(tailnum) %>%
  summarise(count = n(), dist = mean(distance), delay = mean(arr_delay)) %>%
  filter(count > 20, dist < 2000, !is.na(delay)) %>%
  collect

# plot delays
library(ggplot2)
ggplot(delay, aes(dist, delay)) +
  geom_point(aes(size = count), alpha = 1/2) +
  geom_smooth() +
  scale_size_area(max_size = 2)
```

    ## `geom_smooth()` using method = 'gam' and formula 'y ~ s(x, bs = "cs")'

![](tools/readme/dplyr-ggplot2-1.png)<!-- -->

### Window Functions

dplyr [window functions](https://CRAN.R-project.org/package=dplyr) are
also supported, for example:

``` r
batting_tbl %>%
  select(playerID, yearID, teamID, G, AB:H) %>%
  arrange(playerID, yearID, teamID) %>%
  group_by(playerID) %>%
  filter(min_rank(desc(H)) <= 2 & H > 0)
```

    ## # Source:     spark<?> [?? x 7]
    ## # Groups:     playerID
    ## # Ordered by: playerID, yearID, teamID
    ##    playerID  yearID teamID     G    AB     R     H
    ##    <chr>      <int> <chr>  <int> <int> <int> <int>
    ##  1 aaronha01   1959 ML1      154   629   116   223
    ##  2 aaronha01   1963 ML1      161   631   121   201
    ##  3 abadfe01    2012 HOU       37     7     0     1
    ##  4 abbated01   1905 BSN      153   610    70   170
    ##  5 abbated01   1904 BSN      154   579    76   148
    ##  6 abbeych01   1894 WAS      129   523    95   164
    ##  7 abbeych01   1895 WAS      132   511   102   141
    ##  8 abbotji01   1999 MIL       20    21     0     2
    ##  9 abnersh01   1992 CHA       97   208    21    58
    ## 10 abnersh01   1990 SDN       91   184    17    45
    ## # … with more rows

For additional documentation on using dplyr with Spark see the
[dplyr](https://spark.rstudio.com/dplyr/) section of the sparklyr
website.

## Using SQL

It’s also possible to execute SQL queries directly against tables within
a Spark cluster. The `spark_connection` object implements a
[DBI](https://github.com/rstats-db/DBI) interface for Spark, so you can
use `dbGetQuery` to execute SQL and return the result as an R data
frame:

``` r
library(DBI)
iris_preview <- dbGetQuery(sc, "SELECT * FROM iris LIMIT 10")
iris_preview
```

    ##    Sepal_Length Sepal_Width Petal_Length Petal_Width Species
    ## 1           5.1         3.5          1.4         0.2  setosa
    ## 2           4.9         3.0          1.4         0.2  setosa
    ## 3           4.7         3.2          1.3         0.2  setosa
    ## 4           4.6         3.1          1.5         0.2  setosa
    ## 5           5.0         3.6          1.4         0.2  setosa
    ## 6           5.4         3.9          1.7         0.4  setosa
    ## 7           4.6         3.4          1.4         0.3  setosa
    ## 8           5.0         3.4          1.5         0.2  setosa
    ## 9           4.4         2.9          1.4         0.2  setosa
    ## 10          4.9         3.1          1.5         0.1  setosa

## Machine Learning

You can orchestrate machine learning algorithms in a Spark cluster via
the [machine
learning](http://spark.apache.org/docs/latest/mllib-guide.html)
functions within **sparklyr**. These functions connect to a set of
high-level APIs built on top of DataFrames that help you create and tune
machine learning workflows.

Here’s an example where we use
[ml\_linear\_regression](https://spark.rstudio.com/reference/ml_linear_regression/)
to fit a linear regression model. We’ll use the built-in `mtcars`
dataset, and see if we can predict a car’s fuel consumption (`mpg`)
based on its weight (`wt`), and the number of cylinders the engine
contains (`cyl`). We’ll assume in each case that the relationship
between `mpg` and each of our features is linear.

``` r
# copy mtcars into spark
mtcars_tbl <- copy_to(sc, mtcars)

# transform our data set, and then partition into 'training', 'test'
partitions <- mtcars_tbl %>%
  filter(hp >= 100) %>%
  mutate(cyl8 = cyl == 8) %>%
  sdf_partition(training = 0.5, test = 0.5, seed = 1099)

# fit a linear model to the training dataset
fit <- partitions$training %>%
  ml_linear_regression(response = "mpg", features = c("wt", "cyl"))
fit
```

    ## Formula: mpg ~ wt + cyl
    ## 
    ## Coefficients:
    ## (Intercept)          wt         cyl 
    ##   33.499452   -2.818463   -0.923187

For linear regression models produced by Spark, we can use `summary()`
to learn a bit more about the quality of our fit, and the statistical
significance of each of our predictors.

``` r
summary(fit)
```

    ## Deviance Residuals:
    ##    Min     1Q Median     3Q    Max 
    ## -1.752 -1.134 -0.499  1.296  2.282 
    ## 
    ## Coefficients:
    ## (Intercept)          wt         cyl 
    ##   33.499452   -2.818463   -0.923187 
    ## 
    ## R-Squared: 0.8274
    ## Root Mean Squared Error: 1.422

Spark machine learning supports a wide array of algorithms and feature
transformations and as illustrated above it’s easy to chain these
functions together with dplyr pipelines. To learn more see the [machine
learning](https://spark.rstudio.com/mlib/) section.

## Reading and Writing Data

You can read and write data in CSV, JSON, and Parquet formats. Data can
be stored in HDFS, S3, or on the local filesystem of cluster nodes.

``` r
temp_csv <- tempfile(fileext = ".csv")
temp_parquet <- tempfile(fileext = ".parquet")
temp_json <- tempfile(fileext = ".json")

spark_write_csv(iris_tbl, temp_csv)
iris_csv_tbl <- spark_read_csv(sc, "iris_csv", temp_csv)

spark_write_parquet(iris_tbl, temp_parquet)
iris_parquet_tbl <- spark_read_parquet(sc, "iris_parquet", temp_parquet)

spark_write_json(iris_tbl, temp_json)
iris_json_tbl <- spark_read_json(sc, "iris_json", temp_json)

src_tbls(sc)
```

    ## [1] "batting"      "flights"      "iris"         "iris_csv"    
    ## [5] "iris_json"    "iris_parquet" "mtcars"

## Distributed R

You can execute arbitrary r code across your cluster using
`spark_apply`. For example, we can apply `rgamma` over `iris` as
follows:

``` r
spark_apply(iris_tbl, function(data) {
  data[1:4] + rgamma(1,2)
})
```

    ## # Source: spark<?> [?? x 4]
    ##    Sepal_Length Sepal_Width Petal_Length Petal_Width
    ##           <dbl>       <dbl>        <dbl>       <dbl>
    ##  1         6.59        4.99         2.89        1.69
    ##  2         6.39        4.49         2.89        1.69
    ##  3         6.19        4.69         2.79        1.69
    ##  4         6.09        4.59         2.99        1.69
    ##  5         6.49        5.09         2.89        1.69
    ##  6         6.89        5.39         3.19        1.89
    ##  7         6.09        4.89         2.89        1.79
    ##  8         6.49        4.89         2.99        1.69
    ##  9         5.89        4.39         2.89        1.69
    ## 10         6.39        4.59         2.99        1.59
    ## # … with more rows

You can also group by columns to perform an operation over each group of
rows and make use of any package within the closure:

``` r
spark_apply(
  iris_tbl,
  function(e) broom::tidy(lm(Petal_Width ~ Petal_Length, e)),
  columns = c("term", "estimate", "std.error", "statistic", "p.value"),
  group_by = "Species"
)
```

    ## # Source: spark<?> [?? x 6]
    ##   Species    term         estimate std.error statistic  p.value
    ##   <chr>      <chr>           <dbl>     <dbl>     <dbl>    <dbl>
    ## 1 versicolor (Intercept)   -0.0843    0.161     -0.525 6.02e- 1
    ## 2 versicolor Petal_Length   0.331     0.0375     8.83  1.27e-11
    ## 3 virginica  (Intercept)    1.14      0.379      2.99  4.34e- 3
    ## 4 virginica  Petal_Length   0.160     0.0680     2.36  2.25e- 2
    ## 5 setosa     (Intercept)   -0.0482    0.122     -0.396 6.94e- 1
    ## 6 setosa     Petal_Length   0.201     0.0826     2.44  1.86e- 2

## Extensions

The facilities used internally by sparklyr for its dplyr and machine
learning interfaces are available to extension packages. Since Spark is
a general purpose cluster computing system there are many potential
applications for extensions (e.g. interfaces to custom machine learning
pipelines, interfaces to 3rd party Spark packages, etc.).

Here’s a simple example that wraps a Spark text file line counting
function with an R function:

``` r
# write a CSV 
tempfile <- tempfile(fileext = ".csv")
write.csv(nycflights13::flights, tempfile, row.names = FALSE, na = "")

# define an R interface to Spark line counting
count_lines <- function(sc, path) {
  spark_context(sc) %>% 
    invoke("textFile", path, 1L) %>% 
      invoke("count")
}

# call spark to count the lines of the CSV
count_lines(sc, tempfile)
```

    ## [1] 336777

To learn more about creating extensions see the
[Extensions](https://spark.rstudio.com/extensions/) section of the
sparklyr website.

## Table Utilities

You can cache a table into memory with:

``` r
tbl_cache(sc, "batting")
```

and unload from memory using:

``` r
tbl_uncache(sc, "batting")
```

## Connection Utilities

You can view the Spark web console using the `spark_web` function:

``` r
spark_web(sc)
```

You can show the log using the `spark_log` function:

``` r
spark_log(sc, n = 10)
```

    ## 19/07/03 09:30:13 INFO ContextCleaner: Cleaned accumulator 2070
    ## 19/07/03 09:30:13 INFO ContextCleaner: Cleaned accumulator 2082
    ## 19/07/03 09:30:13 INFO ContextCleaner: Cleaned accumulator 2096
    ## 19/07/03 09:30:13 INFO ContextCleaner: Cleaned accumulator 2131
    ## 19/07/03 09:30:13 INFO ContextCleaner: Cleaned accumulator 2068
    ## 19/07/03 09:30:13 INFO ContextCleaner: Cleaned accumulator 2112
    ## 19/07/03 09:30:13 INFO ContextCleaner: Cleaned accumulator 2127
    ## 19/07/03 09:30:13 INFO ContextCleaner: Cleaned accumulator 2107
    ## 19/07/03 09:30:13 INFO ContextCleaner: Cleaned accumulator 2059
    ## 19/07/03 09:30:13 INFO ContextCleaner: Cleaned accumulator 2049

Finally, we disconnect from Spark:

``` r
  spark_disconnect(sc)
```

    ## NULL

## RStudio IDE

The latest RStudio [Preview
Release](https://www.rstudio.com/products/rstudio/download/preview/) of
the RStudio IDE includes integrated support for Spark and the sparklyr
package, including tools for:

  - Creating and managing Spark connections
  - Browsing the tables and columns of Spark DataFrames
  - Previewing the first 1,000 rows of Spark DataFrames

Once you’ve installed the sparklyr package, you should find a new
**Spark** pane within the IDE. This pane includes a **New Connection**
dialog which can be used to make connections to local or remote Spark
instances:

<img src="tools/readme/spark-connect.png" class="screenshot" width=389 />

Once you’ve connected to Spark you’ll be able to browse the tables
contained within the Spark cluster and preview Spark DataFrames using
the standard RStudio data
viewer:

<img src="tools/readme/spark-dataview.png" class="screenshot" width=639 />

You can also connect to Spark through [Livy](http://livy.io) through a
new connection
dialog:

<img src="tools/readme/spark-connect-livy.png" class="screenshot" width=389 />

<div style="margin-bottom: 15px;">

</div>

The RStudio IDE features for sparklyr are available now as part of the
[RStudio Preview
Release](https://www.rstudio.com/products/rstudio/download/preview/).

## Using H2O

[rsparkling](https://cran.r-project.org/package=rsparkling) is a CRAN
package from [H2O](http://h2o.ai) that extends
[sparklyr](http://spark.rstudio.com) to provide an interface into
[Sparkling Water](https://github.com/h2oai/sparkling-water). For
instance, the following example installs, configures and runs
[h2o.glm](http://docs.h2o.ai/h2o/latest-stable/h2o-docs/data-science/glm.html):

``` r
library(rsparkling)
library(sparklyr)
library(dplyr)
library(h2o)

sc <- spark_connect(master = "local", version = "2.3.2")
mtcars_tbl <- copy_to(sc, mtcars, "mtcars")

mtcars_h2o <- as_h2o_frame(sc, mtcars_tbl, strict_version_check = FALSE)

mtcars_glm <- h2o.glm(x = c("wt", "cyl"), 
                      y = "mpg",
                      training_frame = mtcars_h2o,
                      lambda_search = TRUE)
```

``` r
mtcars_glm
```

    ## Model Details:
    ## ==============
    ## 
    ## H2ORegressionModel: glm
    ## Model ID:  GLM_model_R_1527265202599_1 
    ## GLM Model: summary
    ##     family     link                              regularization
    ## 1 gaussian identity Elastic Net (alpha = 0.5, lambda = 0.1013 )
    ##                                                                lambda_search
    ## 1 nlambda = 100, lambda.max = 10.132, lambda.min = 0.1013, lambda.1se = -1.0
    ##   number_of_predictors_total number_of_active_predictors
    ## 1                          2                           2
    ##   number_of_iterations                                training_frame
    ## 1                  100 frame_rdd_31_ad5c4e88ec97eb8ccedae9475ad34e02
    ## 
    ## Coefficients: glm coefficients
    ##       names coefficients standardized_coefficients
    ## 1 Intercept    38.941654                 20.090625
    ## 2       cyl    -1.468783                 -2.623132
    ## 3        wt    -3.034558                 -2.969186
    ## 
    ## H2ORegressionMetrics: glm
    ## ** Reported on training data. **
    ## 
    ## MSE:  6.017684
    ## RMSE:  2.453097
    ## MAE:  1.940985
    ## RMSLE:  0.1114801
    ## Mean Residual Deviance :  6.017684
    ## R^2 :  0.8289895
    ## Null Deviance :1126.047
    ## Null D.o.F. :31
    ## Residual Deviance :192.5659
    ## Residual D.o.F. :29
    ## AIC :156.2425

``` r
spark_disconnect(sc)
```

## Connecting through Livy

[Livy](https://github.com/cloudera/livy) enables remote connections to
Apache Spark clusters. Connecting to Spark clusters through Livy is
**under experimental development** in `sparklyr`. Please post any
feedback or questions as a GitHub issue as needed.

Before connecting to Livy, you will need the connection information to
an existing service running Livy. Otherwise, to test `livy` in your
local environment, you can install it and run it locally as follows:

``` r
livy_install(version = "2.4.0")
```

``` r
livy_service_start()
```

To connect, use the Livy service address as `master` and `method =
"livy"` in `spark_connect`. Once connection completes, use `sparklyr` as
usual, for
instance:

``` r
sc <- spark_connect(master = "http://localhost:8998", method = "livy", version = "2.4.0")
copy_to(sc, iris)
```

    ## # Source: spark<iris> [?? x 5]
    ##    Sepal_Length Sepal_Width Petal_Length Petal_Width Species
    ##           <dbl>       <dbl>        <dbl>       <dbl> <chr>  
    ##  1          5.1         3.5          1.4         0.2 setosa 
    ##  2          4.9         3            1.4         0.2 setosa 
    ##  3          4.7         3.2          1.3         0.2 setosa 
    ##  4          4.6         3.1          1.5         0.2 setosa 
    ##  5          5           3.6          1.4         0.2 setosa 
    ##  6          5.4         3.9          1.7         0.4 setosa 
    ##  7          4.6         3.4          1.4         0.3 setosa 
    ##  8          5           3.4          1.5         0.2 setosa 
    ##  9          4.4         2.9          1.4         0.2 setosa 
    ## 10          4.9         3.1          1.5         0.1 setosa 
    ## # … with more rows

``` r
spark_disconnect(sc)
```

    ## NULL

Once you are done using `livy` locally, you should stop this service
with:

``` r
livy_service_stop()
```

To connect to remote `livy` clusters that support basic authentication
connect as:

``` r
config <- livy_config(username="<username>", password="<password>")
sc <- spark_connect(master = "<address>", method = "livy", config = config)
spark_disconnect(sc)
```
