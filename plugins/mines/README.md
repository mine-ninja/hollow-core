# Mines Plugin - Virtual Mine System

This module implements a virtual mine system where real world blocks are never changed.

## Core behavior

- One physical mine area is defined in config (`mines.<id>.mining-area`).
- Each host player owns one `MineInstance`.
- Invited members share the same virtual blocks and mined state.
- Non-members keep seeing normal world blocks.
- Real `BlockBreakEvent` is cancelled and replaced with virtual break logic.
- Reset clears only mined history and re-renders virtual blocks.

## Integration points

- Main service: `io.github.minehollow.mines.service.VirtualMineService`
- Reward hook event: `io.github.minehollow.mines.event.VirtualMineBlockBreakEvent`

Listen to `VirtualMineBlockBreakEvent` to grant money/xp based on `event.getBlockData()`.

## Notes

- Rendering uses `Player#sendBlockChange` chunk overlays.
- Block generation is deterministic by `(instance seed + reset epoch + block coordinates)`.
- Only mined positions are stored in memory.

