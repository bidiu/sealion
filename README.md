#### sealion
a distributed in-memory database implemented with Netty, currently at its super early stage, with very limited features and configurations, and super unstable. I built it mainly in purpose of learning.

### Main Features
#### In-memory
All data is stored in memory in a form of key-value pairs. And sealion supports unstructured binary data. In order to boost performance, memory that is used to store key-values is direct buffer from Java NIO. And those continuous memory areas are pooled when freed. Once receiving key-value pairs from clients, server stores them directly into direct buffer, no more copy from outside JVM to inside needed.

Server uses LRU algorithm to sweep away key-values when no more specified memory available.

There's no persistence feature.

#### TTL and CAS
sealion supports TTL (time-to-live). When setting key-values into sealion cluster, users can specify data's life time. sealion adopts a lazy expiration algorithm to improve performance. Based on this feature, sealion can be used as cache.

CAS can record key's version. Every time being updated, key's version number is increased by 1 (CAS is a string actually). Concurrent synchronization and optimistic lock can be implemented with this feature.

#### Client
Built on top of a super minimalist protocol, a command line interface is available to interact with sealion clusters. However, functions of cli are very limited so far. There is also a Java library available to interact with sealion. With this library, data can be set to cluster synchronously or asynchronously.

#### Distribution (Load Balance)
All functions of distribution are implemented at client side. There is not any communication between servers. With consistent hash algorithm, all key-value pairs are evenly distributed to servers in a single cluster.

There is not any failover feature supported.

#### Throughput
Network parts are implemented with Netty, which is a nonblocking, event-driven, asynchronous network I/O library, so hopefully sealion will serve more clients with less resources.

#### Dashboard
There is a basic Web-based dashboard of each server instance whereby users can monitor its realtime status (memory usage, task queue size, network traffic, CPU, etc.).

### Basic Usage
#### Start & Stop
```
./bin/sealion_srv -p 1113 -m 256
```
By default, sealion instance will listen on port 1113, and dashboard's port always is `${sealion_port} + 1`. If not specified, sealion will set its memory limit with 256 megabytes.

To stop server, just press `Ctrl + C`.

The cli's functions are very limited, thus being ignored here.

#### Java API
- Client Instantiation and Close
```
    import io.sunhe.sealion.client.SeaLionClient;

    // the number after port is server's weight against
    // other servers in the cluster, can be ignored
    String servers = "localhost:1113:3 192.168.1.34:1113";
    SeaLionClient client = new SeaLionClient(servers);

    // close the underlaying connections
    client.close();
```
The client just created above is **thread-safe**.

- Set Operations
```
    // set a string value
    // char1 is key, Toro is value
    client.setString("char1", "Toro");

    // set a string with TTL
    client.setString("char2", "Doctor Strange", timestamp);

    // you can also specify it'll expire in 30 minutes
    client.setString("char2", "Doctor Strange", 30);

    // asynchronously set a string, it returns a ChannelFuture,
    // with which you can attach callback,
    // poll its status,
    // or just discard it if you don't care whether the operation succeeds
    ChannelFuture future = client.setStringUnsafe("char3", "Havok");

    // set raw binary data
    client.setBytes("img1", image.bytes());

    // set a bytes only if CAS matched
    client.setString("img2", image.bytes(), timestamp, cas);
```

- Get Operations
```
    // get a raw binary data
    byte[] stream = client.getBytes("img1");

    // get a string only if CAS matched
    String value = client.getString("key", "I'm CAS");

    // you can also get the value and its CAS at the same time,
    // note that there's no 'getStringAndCas',
    // you have to convert yourself from bytes, as following
    SeaLion response = client.getStringAndCas("key");
    ByteBuf data = client.getData();
    String cas = response.getCas();
    // if data is text
    String text = data.toString();

    // get only the version
    // if key doesn't exist (or has expired), you get NULL
    String version = client.getCAS("key");
```

- Delete Operations
```
    // delete a key-value pair
    client.delete("key");

    // delete a key-value only if version matched
    client.delete("key", cas);

    // asynchronously delete a key-value
    // again, future can be ignored, if not needed
    ChannelFuture future = client.deleteUnsafe("key");
```

#### Performance Testing Tool
There is also a very simple performance testing tool which can simulate different scenarios of access to sealion servers. For example, users can specify following:
- hit/miss ratio
- read/write ratio
- synchronous/asynchronous mode
- data average size (in bytes)
- number of connections
- number of work thread per connection
- data expiry (in minutes)

For more information about how to use it:
```
./bin/sealion_pr --help
```

Once running, users can use dashboard to view its status.

#### Dashboard
Once a sealion instance started, it will also start a dashboard module listening on `${sealion_port + 1}`. Users can open browser to visit the following url (suppose sealion instance is on local machine and listening on port 1113):

    http://localhost:1114/web/html/dashboard.html

to monitor its realtime status.

Some of the screenshots:
![dashboard 1](https://raw.githubusercontent.com/bidiu/sealion/refactor/src/main/resources/web/img/screenshot%201.png?raw=true)

![dashboard 2](https://raw.githubusercontent.com/bidiu/sealion/refactor/src/main/resources/web/img/screenshot%202.png?raw=true)

![dashboard 3](https://raw.githubusercontent.com/bidiu/sealion/refactor/src/main/resources/web/img/screenshot%203.png?raw=true)
