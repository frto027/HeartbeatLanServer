# HearbeatLanServer

Send heartrate via UDP LAN from your android phone.

TODO List.

- English support...
- An Icon to hint user the heartrate is sending...
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
│BLR Heartrate Devices...├─┘
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


[webpage server](#)

# Protocol

[UDP协议文档](./script/Readme.md)
The UDP protocol is easy to use. At least I think it is...