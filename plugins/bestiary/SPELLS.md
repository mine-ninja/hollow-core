# Task: High-Performance RPG Monster Ability System (Packet-Based)

### Objective
Develop a perfectly optimized Monster Ability System for a high-population RPG server. The system must handle custom mob skills without ticking unnecessary logic on the main thread and use **only packets** for all visual/audio effects.

### Core Requirements

1. **Ability Data Structure:**
   Each ability must be defined by the following attributes:
    * `displayName`: String (supports MiniMessage/Legacy colors).
    * `type`: Enum (PROJECTILE, AOE, TARGETED).
    * `damageRange`: Object/Record (min-max double values).
    * `cooldown`: Long (in milliseconds or ticks).

2. **Packet-Only Visuals & Optimization:**
    * **No Vanilla Entities for Effects:** Use `ClientboundLevelParticlesPacket` and `ClientboundLevelEventPacket` (or ProtocolLib equivalents) for skill animations.
    * **Culling Logic:** Implement a viewer check. Effects must ONLY be broadcast to players within a **128-block radius** of the monster to prevent network congestion.
    * **Asynchronous Calculations:** Damage calculations and targeting logic should be offloaded from the main tick where possible, using a thread-safe approach.

3. **Ability Behavior:**
    * **PROJECTILE:** A non-entity based projectile (simulated via particle packets in a line) that detects collision with player bounding boxes.
    * **AOE (Area of Effect):** Radial damage around the monster with a circular particle expanding effect.
    * **TARGETED:** An unavoidable skill that locks onto the nearest/highest-aggro player with a "tether" or "strike" effect.

4. **Technical Constraints:**
    * Use **Packets** for any "fake" entities or indicators (e.g., area markers).
    * Ensure the system is scalable (can handle 100+ monsters casting simultaneously).
    * Provide a clean API or Manager class to trigger these abilities via an external Mob AI controller.

### Expected Output
Provide a modular Java architecture (Classes, Enums, and a Manager) implementing this logic, focusing on NMS or ProtocolLib for the packet handling layer.