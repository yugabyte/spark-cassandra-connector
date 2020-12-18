import java.io.File

import sbt.Path
import sbt._
import sbt.librarymanagement.Resolver
import sbt.librarymanagement.ivy.Credentials

import scala.sys.process._

object Publishing extends sbt.librarymanagement.DependencyBuilders {

  val Version: String = {
    sys.props.get("publish.version").getOrElse("git describe --tags" !!).stripLineEnd.stripPrefix("v")
  }

  val altReleaseDeploymentRepository = sys.props.get("publish.repository.name")
  val altReleaseDeploymentLocation = sys.props.get("publish.repository.location")

  val nexus = "https://oss.sonatype.org/"
  val SonatypeSnapshots = Some("snapshots" at nexus + "content/repositories/snapshots")
  val SonatypeReleases = Some("releases" at nexus + "service/local/staging/deploy/maven2")

  val Repository: Option[Resolver] = {
    (altReleaseDeploymentRepository, altReleaseDeploymentLocation) match {
      case (Some(name), Some(location)) => Some(name at location)
      case _ => if (Version.endsWith("SNAPSHOT")) {
        SonatypeSnapshots
      } else {
        SonatypeReleases
      }
    }
  }

  println(s"Using $Repository for publishing")

  lazy val inlineCredentials = for (
    realm ← sys.props.get("publish.repository.credentials.realm");
    host ← sys.props.get("publish.repository.credentials.host");
    user ← sys.props.get("publish.repository.credentials.user");
    password ← sys.props.get("publish.repository.credentials.password")
  ) yield Credentials(realm, host, user, password)

  val Creds: Seq[Credentials] = {
    val credFile = sys.props.get("publish.repository.credentials.file")
      .map(path => Credentials(new File(path)))

    val defaultCredentialsLocation = Some(Credentials(Path.userHome / ".ivy2" / ".credentials"))

    val personalCred = Some(Credentials(Path.userHome / ".sbt" / "credentials"))
    val deployCred = Some(Credentials(Path.userHome / ".sbt" / "credentials.deploy"))

    println(s"Reading credentials from $credFile")

    Seq(credFile, defaultCredentialsLocation,  personalCred, deployCred).flatten
  }


  val License =
    <licenses>
      <license>
        <name>Apache License Version 2.0</name>
        <url>https://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
        <comments />
      </license>
    </licenses>

  val OurScmInfo =
    Some(
      ScmInfo(
        url("https://github.com/yugabyte/spark-cassandra-connector"),
        "scm:git@github.com:yugabyte/spark-cassandra-connector.git"
      )
    )

  val OurDevelopers =
      <developers>
        <developer>
          <name>YugaByte Development Team</name>
          <email>info@yugabyte.com</email>
          <organization>YugaByte, Inc.</organization>
          <organizationUrl>https://www.yugabyte.com</organizationUrl>
        </developer>
      </developers>
    

}
