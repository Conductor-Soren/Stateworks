# Stateworks — Phase 1 Clean Source

This archive is based on the exact Stateworks project upload supplied for the Phase 1 cleanup.

## Cleanup
- Removed stale root-level Java source files left behind by the package refactor.
- Removed duplicate `src2` and `srcfull` source snapshots.
- Removed generated/build/editor artifacts from the distributable source archive.
- Preserved the canonical Phase 1 package structure under `src/main/java/com/stateworks/`.

## Compile fix
The package separation had a few imports/references that still pointed at the old root package:
- `Stateworks` is explicitly imported where client/network classes use it.
- `VisualStatePayload` remains in `com.stateworks.network`.
- `SignalInterpolator` is referenced from its new `com.stateworks.signal` package.

No runtime behavior was intentionally changed by this fix.
