"""Generates the planet textures, city lights and icons for Astral Ascension Renewed.

Everything is procedural and seeded, so running it again gives the same files.
Noise is evaluated on the unit sphere in 3D, so the equirectangular maps have no seam and no
pinching at the poles.

    python tools/make_textures.py            # writes into graphics/
    python tools/make_textures.py --preview  # also writes preview renders into build/texture-preview/
"""
import argparse
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
W, H = 2048, 1024


# ----------------------------------------------------------------------------- noise

def sphere(w, h):
    lon = (np.arange(w, dtype=np.float32) + 0.5) / w * 2 * np.pi - np.pi
    lat = np.pi / 2 - (np.arange(h, dtype=np.float32) + 0.5) / h * np.pi
    lon, lat = np.meshgrid(lon, lat)
    return np.cos(lat) * np.cos(lon), np.cos(lat) * np.sin(lon), np.sin(lat)


def _hash(ix, iy, iz, seed):
    with np.errstate(over="ignore"):
        h = (ix.astype(np.uint32) * np.uint32(73856093)) ^ (iy.astype(np.uint32) * np.uint32(19349663)) \
            ^ (iz.astype(np.uint32) * np.uint32(83492791)) ^ np.uint32((seed * 2654435761) & 0xFFFFFFFF)
        h ^= h >> np.uint32(13)
        h *= np.uint32(0x5BD1E995)
        h ^= h >> np.uint32(15)
        h *= np.uint32(0x27D4EB2D)
        h ^= h >> np.uint32(16)
    return h


def _u(h, salt):
    with np.errstate(over="ignore"):
        g = h ^ np.uint32(salt)
        g *= np.uint32(0x9E3779B1)
        g ^= g >> np.uint32(16)
    return (g & np.uint32(0xFFFFFF)).astype(np.float32) / np.float32(16777216.0)


def value_noise(p, freq, seed):
    x, y, z = (c * freq for c in p)
    ix, iy, iz = np.floor(x), np.floor(y), np.floor(z)
    fx, fy, fz = x - ix, y - iy, z - iz
    sx, sy, sz = (f * f * (3 - 2 * f) for f in (fx, fy, fz))
    ix, iy, iz = ix.astype(np.int64), iy.astype(np.int64), iz.astype(np.int64)

    def v(dx, dy, dz):
        return _u(_hash(ix + dx, iy + dy, iz + dz, seed), 0x1234)

    x00 = v(0, 0, 0) * (1 - sx) + v(1, 0, 0) * sx
    x10 = v(0, 1, 0) * (1 - sx) + v(1, 1, 0) * sx
    x01 = v(0, 0, 1) * (1 - sx) + v(1, 0, 1) * sx
    x11 = v(0, 1, 1) * (1 - sx) + v(1, 1, 1) * sx
    y0 = x00 * (1 - sy) + x10 * sy
    y1 = x01 * (1 - sy) + x11 * sy
    return y0 * (1 - sz) + y1 * sz


def fbm(p, freq, octaves, seed, gain=0.5):
    total, amp, norm = 0.0, 1.0, 0.0
    for i in range(octaves):
        total = total + amp * value_noise(p, freq * (2 ** i), seed + 101 * i)
        norm += amp
        amp *= gain
    return total / norm


def cellular(p, freq, seed):
    """F1, F2, the id of the nearest feature point and its position (Worley noise)."""
    x, y, z = (c * freq for c in p)
    ix, iy, iz = (np.floor(c).astype(np.int64) for c in (x, y, z))
    f1 = np.full(x.shape, np.inf, dtype=np.float32)
    f2 = np.full(x.shape, np.inf, dtype=np.float32)
    cell = np.zeros(x.shape, dtype=np.uint32)
    best = [np.zeros(x.shape, dtype=np.float32) for _ in range(3)]
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            for dz in (-1, 0, 1):
                cx, cy, cz = ix + dx, iy + dy, iz + dz
                h = _hash(cx, cy, cz, seed)
                fx, fy, fz = cx + _u(h, 1), cy + _u(h, 2), cz + _u(h, 3)
                d = (x - fx) ** 2 + (y - fy) ** 2 + (z - fz) ** 2
                closer = d < f1
                f2 = np.where(closer, f1, np.minimum(f2, d))
                cell = np.where(closer, h, cell)
                f1 = np.where(closer, d, f1)
                for i, f in enumerate((fx, fy, fz)):
                    best[i] = np.where(closer, f, best[i])
    return np.sqrt(f1), np.sqrt(f2), cell, tuple(b / freq for b in best)


