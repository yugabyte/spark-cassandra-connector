/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin._

object Publish extends Build {

  val altReleaseDeploymentRepository = sys.props.get("publish.repository.releases")
  val altSnapshotDeploymentRepository = sys.props.get("publish.repository.snapshots")

  val nexus = "https://oss.sonatype.org"
  val defaultReleaseDeploymentRepository = nexus + "/service/local/staging/deploy/maven2"
  val defaultSnapshotDeploymentRepository = nexus + "/content/repositories/snapshots"

  val releasesDeploymentRepository =
    "releases" at (altReleaseDeploymentRepository getOrElse defaultReleaseDeploymentRepository)
  val snapshotsDeploymentRepository =
    "snapshots" at (altSnapshotDeploymentRepository getOrElse defaultSnapshotDeploymentRepository)

  lazy val inlineCredentials = for (
    realm ← sys.props.get("publish.repository.credentials.realm");
    host ← sys.props.get("publish.repository.credentials.host");
    user ← sys.props.get("publish.repository.credentials.user");
    password ← sys.props.get("publish.repository.credentials.password")
  ) yield Credentials(realm, host, user, password)

  lazy val resolvedCredentials = inlineCredentials getOrElse {
    val altCredentialsLocation = sys.props.get("publish.repository.credentials.file").map(new File(_))
    val defaultCredentialsLocation = Path.userHome / ".ivy2" / ".credentials"
    val credentialsLocation = altCredentialsLocation getOrElse defaultCredentialsLocation

    Credentials(credentialsLocation)
  }

  println(s"Using $releasesDeploymentRepository for releases")
  println(s"Using $snapshotsDeploymentRepository for snapshots")

  lazy val creds = Seq(credentials += resolvedCredentials)

  override lazy val settings = creds ++ Seq(
    organizationName := "YugaByte",
    organizationHomepage := Some(url("http://www.yugabyte.com/")),

    publishTo <<= version { v: String =>
      if (v.trim.endsWith("SNAPSHOT"))
        Some(snapshotsDeploymentRepository)
      else
        Some(releasesDeploymentRepository)
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    publishArtifact in IntegrationTest := false,
    pomIncludeRepository := { x => false },
    pomExtra :=
      <scm>
        <url>git@github.com:yugabyte/spark-cassandra-connector.git</url>
        <connection>scm:git:git@github.com:yugabyte/spark-cassandra-connector.git</connection>
      </scm>
      <developers>
        <developer>
          <name>YugaByte Development Team</name>
          <email>info@yugabyte.com</email>
          <organization>YugaByte, Inc.</organization>
          <organizationUrl>https://www.yugabyte.com</organizationUrl>
        </developer>
      </developers>
  )
}