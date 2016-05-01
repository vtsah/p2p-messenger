# p2p-messenger
A peer-to-peer messaging service

Compile with `make`

Start by running a server with `java Server port`

Then run any number of clients with the command `java Client server-ip server-port chat-name username`, with these arguments:

* `server-ip` is the IP address of the server
* `server-port` is the `port` from the server command
* `chat-name` identifies which chat to join (can be any string but will determine which clients talk to each other)
* `username` identifies a user when the other clients print out messages sent from this client

Then start typing in the clients' standard inputs. Each line will be distributed to everyone else in the chat.

