new_ml_model_kmeans <- function(pipeline_model, formula, dataset,
                                features_col) {
  m <- new_ml_model_clustering(
    pipeline_model = pipeline_model,
    formula = formula,
    dataset = dataset,
    features_col = features_col,
    class = "ml_model_kmeans"
  )

  model <- m$model

  m$summary <- model$summary

  m$centers <- model$cluster_centers() %>%
    do.call(rbind, .) %>%
    as.data.frame() %>%
    rlang::set_names(m$feature_names)

  if (!is_required_spark(spark_connection(dataset), "3.0.0")) {
    m$cost <- suppressWarnings(
      possibly_null(
        ~ pipeline_model %>%
          ml_stage(1) %>%
          ml_transform(dataset) %>%
          model$compute_cost()
      )()
    )
  }

  m
}

#' @export
print.ml_model_kmeans <- function(x, ...) {
  preamble <- sprintf(
    "K-means clustering with %s %s",
    nrow(x$centers),
    if (nrow(x$centers) == 1) "cluster" else "clusters"
  )

  cat(preamble, sep = "\n")
  print_newline()
  ml_model_print_centers(x)

  print_newline()
  cat(
    "Within Set Sum of Squared Errors = ",
    if (is.null(x$cost)) "not computed." else x$cost
  )
}

#' Evaluate a K-mean clustering
#'
#' @param model A fitted K-means model returned by \code{ml_kmeans()}
#' @param dataset Dataset on which to calculate K-means cost
#'
#' @name ml_kmeans_cluster_eval
NULL

#' @rdname ml_kmeans
#' @inheritParams ml_kmeans_cluster_eval
#' @return \code{ml_compute_cost()} returns the K-means cost (sum of
#'   squared distances of points to their nearest center) for the model
#'   on the given data.
#' @export
ml_compute_cost <- function(model, dataset) {
  # spark_require_version(spark_connection(spark_jobj(model)), required = "2.0.0", required_max = "3.0.0")

  if (inherits(model, "ml_model_kmeans")) {
    model$pipeline_model %>%
      ml_stage(1) %>%
      ml_transform(dataset) %>%
      model$model$compute_cost()
  } else {
    model$compute_cost(dataset)
  }
}

#' @rdname ml_kmeans
#' @inheritParams ml_kmeans_cluster_eval
#' @param distance_measure Distance measure to apply when computing the Silhouette measure.
#' @return \code{ml_compute_silhouette_measure()} returns the Silhouette measure
#'   of the clustering on the given data.
#' @export
ml_compute_silhouette_measure <- function(model,
                                          dataset,
                                          distance_measure = c("squaredEuclidean", "cosine")) {
  if (!is_required_spark(spark_connection(dataset), "3.0.0")) {
    stop("Silhouette measure is only supported in Spark 3.0 or above.")
  }

  distance_measure <- match.arg(distance_measure)
  if (inherits(model, "ml_model_kmeans")) {
    model$pipeline_model %>%
      ml_stage(1) %>%
      ml_transform(dataset) %>%
      model$model$compute_silhouette_measure(distance_measure)
  } else {
    model$compute_silhouette_measure(dataset, distance_measure)
  }
}
