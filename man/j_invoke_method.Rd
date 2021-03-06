% Generated by roxygen2: do not edit by hand
% Please edit documentation in R/spark_invoke.R
\name{j_invoke_method}
\alias{j_invoke_method}
\title{Generic Call Interface}
\usage{
j_invoke_method(sc, static, object, method, ...)
}
\arguments{
\item{sc}{\code{spark_connection}}

\item{static}{Is this a static method call (including a constructor). If so
then the \code{object} parameter should be the name of a class (otherwise
it should be a spark_jobj instance).}

\item{object}{Object instance or name of class (for \code{static})}

\item{method}{Name of method}

\item{...}{Call parameters}
}
\description{
Call a Java method and retrieve the return value through a JVM object
reference.
}
\keyword{internal}
