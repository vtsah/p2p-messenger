# p2p-messenger
A peer-to-peer messaging service

p2p-messenger is a groupchat application that allows users to message each other without having to talk to a central server after the initial connection occurs. It sends messages in a distributed manner, sending out each message in pieces and allowing recipients to download different pieces from other members of the chat. This reduces the number of copies the sender must deliver by distributing the load over the whole group.

## Installation

`git clone https://github.com/vtsah/p2p-messenger`

Compile with `make`

## Running the application

Start by running a server with `java Server port`

Then run any number of clients with the command `java Client server-ip server-port chat-name username`, with these arguments:

* `server-ip` is the IP address of the server (you can run a client on the same machine as the server using localhost)
* `server-port` is the `port` from the server command
* `chat-name` identifies which chat to join (can be any string but will determine which clients talk to each other)
* `username` identifies a user when the other clients print out messages sent from this client

Then start typing in the clients' standard inputs. Each line will be distributed to everyone else in the chat.

To send files, type:
`/sendfile /path/to/file.txt`

The path above can be either relative to current working directory, or absolute.

## Implementation Details

The mechanism which distributes chat messages and files to all peers is modeled after the Bittorrent protocol.
Message/file data is broken into 5-byte "pieces." The piece is advertized with HAVE messages. Peers express interest with INTERESTED packets, at which point they are either CHOKE'd or UNCHOKE'd, based on the available unchoke slots. An unchoked peer can send a REQUEST, causing DATA to be returned (see `ControlPacket.java`).
When the last piece of a message, written by `<AUTHOR>`, is received from `<SENDER>`, it is printed out in the format `(<TIMESTAMP>) <AUTHOR>: [(via <SENDER>)] <MESSAGE>`.

The clients also track which of the peers are still in the group using KEEPALIVE messages. Every time a client receives a message, it checks to see if 20 seconds have passed since the last round of KEEPALIVEs. If it has been 20 seconds, the client cycles through the peers and checks if any of them haven't responded to the KEEPALIVE messages with an ALIVE message. Any that haven't responded thrice are marked dead and removed from the chat. Then, the client sends out another round of KEEPALIVE messages to the list of peers.
