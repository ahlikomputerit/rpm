# Forest Depths Asset Optimization Report

The first sourced-asset package contains 27 optimized transparent PNG objects and 6 optimized WebP plate candidates.

| Group | Source size | Optimized size | Result |
|---|---:|---:|---|
| Unsplash plate candidates | 12.79 MB | 5.08 MB | WebP quality 82, max edge 2400 px |
| Kenney Nature Kit source bundle | 10.05 MB | 0.07 MB | 27 selected alpha PNG objects, optimized in place |

The optimized package remains outside the web project directory at `/home/ubuntu/webdev-static-assets/forest-optimized`. Every optimized file is registered in `asset-register.json` with dimensions, byte size, license, source, and SHA-256 checksum. The production upload step is intentionally deferred until the Forest Depths managed web project is initialized, so uploaded URLs are tied to the correct project lifecycle.
