val scala3Version = "3.3.0-RC2"

lazy val root = project
  .in(file("."))
  .settings(
    organization                             := "com.example",
    name                                     := "loom-actors",
    version                                  := "0.1.0-SNAPSHOT",
    scalaVersion                             := scala3Version,
    libraryDependencies += "com.github.rssh" %% "shim-scala-async-dotty-cps-async" % "0.9.15",
    libraryDependencies += "org.scalameta"   %% "munit"                            % "0.7.29" % Test,
    javacOptions ++= Seq("-source", "19", "-target", "19"),
    watchTriggeredMessage := Watch.clearScreenOnTrigger,
    watchBeforeCommand    := Watch.clearScreen
  )
