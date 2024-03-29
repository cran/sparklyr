#' Feature Transformation -- QuantileDiscretizer (Estimator)
#'
#' \code{ft_quantile_discretizer} takes a column with continuous features and outputs
#'   a column with binned categorical features. The number of bins can be
#'   set using the \code{num_buckets} parameter. It is possible that the number
#'   of buckets used will be smaller than this value, for example, if there
#'   are too few distinct values of the input to create enough distinct
#'   quantiles.
#'
#'   NaN handling: null and NaN values will be ignored from the column
#'   during \code{QuantileDiscretizer} fitting. This will produce a \code{Bucketizer}
#'   model for making predictions. During the transformation, \code{Bucketizer}
#'   will raise an error when it finds NaN values in the dataset, but the
#'   user can also choose to either keep or remove NaN values within the
#'   dataset by setting \code{handle_invalid} If the user chooses to keep NaN values,
#'   they will be handled specially and placed into their own bucket,
#'   for example, if 4 buckets are used, then non-NaN data will be put
#'   into buckets[0-3], but NaNs will be counted in a special bucket[4].
#'
#'   Algorithm: The bin ranges are chosen using an approximate algorithm (see
#'   the documentation for org.apache.spark.sql.DataFrameStatFunctions.approxQuantile
#'   \href{https://spark.apache.org/docs/latest/api/scala/index.html#org.apache.spark.sql.DataFrameStatFunctions}{here} for a detailed description). The precision of the approximation can be
#'   controlled with the \code{relative_error} parameter. The lower and upper bin
#'   bounds will be -Infinity and +Infinity, covering all real values.
#'
#'   Note that the result may be different every time you run it, since the sample
#'   strategy behind it is non-deterministic.
#'
#' @template roxlate-ml-feature-input-output-col
#' @template roxlate-ml-feature-transformer
#' @template roxlate-ml-feature-estimator-transformer
#' @template roxlate-ml-feature-handle-invalid
#'
#' @param input_cols Names of input columns.
#' @param output_cols Names of output columns.
#' @param num_buckets Number of buckets (quantiles, or categories) into which data
#'   points are grouped. Must be greater than or equal to 2.
#' @param num_buckets_array Array of number of buckets (quantiles, or categories)
#'   into which data points are grouped. Each value must be greater than or equal to 2.
#' @param relative_error (Spark 2.0.0+) Relative error (see documentation for
#'   org.apache.spark.sql.DataFrameStatFunctions.approxQuantile
#'   \href{https://spark.apache.org/docs/latest/api/scala/index.html#org.apache.spark.sql.DataFrameStatFunctions}{here}
#'   for description). Must be in the range [0, 1]. default: 0.001
#' @param weight_column If not NULL, then a generalized version of the Greenwald-Khanna algorithm will be run to compute
#'   weighted percentiles, with each input having a relative weight specified by the corresponding value in `weight_column`.
#'   The weights can be considered as relative frequencies of sample inputs.
#'
#' @seealso \code{\link{ft_bucketizer}}
#' @export
ft_quantile_discretizer <- function(x, input_col = NULL, output_col = NULL, num_buckets = 2,
                                    input_cols = NULL, output_cols = NULL, num_buckets_array = NULL,
                                    handle_invalid = "error", relative_error = 0.001,
                                    uid = random_string("quantile_discretizer_"),
                                    weight_column = NULL, ...) {
  check_dots_used()
  UseMethod("ft_quantile_discretizer")
}

ml_quantile_discretizer <- ft_quantile_discretizer

