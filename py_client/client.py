# -*- coding: utf8 -*-
import socket
from pipe_pb2 import Route
from encoder_decoder import LengthFieldProtoEncoder
from threading import Thread
import urllib2
from time import time

# This is currently port and IP oof Nginx server
TCP_IP = 'http://127.0.0.1'
TCP_PORT = 8020
BUFFER_SIZE = 1024


class MessageClient:
    def __init__(self, buffer_size):
        self.buffer_size = buffer_size

        self.encoder = LengthFieldProtoEncoder()

        self.s = None

        #todo(Kannu: ) should fail if the http connection fails here, then should not continue
        socket_address = self.__get_server_socket_address()
        parts = socket_address.split(":")
        self.host = parts[0]
        self.port = int(parts[1])

        self.connect()

    def __get_server_socket_address(self):
        socket_address = urllib2.urlopen(TCP_IP + ":" + str(TCP_PORT)).read()
        return socket_address

    def connect(self):
        self.s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.s.connect((self.host, self.port))
        print "client connected to server: " + self.__get_server_path()

        # listen to incoming data on a new thread
        thread = Thread(target = self.listen)
        thread.start()

    def listen(self):
        while True:
            try:
                data = self.s.recv(self.buffer_size)
                if (len(data) == 0):
                    print "connection has been closed with: " + self.__get_server_path()
                    self.close()
                    break

                print "data length: ", len(data)
                print "data reply: ", data
            except socket.timeout:
                self.close()
                print "connection timed out with: " + self.__get_server_path()
                break

    def ping(self):
        r = Route()
        r.path = "/ping"
        r.payload = "ping request"

        self.send(r)


    def post_message(self, message):
        r = Route()
        r.path = "/message"
        r.payload = message

        self.send(r)

    def send(self, route):
        message = self.encoder.encode(route)
        self.s.send(message)

    def close(self):
        self.s.close()
        self.s = None

    def __get_server_path(self):
        return self.host + ":" + str(self.port)


    # cli for message client #
    def __get_usage(self):
        return "message client supports following commands - \n" + \
               "ping\n" + \
               "send 'message'\n" + \
               "close\n"

    def start_cli(self):
        print self.__get_usage()
        while True:
            cmd = raw_input("Enter your command:\n")
            if not self.s:
                print "sorry socket not connected, please try again"
            else:
                self.ping()




mc = MessageClient(BUFFER_SIZE)
mc.start_cli()
