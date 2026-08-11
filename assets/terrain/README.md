# Terrain cache

`ensure-site!` loads `site-terrain.glb` from this directory when present (fast path).
Use `yardcraft.site/regenerate-terrain-cache!` to rebuild the cache after terrain
feature changes. The `.glb` is gitignored — do not commit site-specific terrain meshes.
