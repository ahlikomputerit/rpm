# Forest Depths Asset Manifest

## Art direction

Semua aset menggunakan gaya **cinematic biophilic editorial**: hutan lembap, cahaya volumetrik pagi sampai malam, palet moss green, cedar brown, fog blue, amber firefly, dan heartwood black. Asset lingkungan utama harus konsisten dalam perspektif, grain, kelembapan, dan skala. Object transparan harus memiliki alpha bersih, subject lengkap, tanpa background, tanpa text, tanpa frame, dan tanpa bayangan berwarna yang tidak diminta.

## Scene plates

| ID | Asset | Format | Fungsi |
|---|---|---|---|
| plate-01 | Forest edge morning | JPG/WebP | Chapter 01 |
| plate-02 | Fern passage | JPG/WebP | Chapter 02 |
| plate-03 | Moss creek | JPG/WebP | Chapter 03 |
| plate-04 | Fog basin | JPG/WebP | Chapter 04 |
| plate-05 | Cathedral grove | JPG/WebP | Chapter 05 |
| plate-06 | Thorn hollow | JPG/WebP | Chapter 06 |
| plate-07 | Night marsh | JPG/WebP | Chapter 07 |
| plate-08 | Ancient boundary | JPG/WebP | Chapter 08 |
| plate-09 | Heartwood | JPG/WebP | Chapter 09 |

## Transparent environment objects

| ID | Asset | Layer | Format |
|---|---|---|---|
| obj-01 | Cedar trunk with exposed roots | Midground | PNG alpha |
| obj-02 | Twisted ancient tree trunk | Midground | PNG alpha |
| obj-03 | Fern cluster | Foreground | PNG alpha |
| obj-04 | Mossy branch canopy | Foreground | PNG alpha |
| obj-05 | Thorn branches | Foreground | PNG alpha |
| obj-06 | Wet stones and creek rocks | Near ground | PNG alpha |
| obj-07 | Moss-covered stone | Midground | PNG alpha |
| obj-08 | Fallen log with mushrooms | Midground | PNG alpha |
| obj-09 | Bioluminescent mushroom cluster | Accent | PNG alpha |
| obj-10 | Firefly cluster | Accent | PNG alpha |
| obj-11 | Hanging vines | Foreground | PNG alpha |
| obj-12 | Fog veil / low mist | Atmosphere | PNG alpha |
| obj-13 | Leaf and dust particle sheet | Atmosphere | PNG alpha |
| obj-14 | Ancient boundary stone | Landmark | PNG alpha |
| obj-15 | Root arch / heartwood opening | Landmark | PNG alpha |
| obj-16 | Owl silhouette | Hidden detail | PNG alpha |
| obj-17 | Deer silhouette | Hidden detail | PNG alpha |
| obj-18 | Fox silhouette | Hidden detail | PNG alpha |
| obj-19 | Moth silhouette | Hidden detail | PNG alpha |

## Branding

| ID | Asset | Format |
|---|---|---|
| brand-01 | Forest Depths symbol mark, no text | Transparent PNG |
| brand-02 | Favicon symbol | Transparent PNG |

## Generation batches

Batch A menghasilkan sembilan scene plates. Batch B menghasilkan object foreground dan midground. Batch C menghasilkan landmark dan heartwood assets. Batch D menghasilkan hidden wildlife silhouettes, atmosphere sheets, dan brand mark. Setelah generation selesai, setiap file dikompres, diberi nama stable, di-upload ke lifecycle-safe storage, dan dicatat pada manifest final dengan ukuran serta URL.

## Internet-sourced asset register — sourcing pass 2026-08-15

Sourcing pass ini menggunakan provider resmi dan memisahkan asset yang sudah siap dipakai dari kandidat scene plate yang masih membutuhkan verifikasi halaman foto individual. Kenney Nature Kit menyediakan 330 file dengan lisensi CC0 pada halaman resminya [1]. Poly Haven dipertahankan sebagai sumber utama untuk material dan environment realistis karena lisensinya CC0 [2]. Unsplash dapat digunakan untuk scene plate fotografis secara gratis untuk penggunaan komersial dan non-komersial, dengan atribusi opsional, tetapi setiap foto produksi tetap harus dicatat menggunakan URL halaman foto individual [3].

