# NBot 3.0

## Features
* Activity monitoring (Activity.scala). Responds to `!active <num>` with a NOTICE containing the users that were active within `num` minutes ago.
* Nicklist monitoring (Nicklist.scala). Can also give the user a list of nicks in the channel that are also in a predefined list. For more see Nicklist and Factoid Handlers
* Factoids (Factoids.scala). Can gather factoids from multiple sources and PRIVMSG them to the channel
* MFK Game (Mfk.scala). Takes 3 random nicks from a nicklist using the same syntax as nicklist queries.
* Tell (Tell.scala). Syntax: `!tell <nick> <msg>`. The message will be delivered to the user when they are seen online.

## Configuration
NBot can be configured with the config.json file. Options should be pretty self-explanatory. To reload the config file, send a PRIVMSG anywhere with the message `reload-config`

## Extending
### New plugins/behaviors
Make a new class that extends akka.Actor. Messages from the server are sent as raw strings, but you can pattern match them with the case classes from IRCMessage. To send a message to the server, simply send a string back to the client actor. The trailing \\r\\n is not necessary. An example receive method:

	def receive = {
		case rawmsg @ Privmsg(nick, target, msg) =>
			// the variables nick, target, and msg are created
			// rawmsg is the raw IRC line sent from the server
			sender ! s"PRIVMSG $target :Message from $nick: $msg"

		case _ => {}
	}
To add this plugin, spawn the actor from IRCClient. All children of IRCClient receive messages from the server.

### Reading from the config file
To get updates for a key for each config reload, send a `SubscribeToKey(key:String)` message to the `config` actor. Every time the key is changed, a `KeyUpdate(key:String, value:Any)` message will be sent back to that actor.

### Nicklist and Factoid Handlers
Nicklist and Factoid handlers are both instances of CommandDelegate. To add one, make a new class that extends that trait, and override the tryCmd method. For examples, see FactoidFile.scala, or NickGroupFile.scala.

## Running and Compiling
NBot uses sbt. To run, use the `run` task. To create a [one-jar](https://github.com/sbt/sbt-onejar), use the `one-jar` task.