def smooth(e0, e1, v):
    t = np.clip((v - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def hubs(p, count, seed, radius):
    """Angular distance (in hub radii) to the nearest of a few great arcology hubs."""
    rng = np.random.default_rng(seed)
    v = rng.normal(size=(count, 3))
    v /= np.linalg.norm(v, axis=1, keepdims=True)
    best = np.full(p[0].shape, np.inf, dtype=np.float32)
    for a, b, c in v:
        d = np.arccos(np.clip(p[0] * a + p[1] * b + p[2] * c, -1, 1)) / radius
        best = np.minimum(best, d)
    return best


# ----------------------------------------------------------------------------- city

def _line(g, width):
    """1 on the grid lines of coordinate g, falling to 0 at the given half-width (in cells)."""
    return 1 - smooth(width * 0.5, width, np.abs(g - np.round(g)))


def city_layers(p, seed):
    """Districts, each with its own street grid at its own angle, plus highways and hubs."""
    d1, d2, dist_id, c = cellular(p, 5.5, seed)
    # Tangent frame at each district's centre, turned by the district's own angle.
    cn = np.stack(c, -1)
    cn /= np.linalg.norm(cn, axis=-1, keepdims=True) + 1e-9
    up = np.broadcast_to(np.array([0.0, 0.0, 1.0], np.float32), cn.shape)
    e1 = np.cross(up, cn)
    weak = np.linalg.norm(e1, axis=-1, keepdims=True) < 1e-3
    e1 = np.where(weak, np.cross(np.broadcast_to(np.array([1.0, 0, 0], np.float32), cn.shape), cn), e1)
    e1 /= np.linalg.norm(e1, axis=-1, keepdims=True)
    e2 = np.cross(cn, e1)
    theta = (_u(dist_id, 11) * np.pi / 2)[..., None]
    a1 = np.cos(theta) * e1 + np.sin(theta) * e2
    a2 = -np.sin(theta) * e1 + np.cos(theta) * e2
    P = np.stack(p, -1)
    du = np.sum(P * a1, -1)
    dv = np.sum(P * a2, -1)
    del cn, up, e1, e2, a1, a2, P

    spacing = 0.016 + 0.012 * _u(dist_id, 12)          # block size in radians, varies by district
    gu, gv = du / spacing, dv / spacing
    street = np.maximum(_line(gu, 0.13), _line(gv, 0.13))
    avenue = np.maximum(_line(gu / 4, 0.045), _line(gv / 4, 0.045))
    di = dist_id.astype(np.int64)
    block_id = _hash(np.floor(gu).astype(np.int64), np.floor(gv).astype(np.int64), di, seed + 5)
    k = 3 + (_u(dist_id, 13) * 2).astype(np.int64)       # 3 or 4 buildings across a block
    lu, lv = gu * k, gv * k
    alley = np.maximum(_line(lu, 0.16), _line(lv, 0.16)) * (1 - street)
    lot_id = _hash(np.floor(lu).astype(np.int64), np.floor(lv).astype(np.int64),
                   block_id.astype(np.int64), seed + 6)

    highway = 1 - smooth(0.004, 0.022, d2 - d1)
    grain = fbm(p, 420.0, 2, seed + 21)
    zone = fbm(p, 3.0, 3, seed + 31)                       # large-scale old towns vs industrial belts
    parks = fbm(p, 14.0, 3, seed + 41)
    nightlife = fbm(p, 9.0, 3, seed + 51)                  # where the lights are brightest at night

    hub_d = hubs(p, 16, seed + 3, 0.12)
    hub = 1 - smooth(0.85, 1.0, hub_d)
    hub_core = 1 - smooth(0.18, 0.30, hub_d)
    ring = hub * _line(hub_d * 6.0, 0.10)
    return dict(dist_id=dist_id, street=street, avenue=avenue, block_id=block_id, lot_id=lot_id,
                alley=alley, highway=highway, grain=grain, zone=zone, parks=parks, nightlife=nightlife,
                hub=hub, hub_core=hub_core, ring=ring)


def colorize_city(L, palette, park_share, salt):
    pal = np.array(palette, dtype=np.float32) / 255.0
    district = pal[(L["dist_id"] % len(pal)).astype(np.int64)]
    # Neighbouring districts differ only slightly; the large-scale zones carry the variation.
    district = district * 0.45 + pal.mean(axis=0) * 0.55
    zone = (0.80 + 0.40 * L["zone"])[..., None]
    roof = 0.80 + 0.36 * _u(L["lot_id"], salt)[..., None]
    col = district * roof * zone * (0.95 + 0.10 * L["grain"][..., None])

    glass = (_u(L["lot_id"], salt + 2) > 0.88)[..., None]
    col = np.where(glass, col * 0.5 + np.array([0.62, 0.74, 0.86], np.float32) * 0.55, col)

    pick = _u(L["block_id"], salt + 3)[..., None]
    # Parks gather into green belts instead of being sprinkled block by block.
    belt = (L["parks"] > 0.70 - park_share)[..., None]
    park = belt & (pick < 0.75)
    plaza = (pick < 0.012) & ~belt
    water = belt & (pick > 0.97)
    col = np.where(park, np.array([0.33, 0.52, 0.30], np.float32) * (0.85 + 0.3 * L["grain"][..., None]), col)
    col = np.where(plaza, np.array([0.90, 0.90, 0.88], np.float32), col)
    col = np.where(water, np.array([0.18, 0.36, 0.50], np.float32), col)

    alley = L["alley"][..., None]
    street = L["street"][..., None]
    avenue = L["avenue"][..., None]
    highway = L["highway"][..., None]
    # Streets are only a shade darker than the roofs; avenues and highways are pale concrete.
    col = col * (1 - 0.18 * alley)
    col = col * (1 - 0.30 * street)
    col = col * (1 - 0.8 * avenue) + np.array([0.62, 0.63, 0.65], np.float32) * 0.8 * avenue
    col = col * (1 - 0.85 * highway) + np.array([0.70, 0.71, 0.73], np.float32) * 0.85 * highway
    hub, core, ring = L["hub"][..., None], L["hub_core"][..., None], L["ring"][..., None]
    col = col * (1 - 0.45 * hub) + np.array([0.90, 0.91, 0.94], np.float32) * 0.45 * hub
    col = col * (1 - 0.6 * ring) + np.array([0.99, 0.99, 1.00], np.float32) * 0.6 * ring
    col = col * (1 - core) + np.array([0.97, 0.97, 0.99], np.float32) * core
    return col


def city_lights(L, warm, cool, cool_share, salt):
    """Night lights: avenues and highways blaze, streets glow, a scatter of lit buildings."""
    windows = (_u(L["lot_id"], salt) ** 3.0) * 0.45 * (1 - L["alley"]) * (1 - L["street"])
    lines = np.maximum.reduce([L["street"] * 0.20, L["avenue"] * 0.70, L["highway"] * 0.70])
    hubs_lit = L["ring"] * 0.9 + L["hub_core"] * 0.8
    # Downtowns blaze, industrial belts and old quarters stay dim; the change is gradual.
    glow = 0.25 + 1.1 * smooth(0.30, 0.75, L["nightlife"])
    parks_dark = 1 - 0.8 * smooth(0.62, 0.72, L["parks"])
    inten = np.clip((windows + lines) * glow * parks_dark + hubs_lit, 0, 1)
    inten = inten * (0.80 + 0.20 * _u(L["block_id"], salt + 1))
    # Warm sodium light and cool white light come in wide regions, blending at the edges.
    cool_mix = smooth(0.5 - 0.08, 0.5 + 0.08, L["zone"] + (cool_share - 0.5) * 0.4)[..., None]
    rgb = np.array(warm, np.float32) / 255 * (1 - cool_mix) + np.array(cool, np.float32) / 255 * cool_mix
    return rgb, inten


def terrain(p, seed):
    h = fbm(p, 1.6, 9, seed)
    land = smooth(0.445, 0.455, h)
    lat = np.abs(p[2])
    veg = fbm(p, 4.0, 7, seed + 5)
    green = np.array([0.24, 0.40, 0.20], np.float32)
    dry = np.array([0.55, 0.47, 0.32], np.float32)
    ground = green * (1 - veg[..., None]) + dry * veg[..., None]
    ocean_shallow = np.array([0.12, 0.33, 0.48], np.float32)
    ocean_deep = np.array([0.05, 0.14, 0.30], np.float32)
    depth = smooth(0.28, 0.43, h)[..., None]
    ocean = ocean_deep * (1 - depth) + ocean_shallow * depth
    col = ocean * (1 - land[..., None]) + ground * land[..., None]
    ice = smooth(0.86, 0.93, lat + 0.05 * veg)[..., None]
    col = col * (1 - ice) + np.array([0.90, 0.93, 0.96], np.float32) * ice
    return col, land, h


# ----------------------------------------------------------------------------- planets

ECUMENOPOLIS_PALETTE = [(196, 199, 205), (214, 210, 198), (180, 188, 200), (226, 222, 214),
                        (205, 198, 182), (190, 196, 192), (232, 233, 236), (178, 184, 196)]
ARCOLOGY_PALETTE = [(206, 200, 186), (190, 194, 200), (222, 216, 202), (180, 186, 192),
                    (214, 206, 190), (198, 206, 212)]


def make_ecumenopolis(p):
    L = city_layers(p, 404)
    col = colorize_city(L, ECUMENOPOLIS_PALETTE, 0.03, 900)
    lat = np.abs(p[2])
    # Polar caps are covered by climate domes rather than left as a pinched grid.
    dome = smooth(0.92, 0.96, lat)[..., None]
    col = col * (1 - dome) + np.array([0.80, 0.86, 0.92], np.float32) * (0.9 + 0.1 * L["grain"][..., None]) * dome
    rgb, inten = city_lights(L, (255, 196, 120), (190, 220, 255), 0.45, 500)
    inten = inten * (1 - smooth(0.92, 0.96, lat))
    return col, rgb, inten


def make_arcology(p):
    base, land, h = terrain(p, 77)
    L = city_layers(p, 707)
    sprawl = fbm(p, 2.4, 5, 99)
    # Cities cover most of the land and spill onto the coasts.
    urban = smooth(0.36, 0.44, sprawl + 0.20 * land) * smooth(0.38, 0.44, h)
    edge = urban[..., None]
    city = colorize_city(L, ARCOLOGY_PALETTE, 0.06, 901)
    col = base * (1 - edge) + city * edge
    # Suburbs: a faint street grid reaches a little past the city edge.
    suburb = smooth(0.26, 0.36, sprawl + 0.20 * land) * (1 - urban) * land
    col = col * (1 - 0.25 * (suburb * L["street"])[..., None])
    rgb, inten = city_lights(L, (255, 188, 108), (200, 225, 255), 0.25, 501)
    inten = inten * (urban + 0.35 * suburb * L["street"])
    return col, rgb, inten


def to_image(col):
    return Image.fromarray((np.clip(col, 0, 1) * 255).astype(np.uint8), "RGB")


def lights_image(rgb, inten):
    rgba = np.concatenate([np.clip(rgb, 0, 1), np.clip(inten, 0, 1)[..., None]], axis=-1)
    img = Image.fromarray((rgba * 255).astype(np.uint8), "RGBA")
    # A soft halo around the brightest lines reads as city glow from orbit.
    halo = img.filter(ImageFilter.GaussianBlur(1.6))
    return Image.alpha_composite(halo, img)


# ----------------------------------------------------------------------------- previews and icons

def render_sphere(surface, lights, size, light_dir=(-0.65, 0.45, 0.62), night_lights=True, rot=0.0):
    tex = np.asarray(surface.convert("RGB"), dtype=np.float32) / 255
    glow = np.asarray(lights.convert("RGBA"), dtype=np.float32) / 255
    th, tw = tex.shape[:2]
    gh, gw = glow.shape[:2]
    ys, xs = np.mgrid[0:size, 0:size].astype(np.float32)
    nx = (xs + 0.5) / size * 2 - 1
    ny = 1 - (ys + 0.5) / size * 2
    r2 = nx * nx + ny * ny
    inside = r2 <= 1
    nz = np.sqrt(np.clip(1 - r2, 0, 1))
    tilt = np.radians(-12)
    py = ny * np.cos(tilt) - nz * np.sin(tilt)
    pz = ny * np.sin(tilt) + nz * np.cos(tilt)
    lon = np.arctan2(nx, pz) + rot
    lat = np.arcsin(np.clip(py, -1, 1))
    u = ((lon / (2 * np.pi) + 0.5) % 1.0)
    v = 0.5 - lat / np.pi
    tx = np.clip((u * tw).astype(int), 0, tw - 1)
    ty = np.clip((v * th).astype(int), 0, th - 1)
    gx = np.clip((u * gw).astype(int), 0, gw - 1)
    gy = np.clip((v * gh).astype(int), 0, gh - 1)
    ld = np.array(light_dir, np.float32)
    ld /= np.linalg.norm(ld)
    lam = np.clip(nx * ld[0] + ny * ld[1] + nz * ld[2], 0, 1)
    shade = 0.06 + 0.94 * lam
    col = tex[ty, tx] * shade[..., None]
    if night_lights:
        g = glow[gy, gx]
        night = (1 - smooth(0.0, 0.35, lam))[..., None]
        col = col + g[..., :3] * g[..., 3:4] * night * 1.1
    rim = smooth(0.80, 1.0, np.sqrt(r2))[..., None]
    col = col * (1 - 0.35 * rim) + np.array([0.55, 0.70, 1.0], np.float32) * 0.35 * rim * lam[..., None]
    alpha = np.clip((1 - np.sqrt(r2)) * size / 1.5, 0, 1) * inside
    rgba = np.concatenate([np.clip(col, 0, 1), alpha[..., None]], axis=-1)
    return Image.fromarray((rgba * 255).astype(np.uint8), "RGBA")


def font(size):
    for name in ("bahnschrift.ttf", "arialbd.ttf", "segoeuib.ttf"):
        path = os.path.join(os.environ.get("WINDIR", "C:/Windows"), "Fonts", name)
        if os.path.isfile(path):
            f = ImageFont.truetype(path, size)
            if name == "bahnschrift.ttf":
                try:
                    f.set_variation_by_name("Bold")
                except Exception:
                    pass
            return f
    return ImageFont.load_default()


# Continues the base game's population icons (3 slate ... 10 orange-red) into violet and white.
POPULATION_COLORS = {11: ((214, 60, 110), (120, 20, 60)), 12: ((190, 60, 170), (100, 20, 95)),
                     13: ((150, 70, 210), (70, 25, 120)), 14: ((110, 110, 240), (40, 40, 130)),
                     15: ((235, 225, 255), (120, 100, 190))}


def population_icon(size_number):
    s = 40
    top, bottom = POPULATION_COLORS[size_number]
    img = Image.new("RGBA", (s, s))
    px = img.load()
    for y in range(s):
        t = y / (s - 1)
        c = tuple(int(top[i] * (1 - t) + bottom[i] * t) for i in range(3))
        for x in range(s):
            vig = 1 - 0.25 * (abs(x - s / 2) / (s / 2)) ** 2
            px[x, y] = tuple(int(ch * vig) for ch in c) + (255,)
    d = ImageDraw.Draw(img)
    big = font(22)
    text = str(size_number)
    left, top, right, bottom = d.textbbox((0, 0), text, font=big)
    x = (s - (right - left)) / 2 - left
    y = 3 - top
    d.text((x + 1, y + 1), text, font=big, fill=(20, 10, 30, 160))
    d.text((x, y), text, font=big, fill=(255, 255, 255, 255))
    d.text((2, s - 11), "10^", font=font(9), fill=(255, 255, 255, 230))
    d.rectangle([0, 0, s - 1, s - 1], outline=(0, 0, 0, 120))
    return img


def planet_icon(surface, lights, s):
    big = render_sphere(surface, lights, s * 4).resize((s, s), Image.LANCZOS)
    bg = Image.new("RGBA", (s, s), (12, 14, 24, 255))
    return Image.alpha_composite(bg, big)


def tint_icon(path, hue_rgb, strength=0.65):
    img = Image.open(path).convert("RGBA")
    a = np.asarray(img, dtype=np.float32) / 255
    lum = (0.3 * a[..., 0] + 0.59 * a[..., 1] + 0.11 * a[..., 2])[..., None]
    tint = np.array(hue_rgb, np.float32) / 255
    out = a[..., :3] * (1 - strength) + np.clip(lum * tint * 1.6, 0, 1) * strength
    return Image.fromarray((np.concatenate([out, a[..., 3:]], -1) * 255).astype(np.uint8), "RGBA")


# ----------------------------------------------------------------------------- main

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview", action="store_true")
    ap.add_argument("--core", default="D:/Games/StarSector/starsector-core")
    args = ap.parse_args()

    out_planets = os.path.join(ROOT, "graphics", "planets")
    out_markets = os.path.join(ROOT, "graphics", "icons", "markets")
    out_skills = os.path.join(ROOT, "graphics", "icons", "skills")
    for d in (out_planets, out_markets, out_skills):
        os.makedirs(d, exist_ok=True)

    p = sphere(W, H)
    made = {}
    for name, fn in (("aar_ecumenopolis", make_ecumenopolis), ("aar_arcology", make_arcology)):
        col, rgb, inten = fn(p)
        surface = to_image(col)
        lights = lights_image(rgb, inten)
        surface.save(os.path.join(out_planets, name + ".jpg"), quality=92)
        lights.save(os.path.join(out_planets, name + "_lights.png"), optimize=True)
        made[name] = (surface, lights)
        print("wrote", name)

    planet_icon(*made["aar_arcology"], 40).save(os.path.join(out_markets, "aar_arcology_world.png"))
    planet_icon(*made["aar_ecumenopolis"], 40).save(os.path.join(out_markets, "aar_ecumenopolis.png"))
    for n in range(11, 16):
        population_icon(n).save(os.path.join(out_markets, "aar_population%d.png" % n))

    skills = os.path.join(args.core, "graphics", "icons", "skills")
    tint_icon(os.path.join(skills, "industrial_planning.png"), (210, 150, 255)).save(
        os.path.join(out_skills, "aar_ethereal_enlightenment.png"))
    tint_icon(os.path.join(skills, "planetary_ops.png"), (255, 140, 210)).save(
        os.path.join(out_skills, "aar_acumenious_oracle.png"))
    tint_icon(os.path.join(skills, "hypercognition.png"), (160, 170, 255)).save(
        os.path.join(out_skills, "aar_delphic_optimiser.png"))

    planet_icon(*made["aar_ecumenopolis"], 128).save(
        os.path.join(ROOT, "graphics", "icons", "astral-ascension-renewed.png"))

    if args.preview:
        prev = os.path.join(ROOT, "build", "texture-preview")
        os.makedirs(prev, exist_ok=True)
        for name, (surface, lights) in made.items():
            sph = render_sphere(surface, lights, 640, rot=0.6)
            Image.alpha_composite(Image.new("RGBA", sph.size, (0, 0, 0, 255)), sph).save(
                os.path.join(prev, name + "_sphere.png"))
            lit = Image.new("RGB", lights.size)
            lit.paste(lights, mask=lights.split()[3])
            sheet = Image.new("RGB", (1024, 1024))
            sheet.paste(surface.resize((1024, 512)), (0, 0))
            sheet.paste(lit.resize((1024, 512)), (0, 512))
            sheet.save(os.path.join(prev, name + "_maps.jpg"), quality=88)
            surface.crop((800, 300, 1312, 556)).save(os.path.join(prev, name + "_detail.png"))
        strip = Image.new("RGBA", (5 * 48, 40), (30, 30, 36, 255))
        for i, n in enumerate(range(11, 16)):
            strip.paste(population_icon(n), (i * 48, 0))
        strip.save(os.path.join(prev, "population_icons.png"))
        print("previews in", prev)


if __name__ == "__main__":
    main()