| Stable ID | Local optimized file | Role | Source | License | Status |
|---|---|---|---|---|---|
| kenney_tree_detailed | `forest-optimized/objects/tree_detailed.png` | Cedar/trunk midground fallback | [Kenney Nature Kit][1] | CC0 | Ready; alpha preserved; PNG optimized |
| kenney_tree_oak | `forest-optimized/objects/tree_oak.png` | Ancient boundary midground | [Kenney Nature Kit][1] | CC0 | Ready; alpha preserved; PNG optimized |
| kenney_tree_pineTallA_detailed | `forest-optimized/objects/tree_pineTallA_detailed.png` | Cathedral grove vertical layer | [Kenney Nature Kit][1] | CC0 | Ready; alpha preserved; PNG optimized |
| kenney_tree_tall | `forest-optimized/objects/tree_tall.png` | Deep forest silhouette | [Kenney Nature Kit][1] | CC0 | Ready; alpha preserved; PNG optimized |
| kenney_plant_bushDetailed | `forest-optimized/objects/plant_bushDetailed.png` | Fern/understory substitute | [Kenney Nature Kit][1] | CC0 | Ready; alpha preserved; PNG optimized |
| kenney_grass_leafsLarge | `forest-optimized/objects/grass_leafsLarge.png` | Foreground grass/fern texture | [Kenney Nature Kit][1] | CC0 | Ready; alpha preserved; PNG optimized |
| kenney_stone_tallA | `forest-optimized/objects/stone_tallA.png` | Boundary stone / wet rock | [Kenney Nature Kit][1] | CC0 | Ready; alpha preserved; PNG optimized |
| kenney_stone_largeA | `forest-optimized/objects/stone_largeA.png` | Creek and moss stone | [Kenney Nature Kit][1] | CC0 | Ready; alpha preserved; PNG optimized |
| kenney_log_large | `forest-optimized/objects/log_large.png` | Fallen log | [Kenney Nature Kit][1] | CC0 | Ready; alpha preserved; PNG optimized |
| kenney_mushroom_redGroup | `forest-optimized/objects/mushroom_redGroup.png` | Bioluminescent accent base | [Kenney Nature Kit][1] | CC0 | Ready; tint/glow added in renderer |
| unsplash_moss_creek_candidate | `forest-optimized/plates/tZRokU1Ujc6o.webp` | Moss creek plate candidate | [Unsplash search result][4] | Unsplash License | Optimized to WebP; individual page URL must be locked before production |
| unsplash_old_growth_candidate | `forest-optimized/plates/8PFWBcc6WCvj.webp` | Forest edge / ancient boundary candidate | [Unsplash forest search][5] | Unsplash License | Optimized to WebP; individual page URL must be locked before production |
| unsplash_fog_path_candidate | `forest-optimized/plates/GDwyrw78SVr6.webp` | Fog basin candidate | [Unsplash forest search][5] | Unsplash License | Optimized to WebP; individual page URL must be locked before production |
| unsplash_cedar_candidate | `forest-optimized/plates/48R5xmBdrhpZ.webp` | Cathedral grove candidate | [Cedar photo page][6] | Unsplash License | Optimized to WebP; page verified |
| unsplash_mossy_forest_candidate | `forest-optimized/plates/jIj7rtVw2ZY5.webp` | Fern passage candidate | [Mossy forest search][7] | Unsplash License | Optimized to WebP; individual page URL must be locked before production |
| unsplash_fog_path_person_candidate | `forest-optimized/plates/1aLT4ss2eJvp.webp` | Forest edge narrative candidate | [Unsplash forest search][5] | Unsplash License | Optimized to WebP; individual page URL must be locked before production |

### Optimization report

The first optimization pass produced **27 transparent PNG objects** and **6 WebP scene-plate candidates**. The optimized package is approximately **5.3 MB** before lifecycle-safe upload. Each file has dimensions, byte size, and SHA-256 checksum recorded in `forest-optimized/asset-register.json`. Source originals remain outside the project directory under `/home/ubuntu/webdev-static-assets/forest-sources/`.

### Remaining sourcing gaps

The current internet pass does not yet fully satisfy all 9 chapter-specific plates or all hidden wildlife/atmosphere assets. The next pass should source or create the missing fern-dominant plate, thorn hollow, night marsh, heartwood, fog veil, firefly cluster, hanging vines, root arch, and owl/deer/fox/moth silhouettes. These gaps are intentionally marked rather than filled with CSS silhouettes or unverified thumbnails.

## References

[1]: https://kenney.nl/assets/nature-kit "Kenney Nature Kit — CC0"
[2]: https://polyhaven.com/license "Poly Haven License — CC0"
[3]: https://unsplash.com/license "Unsplash License"
[4]: https://unsplash.com/photos/a-fast-flowing-river-winds-through-a-lush-mossy-forest-c9z764-wn3c "A fast-flowing river winds through a lush, mossy forest — Charlie Mitchell"
[5]: https://unsplash.com/s/photos/forest "Unsplash Forest Search"
[6]: https://unsplash.com/photos/massive-ancient-cedar-tree-in-a-lush-forest-MWQjFYyJTM4 "Massive ancient cedar tree in a lush forest"
[7]: https://unsplash.com/s/photos/mossy-forest "Unsplash Mossy Forest Search"
