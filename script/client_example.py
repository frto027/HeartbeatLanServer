import socket
import datetime

SERVER_PORT = 9965

SERVER_MESSAGE = b"HeartBeatSenderHere001"
CLIENT_MESSAGE = b"HeartBeatRecHere001"


############ find server ip and port ###############
sock = socket.socket(socket.AF_INET, # Internet
                     socket.SOCK_DGRAM) # UDP
sock.bind(("0.0.0.0", SERVER_PORT))
(msg, (server_ip, server_port)) = sock.recvfrom(1024)
sock.close()

if msg != SERVER_MESSAGE:
    print("not a valid server package:", server_ip, server_port)
    print(msg)
    exit(-1)

print("server at ", server_ip, server_port)

############# access data from server ###############
sock = socket.socket(socket.AF_INET, # Internet
                     socket.SOCK_DGRAM) # UDP

dev_infos = dict()
heart_beat_package_sent_time = datetime.datetime.now() - datetime.timedelta(seconds=1000)

def print_infos():
    import os
    os.system("cls")
    print("last heartbeat package: ", heart_beat_package_sent_time.isoformat())
    for (name, addr, heartbeat, time) in dev_infos.values():
        print("%20s" % name, "|", addr, "|", "% 3d" % heartbeat, "|", time.isoformat())

while True:
    ##### tell server where are you, you need repeat it no more than one minutes again #####
    if (datetime.datetime.now() - heart_beat_package_sent_time).total_seconds() > 20:
        sock.sendto(CLIENT_MESSAGE, (server_ip, server_port))
        heart_beat_package_sent_time = datetime.datetime.now()
        # print("send alive package")

    ##### receive and parse data from server #####
    (msg, (server_ip, server_port)) = sock.recvfrom(1024)
    ####################### msg format ##########################
    # name '\0' bluetooth_device_mac_addr '\0' heartbeat(4byte) #
    #############################################################
    i_name = 0
    i_addr = msg.index(b'\0')+1
    i_heartbeat = msg.index(b'\0', i_addr) + 1

    name = msg[:i_addr-1].decode()
    addr = msg[i_addr:i_heartbeat-1].decode()
    heartbeat = int.from_bytes(msg[-4:])

    dev_infos[addr] = (name, addr, heartbeat, datetime.datetime.now())
    print_infos()