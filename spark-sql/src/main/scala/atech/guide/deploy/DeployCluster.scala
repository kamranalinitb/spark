package atech.guide.deploy

import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.sql.functions._

object DeployCluster {

  /**
    * Movies.json as args(0)
    * GoodComedies.json as args(1)
    *
    * good comedy = genre == Comedy and IMDB > 6.5
    */
  def main(args: Array[String]): Unit = {

    if (args.length != 2) {
      println("Need input path and output path")
      System.exit(1)
    }

    val spark = SparkSession.builder()
      .appName("Test Deploy App")
      // method 1
      .config("spark.executor.memory", "1g")
      .getOrCreate()

    // spark.sparkContext.setLogLevel("ERROR")

    val moviesDF = spark.read
      .option("inferSchema", "true")
      .json(args(0))

    val goodComediesDF = moviesDF
      .select(
        col("Title"),
        col("IMDB_Rating").as("Rating"),
        col("Release_Date").as("Release")
      )
      .where(col("Major_Genre") === "Comedy" and col("IMDB_Rating") > 6.5)
      .orderBy(col("Rating").desc_nulls_last)

    // method 2
    spark.conf.set("spark.executor.memory", "1g") //<- Illegal: This is NOT going to be allowed.
    // As some conf are not available to set while application is running

    goodComediesDF.show

    goodComediesDF.write
      .mode(SaveMode.Overwrite)
      .format("json")
      .save(args(1))

    // method 3
    // Passing Configurations directly into Spark Submit

  }

/**
  * Build a JAR to run a Spark application on the Docker cluster
  *
  *   - project structure -> artifacts, add artifact from "module with dependencies"
  *   - (important) check "copy to the output folder and link to manifest"
  *   - (important) then from the generated folder path, delete so that the folder path ends in src/
  *
  * Build the JAR: Build -> Build Artifacts... -> select the jar -> build
  * Copy the JAR and movies.json to spark-cluster/apps
  * (the apps and data folders are mapped to /opt/spark-apps and /opt/spark-data in the containers)
  *
  **/

/**
  * How to run the Spark application on the Docker cluster
  *
  * 1. Start the cluster
  *   docker-compose up --scale spark-worker=3
  *
  * 2. Connect to the master node
  *   docker exec -it spark-cluster_spark-master_1 bash
  *
  * 3. Run the spark-submit command
  *   /spark/bin/spark-submit \
  *     --class atech.guide.deploy.DeployCluster \
  *     --master spark://(dockerID):7077 \
  *     --deploy-mode client \
  *     --verbose \
  *     --supervise \
  *
  * Actual Command
  *
  * /spark/bin/spark-submit \
  *   --class atech.guide.deploy.DeployCluster \
  *   --master spark://b5ac6c87a0d4:7077 \
  *   --deploy-mode client \
  *   --verbose \
  *   --supervise \
  *   /opt/spark-apps/spark-sql.jar /opt/spark-data/movies.json /opt/spark-data/goodComedies.json
  *
  * Command with configurations
  * /spark/bin/spark-submit \
  *   --class atech.guide.deploy.DeployCluster \
  *   --master spark://b5ac6c87a0d4:7077 \
  *   --deploy-mode client \
  *   --conf spark.executor.memory 1g \
  *   --verbose \
  *   --supervise \
  *   /opt/spark-apps/spark-sql.jar /opt/spark-data/movies.json /opt/spark-data/goodComedies.json
  **/

}
