"""Простой RCON-клиент для локального тестового сервера: python tools/rcon.py "команда" ..."""
import socket
import struct
import sys


def packet(sock, req_id, kind, body):
    data = struct.pack('<ii', req_id, kind) + body.encode('utf-8') + b'\x00\x00'
    sock.sendall(struct.pack('<i', len(data)) + data)
    size = struct.unpack('<i', sock.recv(4))[0]
    buf = b''
    while len(buf) < size:
        buf += sock.recv(size - len(buf))
    return buf[8:-2].decode('utf-8', 'replace')


def main():
    with socket.create_connection(('127.0.0.1', 25599), timeout=10) as s:
        packet(s, 1, 3, 'litrpgtest')
        for cmd in sys.argv[1:]:
            print('>', cmd)
            print(packet(s, 2, 2, cmd))


if __name__ == '__main__':
    main()
