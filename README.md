# HearbeatLanSender

Send heartrate via UDP LAN from your android phone.

Download software [here](https://github.com/frto027/HeartbeatLanServer/releases/latest).

This app can be installed to your Oculus quest headset, and it will only send data to the headset itself instad of lan by default.

在[这里](https://github.com/frto027/HeartbeatLanServer/releases/latest)下载最新版本的apk。

```mermaid
graph TD;
    POLAR_H10[Polar H10]
    SMART_WATCH[Shart watch, broadcast heartrate]
    BLE_DEV[other BLE heartrate devs]
    PHONE[sender, phone. android apk <b>YOU ARE HERE</b>]
    BEATSABER[receiver, beatsaber quest mod]
    PCAPP[receiver, a webpage server]
    BEATSABER_PC[Beatsaber PC game with HRCounter mod]
    BROWSER[browser to view config and datas]
    EXAMPLE_PY[receiver, example.py]

    POLAR_H10--bluetooth-->PHONE;
    SMART_WATCH--bluetooth-->PHONE;
    BLE_DEV--bluetooth-->PHONE;
    PHONE--LAN-->EXAMPLE_PY;
    PHONE--LAN-->BEATSABER;
    PHONE--LAN-->PCAPP;
    PHONE--LAN-->...;

    PCAPP--127.0.0.1:xxxx-->BROWSER;
    PCAPP--HRCounter protocol-->BEATSABER_PC;
    PCAPP--a web ui <b>TODO</b> -->OBS

```

- [receiver, example.py](script/client_example.py)
- [receiver, a webpage server](https://github.com/frto027/HeartbeatLanClient) It is also possible to read heartbeat data from this receiver via http protocol.
- [receiver, beatsaber quest mod](https://github.com/frto027/HeartBeatLanClientBSQuest)

# Protocol

[UDP Protocol](Protocol.md)

[UDP协议文档](./script/Readme.md)
