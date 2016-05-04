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

## TODO

* KEEPALIVE messages between clients and with server to detect clients disconnecting.
* Requesting of old messages, so a new peer can download all previously sent messages
    * Possibly the available messages should be sent in response to a "Business Card"
* Send files
* Resend packets like "HAVE" and "UNCHOKE" after a timeout if there is no response (possibly have a timeout on unchoke slots as well)
* Print out messages in chronological order.
* Make a UI for displaying Chat Transcript separate from input and in order, and for choosing file for attachment and for downloading files to given path.

### Possible additional features

* End-to-end Cryptography
* Passwords
* Tit-for-tat: selective unchoking strategy
* UDP punching: for breaking through NAT
