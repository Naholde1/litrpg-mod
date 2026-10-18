"""Минимальная запись/чтение PNG без сторонних библиотек (RGBA, 8 бит)."""
import struct
import zlib


def write_png(path, pixels, width, height):
    """pixels: список строк, каждая — список (r, g, b, a)."""
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        for x in range(width):
            raw.extend(pixels[y][x])

    def chunk(tag, data):
        c = struct.pack('>I', len(data)) + tag + data
        return c + struct.pack('>I', zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b'\x89PNG\r\n\x1a\n'
    png += chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(bytes(raw), 9))
    png += chunk(b'IEND', b'')
    with open(path, 'wb') as f:
        f.write(png)


def blank(width, height):
    return [[(0, 0, 0, 0) for _ in range(width)] for _ in range(height)]


def hex_rgba(value, alpha=255):
    return ((value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF, alpha)


def scale(pixels, factor, bg=(40, 44, 52, 255)):
    """Увеличение для превью: прозрачное заливается фоном."""
    out = []
    for row in pixels:
        line = []
        for p in row:
            q = p if p[3] > 0 else bg
            line.extend([q] * factor)
        for _ in range(factor):
            out.append(list(line))
    return out
