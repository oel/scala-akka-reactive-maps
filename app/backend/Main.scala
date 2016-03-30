package backend

import akka.actor.ActorSystem
import actors.RegionManagerClient
import java.net.URL
import akka.cluster.Cluster
import akka.contrib.pattern.ClusterSharding
import com.typesafe.conductr.bundlelib.akka.{ConnectionContext, StatusService, Env}
import com.typesafe.config.ConfigFactory

/**
 * Main class for starting a backend node.
 * A backend node can have two roles: "backend-region" and/or "backend-summary".
 * The lowest level regions run on nodes with role "backend-region".
 * Summary level regions run on nodes with role "backend-summary".
 *
 * The roles can be specified on the sbt command line as:
 * {{{
 * sbt -Dakka.remote.netty.tcp.port=0 -Dakka.cluster.roles.1=backend-region -Dakka.cluster.roles.2=backend-summary "run-main backend.Main"
 * }}}
 *
 * If the node has role "frontend" it starts the simulation bots.
 */
object Main {
  def main(args: Array[String]): Unit = {
    val config = Env.asConfig
    val systemName = sys.env.getOrElse("BUNDLE_SYSTEM", "application")
    implicit val system = ActorSystem(systemName, config.withFallback(ConfigFactory.load()))

    if (Cluster(system).selfRoles.exists(r => r.startsWith("backend"))) {
      system.actorOf(RegionManager.props(), "regionManager")

      ClusterSharding(system).start(
        typeName = UserMetaData.shardName,
        entryProps = Some(UserMetaData.props),
        idExtractor = UserMetaData.idExtractor,
        shardResolver = UserMetaData.shardResolver)
    }

    if (Settings(system).BotsEnabled && Cluster(system).selfRoles.contains("frontend")) {
      val regionManagerClient = system.actorOf(RegionManagerClient.props(), "regionManagerClient")

      ClusterSharding(system).start(
        typeName = UserMetaData.shardName,
        entryProps = None,
        idExtractor = UserMetaData.idExtractor,
        shardResolver = UserMetaData.shardResolver)
      val userMetaData = ClusterSharding(system).shardRegion(UserMetaData.shardName)

      def findUrls(id: Int): List[URL] = {
        val url = Option(this.getClass.getClassLoader.getResource("bots/" + id + ".json"))
        url.map(url => url :: findUrls(id + 1)).getOrElse(Nil)
      }

      system.actorOf(BotManager.props(regionManagerClient, userMetaData, findUrls(1)))
    }

    implicit val cc = ConnectionContext()
    StatusService.signalStartedOrExit()
  }
}