// SparkStockProcessor.scala
package com.stockmonitor.spark

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.types._
import java.sql.Timestamp

object SparkStockProcessor {
  
  case class TradeData(
    symbol: String,
    price: Double,
    volume: Long,
    timestamp: Timestamp
  )
  
  case class AggregatedData(
    symbol: String,
    window_start: Timestamp,
    window_end: Timestamp,
    avg_price: Double,
    max_price: Double,
    min_price: Double,
    total_volume: Long,
    trade_count: Long,
    ma_5min: Double
  )
  
  def main(args: Array[String]): Unit = {
    
    val spark = SparkSession.builder()
      .appName("StockMarketStreamProcessor")
      .master("local[*]")
      .config("spark.sql.streaming.checkpointLocation", "/tmp/checkpoint")
      .config("spark.sql.shuffle.partitions", "3")
      .getOrCreate()
    
    spark.sparkContext.setLogLevel("WARN")
    
    import spark.implicits._
    
    // Define schema for incoming JSON
    val tradeSchema = StructType(Seq(
      StructField("symbol", StringType, nullable = false),
      StructField("price", DoubleType, nullable = false),
      StructField("volume", LongType, nullable = false),
      StructField("timestamp", LongType, nullable = false)
    ))
    
    // Read from Kafka
    val rawTrades = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "raw-trades")
      .option("startingOffsets", "latest")
      .option("failOnDataLoss", "false")
      .load()
    
    // Parse JSON and convert timestamp
    val parsedTrades = rawTrades
      .selectExpr("CAST(value AS STRING) as json")
      .select(from_json($"json", tradeSchema).as("data"))
      .select("data.*")
      .withColumn("timestamp", 
        to_timestamp(from_unixtime($"timestamp" / 1000)))
      .as[TradeData]
    
    // Apply watermarking (handle late data up to 1 minute)
    val watermarkedTrades = parsedTrades
      .withWatermark("timestamp", "1 minute")
    
    // Create 5-minute sliding windows (update every 1 minute)
    val windowedAggregates = watermarkedTrades
      .groupBy(
        $"symbol",
        window($"timestamp", "5 minutes", "1 minute")
      )
      .agg(
        avg($"price").as("avg_price"),
        max($"price").as("max_price"),
        min($"price").as("min_price"),
        sum($"volume").as("total_volume"),
        count($"price").as("trade_count")
      )
      .select(
        $"symbol",
        $"window.start".as("window_start"),
        $"window.end".as("window_end"),
        $"avg_price",
        $"max_price",
        $"min_price",
        $"total_volume",
        $"trade_count"
      )
    
    // Calculate 5-minute moving average
    val withMovingAverage = windowedAggregates
      .withColumn("ma_5min", $"avg_price")
    
    // Write to TimescaleDB/PostgreSQL
    val dbWriter = withMovingAverage.writeStream
      .foreachBatch { (batchDF: org.apache.spark.sql.Dataset[org.apache.spark.sql.Row], batchId: Long) =>
        
        // JDBC properties
        val jdbcUrl = "jdbc:postgresql://localhost:5432/stockdb"
        val connectionProperties = new java.util.Properties()
        connectionProperties.put("user", "postgres")
        connectionProperties.put("password", "postgres")
        connectionProperties.put("driver", "org.postgresql.Driver")
        
        // Write to database
        batchDF.write
          .mode("append")
          .jdbc(jdbcUrl, "stock_aggregates", connectionProperties)
        
        println(s"Batch $batchId: Wrote ${batchDF.count()} records to database")
      }
      .outputMode("append")
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .start()
    
    // Also write to console for monitoring
    val consoleWriter = withMovingAverage.writeStream
      .outputMode("append")
      .format("console")
      .option("truncate", false)
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .start()
    
    // Detect breakouts (price crossing MA)
    val breakoutDetection = watermarkedTrades
      .groupBy($"symbol", window($"timestamp", "1 minute"))
      .agg(
        last($"price").as("current_price"),
        avg($"price").as("window_avg")
      )
      .select(
        $"symbol",
        $"window.start".as("timestamp"),
        $"current_price",
        $"window_avg",
        when($"current_price" > $"window_avg", lit("BULLISH"))
          .when($"current_price" < $"window_avg", lit("BEARISH"))
          .otherwise(lit("NEUTRAL")).as("signal")
      )
      .filter($"signal" =!= lit("NEUTRAL"))
    
    val alertWriter = breakoutDetection.writeStream
      .foreachBatch { (batchDF: org.apache.spark.sql.Dataset[org.apache.spark.sql.Row], batchId: Long) =>
        
        val jdbcUrl = "jdbc:postgresql://localhost:5432/stockdb"
        val connectionProperties = new java.util.Properties()
        connectionProperties.put("user", "postgres")
        connectionProperties.put("password", "postgres")
        connectionProperties.put("driver", "org.postgresql.Driver")
        
        batchDF.write
          .mode("append")
          .jdbc(jdbcUrl, "breakout_alerts", connectionProperties)
        
        // Print alerts to console
        batchDF.collect().foreach { row =>
          println(s"🚨 ALERT: ${row.getAs[String]("symbol")} - " +
            s"${row.getAs[String]("signal")} signal at " +
            s"$${row.getAs[Double]("current_price")}")
        }
      }
      .outputMode("append")
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .start()
    
    // Wait for termination
    spark.streams.awaitAnyTermination()
  }
}