#' @export
ft_quantile_discretizer.spark_connection <- function(x, input_col = NULL, output_col = NULL, num_buckets = 2,
                                                     input_cols = NULL, output_cols = NULL, num_buckets_array = NULL,
                                                     handle_invalid = "error", relative_error = 0.001,
                                                     uid = random_string("quantile_discretizer_"),
                                                     weight_column = NULL, ...) {
  if (!is.null(weight_column) && spark_version(x) < "3.0.0") {
    stop("Weighted quantile discretizer is only supported in Spark 3.0 or above.")
  }

  .args <- list(
    input_col = input_col,
    output_col = output_col,
    num_buckets = num_buckets,
    input_cols = input_cols,
    output_cols = output_cols,
    num_buckets_array = num_buckets_array,
    handle_invalid = handle_invalid,
    relative_error = relative_error,
    uid = uid,
    weight_column = weight_column
  ) %>%
    c(rlang::dots_list(...)) %>%
    validator_ml_quantile_discretizer()

  jobj <- spark_pipeline_stage(
    x,
    if (is.null(weight_column)) {
      "org.apache.spark.ml.feature.QuantileDiscretizer"
    } else {
      "org.apache.spark.ml.feature.WeightedQuantileDiscretizer"
    },
    input_col = .args[["input_col"]], output_col = .args[["output_col"]],
    input_cols = .args[["input_cols"]], output_cols = .args[["output_cols"]],
    uid = .args[["uid"]]
  ) %>%
    jobj_set_param("setHandleInvalid", .args[["handle_invalid"]], "2.1.0", "error") %>%
    jobj_set_param("setRelativeError", .args[["relative_error"]], "2.0.0", 0.001)
  if (!is.null(weight_column)) {
    jobj <- jobj %>%
      jobj_set_param("setWeightCol", .args[["weight_column"]], "2.3.0", NULL)
  }

  if (!is.null(input_col) && !is.null(output_col)) {
    jobj <- jobj %>%
      jobj_set_param("setNumBuckets", .args[["num_buckets"]])
  } else if (!is.null(input_cols) && !is.null(output_cols)) {
    jobj <- jobj %>%
      jobj_set_param("setNumBucketsArray", .args[["num_buckets_array"]], "2.3.0")
  }

  estimator <- jobj %>%
    new_ml_quantile_discretizer()

  estimator
}

#' @export
ft_quantile_discretizer.ml_pipeline <- function(x, input_col = NULL, output_col = NULL, num_buckets = 2,
                                                input_cols = NULL, output_cols = NULL, num_buckets_array = NULL,
                                                handle_invalid = "error", relative_error = 0.001,
                                                uid = random_string("quantile_discretizer_"), ...) {
  stage <- ft_quantile_discretizer.spark_connection(
    x = spark_connection(x),
    input_col = input_col,
    output_col = output_col,
    num_buckets = num_buckets,
    input_cols = input_cols,
    output_cols = output_cols,
    num_buckets_array = num_buckets_array,
    handle_invalid = handle_invalid,
    relative_error = relative_error,
    uid = uid,
    ...
  )
  ml_add_stage(x, stage)
}

#' @export
ft_quantile_discretizer.tbl_spark <- function(x, input_col = NULL, output_col = NULL, num_buckets = 2,
                                              input_cols = NULL, output_cols = NULL, num_buckets_array = NULL,
                                              handle_invalid = "error", relative_error = 0.001,
                                              uid = random_string("quantile_discretizer_"), ...) {
  stage <- ft_quantile_discretizer.spark_connection(
    x = spark_connection(x),
    input_col = input_col,
    output_col = output_col,
    num_buckets = num_buckets,
    input_cols = input_cols,
    output_cols = output_cols,
    num_buckets_array = num_buckets_array,
    handle_invalid = handle_invalid,
    relative_error = relative_error,
    uid = uid,
    ...
  )

  if (is_ml_transformer(stage)) {
    ml_transform(stage, x)
  } else {
    ml_fit_and_transform(stage, x)
  }
}

new_ml_quantile_discretizer <- function(jobj) {
  new_ml_estimator(jobj, class = "ml_quantile_discretizer")
}

validator_ml_quantile_discretizer <- function(.args) {
  .args[["uid"]] <- cast_string(.args[["uid"]])

  if (!is.null(.args[["input_col"]]) && !is.null(.args[["input_cols"]])) {
    stop("Only one of `input_col` or `input_cols` may be specified.", call. = FALSE)
  }
  .args[["input_col"]] <- cast_nullable_string(.args[["input_col"]])
  .args[["output_col"]] <- cast_nullable_string(.args[["output_col"]])
  .args[["num_buckets"]] <- cast_scalar_integer(.args[["num_buckets"]])
  .args[["input_cols"]] <- cast_string_list(.args[["input_cols"]], allow_null = TRUE)
  .args[["output_cols"]] <- cast_string_list(.args[["output_cols"]], allow_null = TRUE)
  .args[["num_buckets_array"]] <- cast_integer_list(.args[["num_buckets_array"]], allow_null = TRUE)
  .args[["handle_invalid"]] <- cast_choice(
    .args[["handle_invalid"]],
    c("error", "skip", "keep")
  )
  .args[["relative_error"]] <- cast_scalar_double(.args[["relative_error"]])
  .args
}
