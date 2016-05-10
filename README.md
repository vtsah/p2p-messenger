# p2p-messenger
A peer-to-peer messaging service

p2p-messenger is a groupchat application that allows users to message each other without having to talk to a central server after the initial connection occurs. It sends messages in a distributed manner, sending out each message in pieces and allowing recipients to download different pieces from other members of the chat. This reduces the number of copies the sender must deliver by distributing the load over the whole group.

## Installation

`git clone https://github.com/vtsah/p2p-messenger`

Compile with `make`

Create documentation with `make docs`, which will place html documentation in the `documentation` directory.

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

## Future Improvements

* Create a way to enable logging to record chats (especially long ones that roll off the top of the terminal and are lost permanently).
* Improve choking so that it's done on a tit-for-tat basis which would more efficiently distribute sent files across the chat.
* Cut out the server and set up chat entry so that log-in occurs by contacting an existing peer and downloading the active members from that peer. This would allow for chats where anyone in the chat can act like a gateway by publicizing their IP address. Alternatively, one could set up chats that only permit entry if the new user knows an existing member.
* Optimize the piece sizes to find the most efficient balance between smaller pieces and fewer packets to download messages quickly.
* Create timeouts to resend packets in case they are lost over UDP.

## Problems Encountered

One issue we discovered at the last moment and had difficulty debugging was the IP addresses. At first the peer's IP addresses were as seen by the server, but this didn't work when the client and server were run on the same machine. Then we used `InetAddress.getLocalHost()` which worked for Mac but not for Linux clients on different machines. Then we looked in Network Interfaces table, but on the zoo the result was an unusable VM address. Finally we openned up a TCP connection to google.com in order to find the public IP of a client.

A bug that only showed up when sending multiple messages simultaneously involved not freeing up unchoke slots.
Another much more difficult problem also involved unchoke slots. Originally data was transmitted over TCP synchronously, and despite lots of synchronization we were unable to stop a peer from requesting after it had been choked, and the refused TCP connection threw exceptions. To solve this problem we transitioned completely to UDP for sending data.
