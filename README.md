# HearbeatLanServer

Send heartrate via UDP LAN from your android phone.

Download software [here](https://github.com/frto027/HeartbeatLanServer/releases/latest).

在[这里](https://github.com/frto027/HeartbeatLanServer/releases/latest)下载最新版本的apk。

TODO List.

- Protocol english document...

```
┌─────────┐   Bluetooth(BLE)
│POLAR H10├────────────────┐
└─────────┘                │         YOU ARE HERE
                           │            │
┌───────────┐              │   ┌────────▼────┐
│Smart watch├──────────────┼──►│Android Phone├───────►UDP Package
└───────────┘              │   └─────────────┘        via WLAN
                           │
┌────────────────────────┐ │
│BLE Heartrate Devices...├─┘
└────────────────────────┘
```

there are some application. see the graph below.


```
                                        ┌─────────────────────┐
                                        │example python client│
                                        └────────────▲────────┘
┌─────────┐   Bluetooth(BLE)                         │
│POLAR H10├────────────────┐                         │
└─────────┘                │         YOU ARE HERE    │
                           │            │            │
┌───────────┐              │   ┌────────▼────┐       │
│Smart watch├──────────────┼──►│Android Phone├───────┤UDP Package
└───────────┘              │   └─────────────┘       │via WLAN
                           │                         │
┌────────────────────────┐ │                         │
│BLR Heartrate Devices...├─┘                         │
└────────────────────────┘                           │
                                         ┌───────────▼──┐
                                         │webpage server│
                                         └──────────────┘
```
[example python client](script/client_example.py)


[webpage server](https://github.com/frto027/HeartbeatLanClient)

# Protocol

[UDP协议文档](./script/Readme.md)
The UDP protocol is easy to use. At least I think it is...