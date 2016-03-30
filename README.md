## Scala-Akka - Reactive Maps

---

The Git repo contains source code of a Scala application using Akka actors on the Play framework.  It's part of a platform evaluation process to understand certain techniques illustrated in the reactive-maps sample application from Lightbend (https://www.lightbend.com/activator/template/reactive-maps).  A majority of the code is the resulting source code of the sample application after going over the application development walk-thru, except for a couple of changes as follows: 

1. The deprecated Akka persistence interface EventsourcedProcessor is replaced with trait PersistenActor plus an overridden PersistenceId method in app/backend/UserMetaData.scala.

2. The ClusterSharding related code is moved from class BackendActors into class UserMetaDataProvider in app/actors/Actors.scala to address an injector creation problem.

The final step for custom journal setup in the sample application using Leveldb crashes at run-time apparently due to some Guava dependency conflict and is not included in this source code.

##### To run the application, simply proceed as follows:

1. Git-clone the repo to a local disk
2. Run './activator ui' from within the project root (Activator's UI will be fired off in a web browser)
3. Click 'compile', then 'run' from within the Activator's UI
4. Open a web browser and go to http://localhost:9000/

---
