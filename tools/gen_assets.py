#!/usr/bin/env python3
"""Genereaza texturile ORIGINALE ale modului (fara dependente externe).
Rulare: python3 tools/gen_assets.py  (din radacina proiectului)"""
import os, struct, zlib

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "blockcity", "textures")

def write_png(path, w, h, px):
    raw = b"".join(b"\x00" + b"".join(bytes(px[y][x]) for x in range(w)) for y in range(h))
    def chunk(t, d):
        c = struct.pack(">I", len(d)) + t + d
        return c + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)
    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "wb").write(data)

class Img:
    def __init__(self, w, h, fill=(0, 0, 0, 0)):
        self.w, self.h = w, h
        self.px = [[fill for _ in range(w)] for _ in range(h)]
    def rect(self, x0, y0, x1, y1, c):  # x1,y1 exclusive
        for y in range(y0, y1):
            for x in range(x0, x1):
                self.px[y][x] = c
    def dot(self, x, y, c): self.px[y][x] = c
    def save(self, rel): write_png(os.path.join(OUT, rel), self.w, self.h, self.px)

# ---------------- masina (256x128) ----------------
def car():
    t = Img(256, 128)
    body, glass, dark = (190, 30, 40, 255), (70, 110, 150, 255), (25, 25, 28, 255)
    t.rect(0, 0, 144, 50, body)            # caroserie (cuboid 28x6x36 la uv 0,0)
    t.rect(0, 52, 84, 78, glass)           # cabina (cuboid 22x6x20 la uv 0,52)
    t.rect(20, 52, 42, 72, body)           # plafon
    t.rect(150, 0, 174, 16, dark)          # roti
    # far fata = fata NORD a cuboidului: x 36..64, y 36..42
    t.rect(38, 37, 42, 40, (255, 240, 150, 255)); t.rect(58, 37, 62, 40, (255, 240, 150, 255))
    # stopuri = fata SUD: x 100..128
    t.rect(102, 37, 106, 40, (120, 0, 0, 255)); t.rect(122, 37, 126, 40, (120, 0, 0, 255))
    t.rect(36, 41, 64, 42, (40, 40, 40, 255))  # bara fata
    t.save("entity/car.png")

# ---------------- personaje (skin 64x64, layout clasic) ----------------
def person(name, skin, hair, shirt, pants, cap=None):
    t = Img(64, 64)
    shoe = (30, 30, 30, 255)
    t.rect(0, 0, 32, 16, skin)                              # cap
    t.rect(8, 0, 16, 8, hair); t.rect(24, 8, 32, 16, hair)  # par sus + spate
    t.rect(0, 8, 8, 10, hair); t.rect(16, 8, 24, 10, hair); t.rect(8, 8, 16, 10, hair)
    t.dot(10, 12, (20, 20, 30, 255)); t.dot(13, 12, (20, 20, 30, 255)); t.rect(11, 14, 13, 15, (120, 60, 60, 255))
    t.rect(16, 16, 40, 32, shirt)                           # corp
    for (x0, y0) in ((40, 16), (32, 48)):                   # brate
        t.rect(x0, y0, x0 + 16, y0 + 16, skin)
        t.rect(x0, y0, x0 + 16, y0 + 8, shirt)
    for (x0, y0) in ((0, 16), (16, 48)):                    # picioare
        t.rect(x0, y0, x0 + 16, y0 + 16, pants)
        t.rect(x0, y0 + 12, x0 + 16, y0 + 16, shoe)
    if cap:                                                 # strat "hat" = sapca
        t.rect(40, 0, 48, 8, cap); t.rect(32, 8, 64, 11, cap)
        t.rect(40, 11, 48, 12, (20, 20, 25, 255))           # cozoroc fata
        t.rect(20, 20, 24, 23, (230, 200, 60, 255))         # insigna
    t.save("entity/" + name + ".png")

def item_pistol():
    t = Img(16, 16); g, d = (60, 60, 66, 255), (30, 30, 34, 255)
    t.rect(3, 5, 13, 8, g); t.rect(3, 5, 13, 6, d)          # teava + carcasa
    t.rect(4, 8, 7, 13, d); t.rect(5, 8, 6, 12, (90, 60, 40, 255))  # mâner
    t.dot(9, 8, d); t.dot(9, 9, d)                          # trăgaci
    t.save("item/pistol.png")

def item_key():
    t = Img(16, 16); m, d = (200, 200, 210, 255), (110, 110, 120, 255)
    t.rect(2, 2, 7, 7, m); t.rect(3, 3, 6, 6, (0, 0, 0, 0))   # inel
    t.rect(6, 6, 13, 8, m); t.rect(10, 8, 12, 11, m); t.rect(7, 8, 8, 10, d)
    t.save("item/car_key.png")

car()
person("pedestrian_0", (232, 190, 160, 255), (60, 40, 25, 255), (60, 120, 200, 255), (50, 50, 70, 255))
person("pedestrian_1", (150, 100, 70, 255), (20, 15, 10, 255), (220, 180, 40, 255), (90, 70, 50, 255))
person("pedestrian_2", (240, 205, 180, 255), (200, 160, 70, 255), (190, 60, 90, 255), (40, 70, 110, 255))
person("pedestrian_3", (100, 65, 45, 255), (15, 10, 10, 255), (70, 160, 90, 255), (200, 200, 205, 255))
person("police", (225, 185, 155, 255), (50, 35, 25, 255), (25, 45, 105, 255), (20, 30, 70, 255), cap=(20, 35, 85, 255))
item_pistol(); item_key()
print("Texturi generate in", os.path.normpath(OUT))
