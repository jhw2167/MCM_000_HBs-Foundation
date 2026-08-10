# Vanilla API Patch Patterns: 1.21.1 → 26.1 (HBUtil batch)

Mechanical Mojang/registry API changes hit while compiling `HBUtil.java` against MC 26.1. These are reusable across all HB mods — apply the same find/replace patterns wherever they appear.

## Registry access

| 1.21.1 | 26.1 | Notes |
|---|---|---|
| `registryAccess().registryOrThrow(Registries.X)` | `registryAccess().lookupOrThrow(Registries.X)` | `registryOrThrow` removed from `RegistryAccess`/`RegistryAccess.Frozen`. |
| `registryAccess().registry(Registries.X).get()` | `registryAccess().lookupOrThrow(Registries.X)` | The `Optional`-returning `registry(...)` accessor is gone. |
| `registry.getHolder(key)` | `registry.get(key)` | Returns `Optional<Holder.Reference<T>>`; works for both `Identifier` and `ResourceKey` overloads. |
| `BuiltInRegistries.X.get(id)` → `T` | `BuiltInRegistries.X.getValue(id)` → `T` | `Registry.get(Identifier)` now returns `Optional<Reference<T>>`; `getValue(Identifier)` returns the raw `T` (nullable), matching old `get` behavior. Applied to `ITEM`, `ENTITY_TYPE`, `BIOME`. |

Example:
```java
// before
GeneralConfig.LOCAL_LEVEL.registryAccess()
    .registry(Registries.ENCHANTMENT).get().getHolder(key).orElse(null);
// after
GeneralConfig.LOCAL_LEVEL.registryAccess()
    .lookupOrThrow(Registries.ENCHANTMENT).get(key).orElse(null);
```

## ResourceLocation → Identifier accessor rename on keys

`ResourceKey.location()` was renamed to `ResourceKey.identifier()` (part of the `ResourceLocation`→`Identifier` rename in the Balm Mojmap set).

```java
level.dimension().location()      →  level.dimension().identifier()
key.location()                    →  key.identifier()
holder.unwrapKey().get().location() → holder.unwrapKey().get().identifier()
```

## ChunkPos is now a record

`ChunkPos` became a `record ChunkPos(int x, int z)` — the public fields `x`/`z` are private components now, accessed via methods `x()` / `z()`. The `ChunkPos(BlockPos)` convenience constructor and static `ChunkPos.asLong(int,int)` were removed.

```java
chunkPos.x            →  chunkPos.x()
chunkPos.z            →  chunkPos.z()
new ChunkPos(blockPos)→  new ChunkPos(blockPos.getX() >> 4, blockPos.getZ() >> 4)
ChunkPos.asLong(x, z) →  ((long) z << 32) | (x & 0xFFFFFFFFL)   // vanilla packing, kept identical
```
> Assumption flagged: the inlined `asLong` packing reproduces vanilla's exact `((long)z<<32)|(x&0xFFFFFFFFL)` encoding. Foundation only compares these keys against its own maps (cleared each server start), so any bijective encoding is safe, but this keeps the numeric values identical to pre-26.1.

## GameProfile

authlib `GameProfile` is now a record: `getName()` → `name()` (and `getId()` → `id()`).

## Entity spawn reason

`net.minecraft.world.entity.MobSpawnType` → `net.minecraft.world.entity.EntitySpawnReason`. Enum constants keep their names (`COMMAND`, `NATURAL`, `SPAWNER`, …). The 6-arg `EntityType.create(ServerLevel, Consumer<T>, BlockPos, EntitySpawnReason, boolean, boolean)` overload is unchanged apart from the type rename.

## ServerLevel forced-chunks (debug only)

`ServerLevel.getForcedChunks()` was removed. It was only used here for a debug log count, replaced with the mod's own `forceLoadedChunkTicketIds.size()`.

---

## Chunk ticket system (`HBUtil.ChunkUtil`) — RESOLVED via /forceload (author decision)

