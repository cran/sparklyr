package sparklyr

import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.{Param, ParamMap}
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}
import org.apache.spark.sql.types.StructType

abstract class AbstractTransformer extends Transformer {
  override def copy(extra: ParamMap): AbstractTransformer = defaultCopy(extra)
}

abstract class AbstractSampleTransformer extends AbstractTransformer {
  final val weight = new Param[String](this, "weight", "name of the weight column")
  final val replace = new Param[Boolean](this, "replace", "whether to sample with replacement")
  final val seed = new Param[Long](this, "seed", "PRNG seed")

  def setWeight(value: String): this.type = set(weight, value)

  def setReplace(value: Boolean): this.type = set(replace, value)

  def setSeed(value: Long): this.type = set(seed, value)

  override def transformSchema(schema: StructType): StructType = {
    // sampling operation does not alter the schema
    schema
  }

  protected[this] def sampleDF(x: Dataset[_], n: Int): DataFrame = {
    val df = x.asInstanceOf[DataFrame]
    df.sparkSession.createDataFrame(
      if ($(replace)) {
        SamplingUtils.sampleWithReplacement(df.rdd, $(weight), n, $(seed))
      } else {
        SamplingUtils.sampleWithoutReplacement(df.rdd, $(weight), n, $(seed))
      },
      df.schema
    )
  }
}

class SampleN(override val uid: String) extends AbstractSampleTransformer {
  final val n = new Param[Int](this, "n", "sample size")

  def setN(value: Int): this.type = set(n, value)

  override def transform(df: Dataset[_]): DataFrame = {
    sampleDF(df, $(n))
  }
}

class SampleFrac(override val uid: String) extends AbstractSampleTransformer {
  final val frac = new Param[Double](this, "frac", "sampling fraction")

  def setFrac(value: Double): this.type = set(frac, value)

  override def transform(df: Dataset[_]): DataFrame = {
    val n: Int = ($(frac) * df.count).toInt

    sampleDF(df, n)
  }
}
