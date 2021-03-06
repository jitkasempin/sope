package com.sope.etl.transform.model.io

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonProperty, JsonSubTypes, JsonTypeInfo}
import com.sope.spark.sql._
import com.sope.utils.Logging
import org.apache.spark.sql.{DataFrame, SaveMode}

/**
  * Package contains YAML Transformer Output construct mappings and definitions
  *
  * @author mbadgujar
  */
package object output {


  @JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
  @JsonSubTypes(Array(
    new Type(value = classOf[HiveTarget], name = "hive"),
    new Type(value = classOf[OrcTarget], name = "orc"),
    new Type(value = classOf[ParquetTarget], name = "parquet"),
    new Type(value = classOf[CSVTarget], name = "csv"),
    new Type(value = classOf[TextTarget], name = "text"),
    new Type(value = classOf[JsonTarget], name = "json"),
    new Type(value = classOf[CountOutput], name = "count"),
    new Type(value = classOf[ShowOutput], name = "show")
  ))
  abstract class TargetTypeRoot(@JsonProperty(value = "type", required = true) id: String, input: String, mode: String)
    extends Logging {
    def apply(df: DataFrame): Unit

    def getSaveMode: SaveMode = mode.toLowerCase match {
      case "overwrite" => SaveMode.Overwrite
      case "append" => SaveMode.Append
      case "error_if_exits" => SaveMode.ErrorIfExists
      case "ignore" => SaveMode.Ignore
    }

    def getInput: String = input

    def getOptions(options: Map[String, String]): Map[String, String] = Option(options).getOrElse(Map())
  }

  case class HiveTarget(@JsonProperty(required = true) input: String,
                        @JsonProperty(required = true) mode: String,
                        @JsonProperty(required = true) db: String,
                        @JsonProperty(required = true) table: String) extends TargetTypeRoot("hive", input, mode) {
    def apply(df: DataFrame): Unit = {
      val targetTable = s"$db.$table"
      val targetTableDF = df.sqlContext.table(targetTable)
      df.select(targetTableDF.getColumns: _*).write.mode(getSaveMode).insertInto(targetTable)
    }
  }

  case class OrcTarget(@JsonProperty(required = true) input: String,
                       @JsonProperty(required = true) mode: String,
                       @JsonProperty(required = true) path: String,
                       options: Map[String, String]) extends TargetTypeRoot("orc", input, mode) {
    def apply(df: DataFrame): Unit = df.write.mode(getSaveMode).options(getOptions(options)).orc(path)
  }

  case class ParquetTarget(@JsonProperty(required = true) input: String,
                           @JsonProperty(required = true) mode: String,
                           @JsonProperty(required = true) path: String,
                           options: Map[String, String]) extends TargetTypeRoot("parquet", input, mode) {
    def apply(df: DataFrame): Unit = df.write.mode(getSaveMode).options(getOptions(options)).parquet(path)
  }

  case class CSVTarget(@JsonProperty(required = true) input: String,
                       @JsonProperty(required = true) mode: String,
                       @JsonProperty(required = true) path: String,
                       options: Map[String, String]) extends TargetTypeRoot("csv", input, mode) {
    def apply(df: DataFrame): Unit = df.write.mode(getSaveMode).options(getOptions(options)).csv(path)
  }

  case class TextTarget(@JsonProperty(required = true) input: String,
                        @JsonProperty(required = true) mode: String,
                        @JsonProperty(required = true) path: String,
                        options: Map[String, String]) extends TargetTypeRoot("text", input, mode) {
    def apply(df: DataFrame): Unit = df.write.mode(getSaveMode).options(getOptions(options)).text(path)
  }

  case class JsonTarget(@JsonProperty(required = true) input: String,
                        @JsonProperty(required = true) mode: String,
                        @JsonProperty(required = true) path: String,
                        options: Map[String, String]) extends TargetTypeRoot("json", input, mode) {
    def apply(df: DataFrame): Unit = df.write.mode(getSaveMode).options(getOptions(options)).json(path)
  }

  case class CountOutput(@JsonProperty(required = true) input: String) extends TargetTypeRoot("count", input, "") {
    def apply(df: DataFrame): Unit = logInfo(s"Count for transformation alias: $input :- ${df.count}")
  }

  case class ShowOutput(@JsonProperty(required = true) input: String,
                        @JsonProperty(required = true) num_records: Int) extends TargetTypeRoot("show", input, "") {
    def apply(df: DataFrame): Unit = {
      logInfo(s"Showing sample rows for transformation alias: $input")
      if (num_records == 0) df.show(num_records, truncate = false) else df.show(false)
    }
  }

}