`TicketType` was overhauled in 1.21.5+ (no longer generic; `TicketType.create(String,Comparator)` removed; `addRegionTicket`/`removeRegionTicket` dropped the trailing `String` value arg; `ServerLevel.getForcedChunks()` removed). Neither Balm nor Waystones force-load chunks, so there was no in-repo reference.

Per author decision, region tickets were replaced with vanilla **persistent forceload**:
- `MOD_TICKET` / `TicketType.create(...)` field deleted.
- `addRegionTicket(MOD_TICKET, pos, radius, ticketId)` → `level.setChunkForced(pos.x(), pos.z(), true)`.
- `removeRegionTicket(...)` → `level.setChunkForced(pos.x(), pos.z(), false)`, moved **after** the ownership check so a non-owner call stays a no-op (reproducing the old behavior where a mismatched ticketId removal did nothing).
- Debug-only `level.getForcedChunks().size()` → `forceLoadedChunkTicketIds.size()` (the mod's own tracking map).

> **BEHAVIOR CHANGE (verify in-game):** `setChunkForced` only loads the single target chunk at FULL status. The previous `chunkMinStatus`/`TICKET_RADIUS_FEATURES` (2-chunk radius for feature generation) is no longer applied — the `chunkMinStatus` params are now ignored. `setChunkForced` is also persistent (survives save/load) whereas region tickets were transient; the mod clears its tracking maps on server start, so re-forcing on load is driven by the mod as before, but any chunk left forced at shutdown will persist in the save. If either difference matters for a consumer (Ore Cluster regen, Satellites), revisit with a custom `TicketType` once its 26.1 constructor is confirmed.

## Clientside + world-data batch (2nd compile pass)

MC 1.21.6+ "de-getter"/reorg wave. **Note:** `964_waystones` is built against `26.1-snapshot-6`, which predates our `26.1.2` target, so several of its APIs had already drifted — items below marked *(assumption)* are best-effort and should be confirmed on the next build.

High-confidence:
| 1.21.1 | 26.1 |
|---|---|
| `Level.isClientSide` (field) | `level.isClientSide()` (field is now private) |
| `Level.random` (field) | `level.getRandom()` (protected field) |
| `Camera.getPosition()` | `camera.position()` |
| `ServerPlayer.serverLevel()` | `(ServerLevel) player.level()` |
| `ChunkAccess.setUnsaved(true)` | `chunk.markUnsaved()` |
| `new DustParticleOptions(Vector3f, scale)` | `new DustParticleOptions(int packedRGB, scale)` (also removes `Vec3.fromRGB24(...)`) |
| `ResourceKey.location()` (in lambdas) | `key.identifier()` |

Assumptions (verify on next build):
- `Level.getDayTime()` → `level.dayTime()`. *(assumption)*
- `DimensionType.fixedTime()` removed → replaced `level.dimensionType().fixedTime().orElse(TICKS_PER_DAY)` with the constant `TICKS_PER_DAY` (correct for the overworld; **loses per-dimension fixed-time day length** for End/custom fixed-time dims). *(assumption + behavior change)*
- `WorldData.worldGenOptions().seed()` → `server.overworld().getSeed()`. *(assumption)*
- `LevelData.getSpawnPos()` removed → `(level instanceof ServerLevel sl) ? sl.getSharedSpawnPos() : BlockPos.ZERO`. *(assumption; client levels now yield ZERO)*
- `LocalPlayer.input.up/down/left/right` → `player.input.keyPresses.forward()/backward()/left()/right()`. *(assumption — the `Input` record accessor names)*
- `Player.displayClientMessage(Component, boolean)` removed → `player.sendSystemMessage(Component)`. *(assumption; drops the actionbar boolean)*
- `GuiGraphicsExtractor.drawString(Font, String, int, int, int, boolean)` → dropped the trailing shadow `boolean` (5-arg form). *(assumption — GuiGraphicsExtractor may not expose `drawString` at all)*
- `CauldronInteraction.InteractionMap` no longer constructable → `CauldronInteraction.newInteractionMap("")` factory (EssenceCauldronBlock). *(assumption)*

Removed / stubbed (compile-clean, feature flagged for rework):
- **`LightTexture`** removed from `net.minecraft.client.renderer` → dropped from `RenderLevelEvent` (field/ctor/getter), `ClientEventRegistrar.onRenderLevel`, and `LevelRendererMixin` (`gameRenderer.lightTexture()` call removed).
- **`BlockEntityRenderer` no-op** (`SimpleBlockEntityRenderer`) → the submit/render-state contract changed (`CameraRenderState` in `net.minecraft.client.renderer.state` not present on 26.1.2); since it drew nothing, it's no longer registered (`ModRenderers` is now empty) and the class is a placeholder.
- **`BeaconRenderer.renderBeaconBeam(...)`** signature gone (beam rendering moved to the submit pipeline) → the waypoint beam draw is commented out in client `MovingWaypoint`; **the waypoint beam will not render until re-implemented.**
- **`ItemStack.getEntityRepresentation()`** removed → the extra discard in `AnvilMenuMixin.onOnTake` dropped (the dropped item is already spawned via `addFreshEntity`).

Access transformer (NeoForge only): `CreativeModeTab.Output` is **protected under NeoForge's patches** (it stays public in vanilla/Fabric mojmap), which broke `ModItems`' `displayItems` lambda when `:common` compiles under NeoForge moddev. Fix: `common/src/main/resources/META-INF/accesstransformer.cfg` with `public net.minecraft.world.item.CreativeModeTab$Output` (mirrors Balm's AT; applied by both the `:common` and `:neoforge` moddev compiles).

> Do **not** add a Fabric access widener for this. `:fabric` recompiles the common sources under Loom/mojmap where `Output` is already public, so no widening is needed — and Loom rejects a hand-written `named`-namespace widener here with `"Expected official namespace for access widener entry, found: named"`. The Fabric build's `loom { accessWidenerPath ... }` wiring was removed and the `"accessWidener"` field was dropped from `fabric.mod.json`. (No sibling HB project uses an access widener.)

## NeoForge chunk attachment → Balm data attachment API

**Symptom:** runtime crash `LevelCallback.Chunk.LOAD unbound`. **Cause:** the NeoForge `FoundationAttachments.init()` (which called `LevelCallback.Chunk.LOAD.register(...)`) ran in the `@Mod` constructor, *before* `Balm.initializeMod(...)` binds Balm's event mappings. (Fabric was unaffected — it calls `FoundationAttachments.init()` *after* `Balm.initializeMod`.)

**Fix (NeoForge only, per author decision):** `ManagedChunkAttachment` now uses Balm's [data attachment API](https://balm.twelveiterations.com/advanced/data-attachments) instead of a NeoForge `DeferredRegister<AttachmentType>` + manual chunk-load listener:
- `register(BalmRegistrars)` → `registrars.dataAttachmentTypes(r -> LOOKUP = r.register("managed_chunk", CODEC).asLookup())` (persistent codec, no initializer needed since we never call `getOrCreate`).
- Persistence model kept minimal: the **common** `ManagedChunkEvents.onChunkLoad` still creates ManagedChunks into the `LOADED_CHUNKS` registry (unchanged). A bound `LevelCallback.Chunk.LOAD` hook attaches that instance via `LOOKUP.update(chunk, mc)` so Balm saves it; on load Balm auto-decodes it via `CODEC` (the `ManagedChunk(CompoundTag)` ctor re-registers it). `LOOKUP.has(chunk)` short-circuits already-loaded chunks; a get-or-create dedupes against `LOADED_CHUNKS` regardless of listener order.
- The `CODEC.decode` was made ops-agnostic (`ops.convertTo(NbtOps.INSTANCE, input)`) rather than the previous fragile `input instanceof CompoundTag`.
- Wiring moved into the bound init: `FoundationMainForge` now calls `Balm.initializeMod(id, ctx, registrars -> { CommonClass.init(registrars); FoundationAttachments.registerBalmAndEvents(registrars); })`. `FoundationAttachments.init()` (in the `@Mod` ctor) now only static-inits the still-NeoForge-native **player** attachment type; the player-join trigger also moved into `registerBalmAndEvents` (it had the same latent unbound-timing issue).

## Fabric attachment restructure (parity with NeoForge)

Once the NeoForge structure was confirmed working, the **Fabric** side was restructured to match it 1:1:
- `fabric/.../ManagedChunkAttachment` was rewritten to use Balm's data-attachment API — it is now **byte-identical to the NeoForge version** (Balm's API is cross-loader), replacing the old Fabric `AttachmentRegistry.createPersistent` + external `LevelCallback.Chunk.LOAD` listener.
- `fabric/.../FoundationAttachments` now exposes the same `init()` (static-inits only the player attachment) + `registerBalmAndEvents(BalmRegistrars)` (registers the Balm chunk attachment and the `ServerPlayerCallback.Join` player trigger) split as NeoForge.
- `FoundationMainFabric` now calls `FoundationAttachments.init()` and then wraps the initializer: `Balm.initializeMod(id, ctx, registrars -> { CommonClass.init(registrars); FoundationAttachments.registerBalmAndEvents(registrars); })` (previously it called `FoundationAttachments.init()` *after* `Balm.initializeMod`).
- `fabric/.../ManagedPlayerAttachment` stays Fabric-native (Fabric's `AttachmentRegistry` + `Codec`; there is no Fabric equivalent of NeoForge's `IAttachmentSerializer`). Only its trigger moved into `registerBalmAndEvents`.

So the chunk attachment is now a single shared design across both loaders; only the player attachment differs by loader (NeoForge `IAttachmentSerializer` vs Fabric `AttachmentRegistry`), which is unavoidable.

## In-world render stages: LevelRenderer mixin → loader render events

**Symptom:** `registerOnRenderLevel(AFTER_PARTICLES, ...)` never fired (waypoint flare invisible). **Cause:** the common `LevelRendererMixin` injected into `LevelRenderer#renderLevel` at string-constant markers (`sky`/`entities`/`translucent`/`particles`/`weather`); 26.1.2 reworked that pipeline and those inject points no longer exist, so with `require = 0` the injections silently missed.

**Fix:** dropped the fragile mixin, drove `RenderLevelEvent` from the **loader-native render events** instead:
- Removed `LevelRendererMixin` from `hbs_foundation.mixins.json` and emptied the class (couldn't delete).
- **NeoForge (1.21.10):** `neoforge/.../client/FoundationRenderEvents` subscribes on `NeoForge.EVENT_BUS`. **`RenderLevelStageEvent` was rebuilt for the frame-graph pipeline** — it is now abstract with one concrete sub-event per stage (`AfterSky`, `AfterOpaqueBlocks`, `AfterEntities`, `AfterTranslucentBlocks`, `AfterTripwireBlocks`, `AfterParticles`, `AfterWeather`, `AfterLevel`), and the old `Stage` enum + `getStage()`/`getCamera()`/`getProjectionMatrix()` were **removed**. Register a listener per sub-event class (`addListener((RenderLevelStageEvent.AfterParticles e) -> …)`); the stage is implied by which sub-event fires. Only `getPoseStack()` (`@Nullable`) and `getModelViewMatrix()` remain, so the **camera** is pulled from `Minecraft.getInstance().gameRenderer.getMainCamera()` and the **projection matrix** is passed as `null` (unused by our consumers). Wired from `FoundationsMainForgeClient`.
- **Fabric:** *(no working hook yet)* Fabric API **removed** its in-world render event suite (`WorldRenderEvents` / `WorldRenderContext`) in the **1.21.9 port** because of the render-pipeline rework, with no replacement API yet ([fabric-api#4902](https://github.com/FabricMC/fabric-api/issues/4902); official guidance: "use mixins"). `fabric/.../client/FoundationRenderEvents` is therefore a **no-op stub** so the module compiles/runs — in-world render-stage consumers (the waypoint beam) do **not** fire on Fabric. To restore it, add a `LevelRenderer`/`GameRenderer#renderLevel` mixin that calls `ClientEventRegistrar.onRenderLevel(...)`. (NeoForge is fully functional via `RenderLevelStageEvent`.)

### Other 26.1.2 client renames hit on the Fabric build
| 1.21.1 | 26.1.2 |
|---|---|
| `Minecraft.setLevel(ClientLevel, ReceivingLevelScreen.Reason)` | `Minecraft.setLevel(ClientLevel)` — the `ReceivingLevelScreen.Reason` param was dropped; the Fabric `MixinMinecraft` inject target and method signature were updated to match (confirmed against Balm's `MinecraftMixin`). |

Also: `EventRegistrar.onLevelLoad(...)` / `onLevelUnload(...)` were made **`public`** (they were package-private) so the Fabric `MixinMinecraft` — which lives in a different package — can call them directly (client level load/unload is driven by that mixin, not a Balm callback).
- `RenderLevelEvent` / `ClientEventRegistrar.onRenderLevel` reshaped to carry the live **`PoseStack`** + a `float partialTick` (what the loader events supply), dropping the removed `LightTexture` / `GameRenderer` / `DeltaTracker`. Partial tick is read from `Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)`.

### Render API renames used here (26.1.2)
| 1.21.1 | 26.1.2 |
|---|---|
| `net.minecraft.client.renderer.RenderType` | `net.minecraft.client.renderer.rendertype.RenderTypes` (e.g. `RenderTypes.lines()`) |
| `net.minecraft.client.renderer.LightTexture` | *removed* (dropped from the render event) |
| `BeaconRenderer.renderBeaconBeam(...)` | *removed* — beacon rendering moved to the submit pipeline (`BeaconRenderer.submitBeaconBeam(...)` now takes a `SubmitNodeCollector`, which is **not** available inside a `RenderLevelStageEvent`). The waypoint beam copies vanilla `BeaconRenderer`'s quad geometry (`renderPart`/`renderQuad`/`addVertex`) but draws it **immediate-mode** to `RenderTypes.beaconBeam(loc, false/true)` buffers (solid + glow layers) via `bufferSource.getBuffer(...)`, flushed with `bufferSource.endBatch()`. Vertex build uses `addVertex(pose, x,y,z).setColor(argb).setUv(u,v).setOverlay(NO_OVERLAY).setLight(0xF000F0).setNormal(pose,0,1,0)`; the beam animates on `Math.floorMod(gameTime,40)+partialTick` and rotates via `Axis.YP.rotationDegrees(...)`. |
| `Camera.getPosition()` | `Camera.position()` |
| `VertexConsumer.vertex()/color()/normal()` | `addVertex()/setColor()/setNormal()` |

## Files touched in this batch
`HBUtil` (registry/identifier/ChunkPos/GameProfile/EntitySpawnReason/forceload), `core/EssenceType` (lookupOrThrow + `getValue`), `core/MovingWaypoint`, `core/ChunkExplorerManager` (ChunkPos accessors + `new ChunkPos(BlockPos)`), `model/ManagedChunk`, `model/ManagedChunkUtility`, `model/VanillaEntityLike`, `biome/BiomeManager`, `structure/StructureManager` (`getHolder`→`get`, `new ChunkPos(BlockPos)`), `enchantment/EssenceEnchantment` (`getHolder`→`get`), `event/EventRegistrar` (`identifier()`), `CommonClassDebug` (`identifier()` + `GameProfile.name()`).
