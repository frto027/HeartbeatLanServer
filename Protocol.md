# HeartBeatLan protocol

Protocol version 001

## Receiver


If you need get heartbeat data from the apk, following these steps. [example.py](script/client_example.py) is a possible simple impl.

- Listen UDP port `9965`, waiting for a package starts with `HeartBeatSenderHere001`, the null-terminator is not included.
- Remember the source address and port as`(addr, port)` of that UDP package. Stop listen UDP port if you don't need more senders. We are now have been paired, don't need send anything to verify your identity.
- Open a UDP at any port, such as `(0.0.0.0, port X)`. Send a udp package to the server address`(addr, port)`, the content is `HeartBeatRecHere001`, no need null terminator. Repeat this message in the future no more than 1 minute, e.g. repeat send this message per 20 seconds.
- Continues listen UDP package from your `(0.0.0.0, port X)`. If there are heart device datas, you will receive a package as the following format.

```
[name(string)] 0 [ble mac address(string)] 0 [heart(int32, big endian)]
```

## Sender

In summary, the sender does the following.

- If the broadcast UI toggle is switch on, broadcast `HeartBeatSenderHere001` to `255.255.255.255:9965`. The package is send from a random port, e.g. `(0.0.0.0,port S)`. 
- Keep listen UDP package from `(0.0.0.0, port S)`. If a received package starts with `HeartBeatRecHere001`, remember the source address and port(`(x.x.x.x, port X)` in the Receiver side), the last package receive time for every receiver is also recorded.
- If any heart rate data come, for example, from bluetooth device, forward it to every rememberd receiver device.
- Remove the receiver if the receiver don't send `HeartBeatRecHere001` to sender more than one minute.

### Localhost mode

In `This device only mode.` or `localhost mode`, the UDP package doesn't send to `255.255.255.255`, but to `127.0.0.1`. And the listen UDP socket doesn't bind to `0.0.0.0`, but `127.0.0.1`.

# Thread Module

The first version focus on usability. The protocol considers the LAN environment to be relatively secure. As long as the broadcast is turned on, everyone can find the sender and get heart data.
