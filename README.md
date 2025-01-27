# Single-threaded simple event loop

An exercise to implement echo server with simple event loop and Java NIO.

## Protocol

A simple binary protocol over TCP:

- first 4-byte integer - length of message
- actual message

![](images/protocol.png)

## Steps to implement event loop

> Use Java NIO in non-blocking manner (configure your channels to be non-blocking)

### 1. Start listening on `Constants.PORT` for incoming TCP connections

Open selector and server socket channel, register channel with selector to listen to incoming
connections (`SelectionKey.OP_ACCEPT`)

<details>
  <summary>Step-by-step instruction</summary>

#### 1. Create selector to handle multiple channels

```
Selector selector = Selector.open();
```

#### 2. Open ServerSocketChannel in a non-blocking manner on `Constants.PORT`

It is responsible for incoming connections

```
ServerSocketChannel server = ServerSocketChannel.open();

server.configureBlocking(false); // Make it non-blocking
server.bind(new InetSocketAddress(Constants.PORT)); // Specify address to listen to
```

#### 3. Register server channel with created selector to listen to `SelectionKey.OP_ACCEPT`

```
server.register(selector, SelectionKey.OP_ACCEPT); // Make selector listen to incoming connections
```

</details>

### 2. Listen for ready channels in an infinite loop

Use `selector.select()` to check if there are ready channels. Get ready channels with `selector.selectedKeys()`
and for now just iterate over them.

> `select()` may possibly return 0. Don't forget to handle it.

> It is important to remove selected key after processing.
> Selector does not remove the SelectionKey from the set by itself.

<details>
  <summary>Step-by-step instruction</summary>

#### 1. `select()` ready channels inside infinite loop

if `select()` returns `0` - skip this iteration

```
while (true) {
    int ready = selector.select();
    if (ready == 0) {
        continue;
    }
    
    // Iterate and process keys here
}
```

#### 2. Iterate over selected keys (ready channels)

Obtain an iterator from `selector.selectedKeys()`. Don't forget to remote key at the end of iteration loop

```
 Iterator<SelectionKey> keysIterator = selector.selectedKeys().iterator();
 while (keysIterator.hasNext()) {
     SelectionKey key = keysIterator.next();

     // Process the key here

     keysIterator.remove();
 }
```

</details>

### 3. Handle new client connection

Based on `key.isXXX()` methods decide how to process a channel. You're
interested in key that is acceptable (that covers incoming connection)

Use `acceptClient()` with appropriate parameters. Reuse created earlier selector (we'll use it for listening
reading & writing later)

> `key.channel()` returns the channel for the key, which can be downcasted to the channel you
> need to work with (`ServerSocketChannel` or `SocketChannel`)

#### acceptClient() implementation

Accept connection and configure client in a non-blocking way. Register client channel with selector to listen
to read and write (`OP_READ`, `OP_WRITE`)

> Don't forget to maintain opened connection map

<details>
  <summary>Step-by-step instruction</summary>

#### 1. Use `server.accept()` to accept a connection

It returns `SocketChannel` i.e. client.

#### 2. Configure client to work in a non-blocking way

Same way we did for server

#### 3. Register client to listen to read and write

Use `client.register()`, pass selector and both ops: `SelectionKey.OP_READ`, `SelectionKey.OP_WRITE`

> Use bitwise OR to combine options

#### 4. Maintain opened connection map

`client.register()` returns selection key for the channel in the selector. Use it to add new
client connection to `clients` map.

</details>

### 4. Implement read

Inside the keys processing loop use `key.isXXX()` to check if channel is ready to be read.
Call `handleRead()`

#### handleRead() implementation

Use connection with I/O buffers from `clients` and read data from client to incoming buffer.

> Remember you can and should downcast `key.channel()` to the channel you work with (`SocketChannel` in this case)

> Don't forget to handle EOF of client. Close connection in this case (`closeClient()`)

Invoke `parseRequest`

<details>
  <summary>Step-by-step instruction</summary>

#### 1. Downcast channel to needed one

```
SocketChannel client = (SocketChannel) key.channel();
```

#### 2. Obtain connection from clients

#### 3. Use `client.read()` to read ready data to `connegtion.incoming` buffer

`client.read()` returns number of bytes read. `-1` means EOF, we can interpret it as closed client.
`closeClient()` in this case and return.

#### 4. Invoke `parseRequest()`

</details>

### 5. Implement parseRequest

Using `connection.incoming` buffer read message size (first 4 bytes) and message itself if available.

> Remember that buffer can contain incomplete data. In this case do nothing, it will be read on next loop iterations

After reading full message clear the buffer and `writeResponse()` by passing read message (it's echo server ;))

<details>
  <summary>Step-by-step instruction</summary>

#### 1. Read message length if you have enough data, return otherwise

If you have less than 4 bytes read (`incoming.position() < 4`) - return. It will be read rest of the data
on the next loop iterations.

Use absolute `get` to read message length (`incoming.getInt(0)`)

#### 2. Check if you have enough data to read full message, return otherwise

`incoming.position() - 4 < messageSize` - we don't have full data yet, return

#### 3. Read message

```
new String(incoming.array(), 4, msgSize);
```

#### 4. Clear the buffer

#### 5. Call `writeResponse()` by passing read message

</details>

### 6. Implement write response

Use `connection.outgoing` buffer to write message according to the protocol (4 bytes len|msg).

> Don't forget to `outgoing.flip()` after writing

> Use `connection.wantWrite` as an intention to write

### 7. Implement handleWrite()

Inside the keys processing loop use `key.isXXX()` to check if channel is ready to be written.
Call `handleWrite()`

#### handleWrite() implementation

Use connection with I/O buffers from `clients` and outgoing buffer to write to client.
Set `connection.wantWrite` to false and clear the buffer afterward.

> Channel is highly likely to be always ready for write. That's why `connection.wantWrite` is added. Use it as a
> condition
> to write. If it's false simply return

> It is possible that not all data are written. Handle this case, don't reset `wantWrite` flag,
> then it will write the rest on next iterations

<details>
  <summary>Step-by-step instruction</summary>

#### 1. Verify we want to write to channel

Obtain connection from `clients` and check `wantWrite` is true. Return otherwise

#### 2. Downcast channel to needed one

```
SocketChannel client = (SocketChannel) key.channel();
```

#### 3. Use `client.write()` to write data from `connection.outgoing` buffer

Use `connection.getOutgoing().hasRemaining()` to check if all data is written to client

#### 4. Clear buffer and reset `wantWrite` if all data are written

</details>