# HBs_Foundation Update Guide: 1.21.1 → 26.1 (Fabric + NeoForge, Balm-based)

This document records the changes made to migrate `MCM_000_HBs-Foundation` from Minecraft 1.21.1 to the Minecraft 26.1 snapshot line, targeting Fabric and NeoForge on top of **Balm 26.1** (`net.blay09.mods:balm-* : 26.1.2.5`). The reference implementation for API patterns was `964_waystones`, already updated to 26.1, and the Balm source in the sibling `Balm/` directory.

Unlike the 1.21.1 pack, the biggest change here is **not** the build system (the `buildSrc` convention-plugin layout was retained per the "minimal changes" decision) but the **Balm API surface**, which was reorganized wholesale, plus a set of vanilla Mojmap renames.

---

## 1. Versions (`gradle.properties`)

| Property | 1.21.1 | 26.1 |
|---|---|---|
| `minecraft_version` | `1.21.1` | `26.1.2` |
| `java_version` | `21` | `25` |
| `neoforge_version` | `21.1.8` | `26.1.2.21-beta` |
| `neoforge_version_range` | `[21-beta,)` | `[26.1,)` |
| `neo_form_version` | `1.21.1-...` | `26.1.2-1` |
| `parchment_minecraft` / `parchment_version` | `1.21` / `2024.06.23` | `1.21.10` / `2025.10.12` |
| `fabric_version` | `0.116.11+1.21.1` | `0.146.1+26.1.2` |
| `fabric_loader_version` | `0.17.3` | `0.19.2` |
| `loom_version` | `1.8-SNAPSHOT` | `1.14-SNAPSHOT` |
| `forge_version` | `47.3.0` | `64.0.4` (unused; Forge not built) |
| `balm_version` | `21.0.62+1.21.1` | `26.1.2.5` |
| `balm_version_range` | `[21.0.58,)` | `[26.1.2,)` |

New property added: `fabric_minecraft_version_range = ~26.1-` (Fabric now wants a version *predicate* range rather than an exact MC id — see §5).

Other build changes:
- `build.gradle`: `net.neoforged.moddev` plugin `2.0.49-beta` → `2.0.141`.
- `gradle/wrapper/gradle-wrapper.properties`: Gradle `8.10` → `9.3.0`.
- `common/build.gradle`: mixin `0.8.5` → `0.8.7`.
- `buildSrc/.../multiloader-common.gradle`: added `fabric_minecraft_version_range` to the `expandProps` map so the new `fabric.mod.json` placeholder resolves.

Loaders built: **Fabric + NeoForge** (unchanged `settings.gradle`). Forge remains excluded.

---

## 2. Resource templates

- `common/.../pack.mcmeta`: `pack_format` `8` → `64`.
- `fabric/.../fabric.mod.json` `depends` block rewritten to the Waystones 26.1 shape:
  - `minecraft` now uses `${fabric_minecraft_version_range}` (e.g. `~26.1-`) instead of the raw `${minecraft_version}`.
  - `fabric` → `fabric-api`, `balm-fabric` → `balm`, and `java`/`fabricloader` now bound to `${java_version}` / `${fabric_loader_version}`.
- `neoforge.mods.toml` is unchanged in shape (still valid in 26.1).

---

## 3. Balm API reorganization (the core of this update)

Balm 26.1 moved essentially every public class out of the old `net.blay09.mods.balm.api.*` tree and replaced the reflective **event bus** (`Balm.getEvents().onEvent(SomeEvent.class, …)`) with typed **callback interfaces** (`SomeCallback.EVENT.register(…)`). Key package moves used here:

| 1.21.1 (Balm 21) | 26.1 (Balm 26) |
|---|---|
| `net.blay09.mods.balm.api.Balm` | `net.blay09.mods.balm.Balm` |
| `net.blay09.mods.balm.api.client.BalmClient` | `net.blay09.mods.balm.client.BalmClient` |
| `net.blay09.mods.balm.api.network.BalmNetworking` | `net.blay09.mods.balm.network.BalmNetworking` |
| `net.blay09.mods.balm.api.command.BalmCommands` | `net.blay09.mods.balm.commands.BalmCommands` |
| `net.blay09.mods.balm.api.config.Config` / `Comment` | `net.blay09.mods.balm.platform.config.reflection.Config` / `Comment` |
| `net.blay09.mods.balm.neoforge.NeoForgeLoadContext` | `net.blay09.mods.balm.neoforge.platform.runtime.NeoForgeLoadContext` |
| `net.blay09.mods.balm.api.DeferredObject` | *(removed — see §3.3)* |
| `net.blay09.mods.balm.api.BalmRegistries` | *(removed — use vanilla `BuiltInRegistries`)* |
| `net.blay09.mods.balm.api.config.BalmConfigData` | *(removed — config classes are plain `@Config` POJOs)* |
| `Balm.getEvents().onEvent(...)` | typed callbacks under `net.blay09.mods.balm.platform.event.callback.*` |

### 3.1 Mod entrypoints & initialization

`Balm.initialize(id, EmptyLoadContext.INSTANCE, …)` → `Balm.initializeMod(id, <LoadContext>, Consumer<BalmRegistrars>)`.

- **Fabric**: `FabricLoadContext.INSTANCE` (`net.blay09.mods.balm.fabric.platform.runtime.FabricLoadContext`).
- **NeoForge**: `new NeoForgeLoadContext(modContainer, modEventBus)` — the constructor now takes **both** the `ModContainer` and the mod event bus, so the `@Mod` constructor signature became `FoundationMainForge(ModContainer, IEventBus)`.
- The initializer now receives a `BalmRegistrars` and all registration (blocks, block entities, items, creative tabs, data components) happens **inside** it. `CommonClass.init` and `CommonClassClient.initClient` were re-typed to accept `BalmRegistrars` / `BalmClientRegistrars`.

### 3.2 Registration model

The old "get the registrar off `Balm`" style (`Balm.getBlocks()`, `Balm.getItems()`, `Balm.getBlockEntities()`, `Balm.getRuntime().registrar()`) is gone. Registration is now scoped through the `BalmRegistrars` passed to the initializer:

```java
static void initRegistries(BalmRegistrars registrars) {
    registrars.blocks(ModBlocks::initialize);            // BalmBlockRegistrar
    registrars.blockEntityTypes(ModBlockEntities::initialize);
    registrars.items(ModItems::initialize);              // BalmItemRegistrar
    registrars.creativeModeTabs(ModItems::creativeTab);
    ModDataComponents.register(registrars.registrar());  // BalmRegistrar
}
```

Registrar method shapes also changed:
- `BalmBlockRegistrar.register(name, Function<Properties,Block> ctor, Supplier<Properties>)` returns a `BalmBlockRegistration`; call `.withDefaultItem().asDeferredBlock()` to also get the `BlockItem` and a `DeferredBlock` handle.
- `BalmItemRegistrar.register(name, Function<Item.Properties,Item> ctor).asDeferredItem()`.
- `BalmBlockEntityTypeRegistrar.register(name, BE::new, DeferredBlock...).asHolder()`.
- Item/Block constructors now receive their `Properties` as a **constructor argument** (Mojang 1.21.2+ change). All Foundation `Item`/`Block` subclasses (`WaypointStick`, `SimpleRewardItem`, `EnchantedEssence`, `EmptyBlock`, `SimpleBlockEntityBlock`, `EssenceCauldronBlock`) had their constructors changed to take `Item.Properties` / `BlockBehaviour.Properties`.

### 3.3 `DeferredObject` shim

Balm's `net.blay09.mods.balm.api.DeferredObject<T>` was removed. To avoid touching every downstream consumer that reads `ModBlocks.stoneBrickBlockEntity.get()` etc., a lightweight replacement was added:

`com.holybuckets.foundation.util.DeferredObject<T>` — a `Supplier<T>` wrapper with `DeferredObject.of(Holder<T>)`. `ModBlocks`/`ModItems`/`ModBlockEntities` keep their old `DeferredObject<Block/Item/…>` public fields (now backed by the Balm `DeferredBlock`/`DeferredItem`/`Holder`).

### 3.4 Config

`BalmConfigData` interface removed — a config class is now just a POJO annotated `@Config(modId)` with `@Comment`/`@Range` fields. `PerformanceImpactConfigData` dropped `implements BalmConfigData`. Access changed:
`Balm.getConfig().getActive(X.class)` → `Balm.config().getActiveConfig(X.class)`, and `registerConfig(X.class, null)` → `registerConfig(X.class)`.

### 3.5 The event system — Foundation compat layer

This is the largest change. Foundation's `EventRegistrar` exposes a stable, priority-aware event API to the downstream mods (001–010) built on Balm's old event classes (`ChunkLoadingEvent`, `PlayerLoginEvent`, `ServerStartingEvent`, `EventPriority`, …). Balm 26.1 deleted all of those.

Per the design decision, those event *shapes* were **re-homed inside Foundation** rather than rewriting every downstream call site:

- New package `com.holybuckets.foundation.event.balm.*` (+ `.server`, `.client`, `.client.screen`) containing Foundation-owned copies of the events Foundation actually fires: `BalmEvent`, `EventPriority`, `ChunkLoadingEvent`, `LevelLoadingEvent`, `PlayerLoginEvent`, `PlayerLogoutEvent`, `PlayerAttackEvent`, `BreakBlockEvent`, `PlayerChangedDimensionEvent`, `PlayerRespawnEvent`, `LivingDeathEvent`, `LivingDamageEvent`, `LivingFallEvent`, `LivingHealEvent`, `UseBlockEvent`, `DigSpeedEvent`, `TossItemEvent`, `server/ServerStarting|Started|StoppedEvent`, `client/ClientStarted|ConnectedToServer|DisconnectedFromServer|BlockHighlightDraw|GuiDrawEvent`, `client/screen/Screen|ContainerScreenDrawEvent`.
- `EventPriority.toPhase()` maps the enum onto Balm's `EventPhases` identifiers (`LOWEST…HIGHEST`), so priority survives the move to Fabric-style phased callbacks.
- `BalmEventRegister` was rewritten from a bank of `registry.onEvent(SomeEvent.class, c, priority)` calls into **adapters** that register Foundation consumers against the new Balm callbacks and wrap the callback arguments into the Foundation event objects. Mapping used:

| Foundation event | Balm 26.1 callback |
|---|---|
| `ServerStarting/Started/Stopped` | `ServerLifecycleCallback.Starting/Started/Stopped.EVENT` |
| `LevelLoadingEvent.Load/Unload` | `LevelCallback.LOAD/UNLOAD` |
| `ChunkLoadingEvent.Load/Unload` | `LevelCallback.Chunk.LOAD/UNLOAD` |
| `PlayerLoginEvent` / `PlayerLogoutEvent` | `ServerPlayerCallback.Join/Leave.EVENT` |
| `PlayerChangedDimensionEvent` | `ServerPlayerCallback.DimensionChange.EVENT` |
| `PlayerRespawnEvent` | `ServerPlayerCallback.Respawn.EVENT` |
| `PlayerAttackEvent` | `PlayerCallback.Attack.Before.EVENT` |
| `BreakBlockEvent` | `BlockCallback.Break.Before.EVENT` |
| `UseBlockEvent` | `BlockCallback.Use.EVENT` (returns `InteractionEventResult`) |
| `DigSpeedEvent` | `BlockCallback.DigSpeed.EVENT` |
| `LivingDeath/Damage/Fall/Heal` | `LivingEntityCallback.Death/Damage/Fall/Heal.Before.EVENT` |
| `TossItemEvent` | `ItemCallback.Toss.Before.EVENT` |
| server tick / server-level tick | `ServerTickCallback.BEFORE` / `ServerTickCallback.ServerLevelTick.BEFORE` |

- **Client** side: `ClientBalmEventRegister` was likewise rewritten onto `ClientLifecycleCallback.*`, `ClientTickCallback.*`, `RenderCallback.*`, and `ScreenCallback.Render.*`. Note two lossy mappings flagged for verification:
  - `GuiDrawEvent` element-specific hooks map onto `RenderCallback.Gui.{Health,Chat,Debug,BossInfo,PlayerList}.AFTER`.
  - There is no dedicated container-screen *foreground* (renderLabels) hook in Balm 26.1; both background and foreground consumers are driven from `ScreenCallback.Render` variants.
- The Fabric/NeoForge `FoundationAttachments` classes previously used `Balm.getEvents().onEvent(...)` to register chunk/player attachment hooks; these now use `LevelCallback.Chunk.LOAD` and `ServerPlayerCallback.Join.EVENT` directly.
- The client `LevelLoadingEvent` fired from the Fabric `MixinMinecraft` now calls `EventRegistrar.getInstance().onLevelLoad/Unload(...)` directly (client levels aren't covered by Balm's server `LevelCallback`). The NeoForge `MinecraftMixin` calls `ClientEventRegistrar.onClientStarted(...)`.

### 3.6 `GuiGraphics` → `GuiGraphicsExtractor`

The Balm Mojmap set renames `GuiGraphics` to `GuiGraphicsExtractor` in the client GUI/screen callbacks. Foundation's client draw events (`GuiDrawEvent`, `ScreenDrawEvent`, `ContainerScreenDrawEvent`) and `MessagerClient`'s text-outline helper were re-typed to `net.minecraft.client.gui.GuiGraphicsExtractor` (same method surface: `drawString`, `blit`, …).

### 3.7 Renderers

`Balm.getRenderers()` / `renderers.registerBlockEntityRenderer(...)` → `BalmClientRegistrars.blockEntityRenderers(BalmBlockEntityRendererRegistrar r)` with `r.register(Holder<BlockEntityType>, provider)`. `SimpleBlockEntityRenderer` was ported to the new **render-state pipeline**: it now implements `BlockEntityRenderer<BE, BlockEntityRenderState>` with `createRenderState()` + `submit(...)` instead of the old `render(...)` method.

---

## 4. Vanilla Mojmap / API changes

- `ResourceLocation` → `Identifier` (Balm's Mojmap set) across all sources. Constructors likewise use `Identifier.fromNamespaceAndPath` / `Identifier.withDefaultNamespace`.
- `Item.appendHoverText(stack, TooltipContext, List<Component>, TooltipFlag)` → `appendHoverText(stack, TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)` (`SimpleRewardItem`, `EnchantedEssence`).
- `Item.use(...)` returns `InteractionResult` (not `InteractionResultHolder<ItemStack>`); `WaypointStick.use` and the Fabric `UseItemCallback` handler updated.
- `Block.useItemOn(...)` returns `InteractionResult` (`ItemInteractionResult` removed); `EssenceCauldronBlock` updated.
- `LevelHeightAccessor.getMaxBuildHeight()/getMinBuildHeight()` → `getMaxY()/getMinY()`. **Note:** `getMaxY()` is the *inclusive* top block Y whereas `getMaxBuildHeight()` was exclusive — loops using `< getMaxBuildHeight()` may need `<= getMaxY()`. Sites touched: `HBUtil`, `ManagedPlayer`, `EssenceCauldronManager`, client `MovingWaypoint`.
- `BaseEntityBlock.codec()` must return a real codec; `SimpleBlockEntityBlock` now returns `simpleCodec(SimpleBlockEntityBlock::new)`.
- Block-entity save/load moved to the `ValueInput`/`ValueOutput` system (`SimpleBlockEntity` rewritten to `loadAdditional(ValueInput)` / `saveAdditional(ValueOutput)` using a `Codec<Map<String,String>>`, and `getUpdatePacket()` via `BalmBlockEntityUtils`). `BalmBlockEntity` base class was removed from Balm — `SimpleBlockEntity` now extends vanilla `BlockEntity` directly.
- Client render internals: `RenderSystem.setShaderFogStart/End` were removed with the fog rework; the waypoint-flare renderer no longer overrides fog and now flushes its buffer via `bufferSource.endBatch()` (see §6 — needs in-game verification).
- Registry lookups: `Balm.getRegistries().getBlock(id)` → `BuiltInRegistries.BLOCK.getValue(id)`.
- Client player access: `Balm.getProxy().getClientPlayer()` → `Minecraft.getInstance().player`.

---

## 5. Level-render mixin (`LevelRendererMixin`) — flagged for testing

MC 26.1 reworked `LevelRenderer.renderLevel` into a frame-graph / render-state / `submit` pipeline. The old injection that captured `PoseStack`, `Matrix4f modelView/projection`, `LightTexture`, `DeltaTracker` as method locals no longer applies.

The mixin was ported **best-effort**: it now injects at the frame-pass name constants (`sky`, `entities`, `translucent`, `particles`, `weather`) plus `TAIL`, pulls camera / delta / light from `Minecraft.getInstance()`, and passes `null` for the matrices (no longer available at these points). Every injection uses `require = 0` so a missed target logs instead of crashing the client.

**This must be verified in-game.** `RenderLevelEvent.getModelViewMatrix()/getProjectionMatrix()` now return `null`; any consumer relying on them (none in Foundation itself; check downstream mods) needs the matrices sourced differently. The waypoint beacon-beam rendering in `MovingWaypoint` and the fog handling are the most likely visual regressions.

---

## 6. Remaining work — vanilla NBT `Optional` migration (NOT yet done)

Minecraft's `CompoundTag` accessor API changed: the getters now return `Optional`s and some helpers were renamed. This is a purely vanilla, mechanical migration that still needs to be applied across Foundation's custom serialization code (its datastore uses raw `CompoundTag`, not the block-entity `ValueInput` system). Affected files (~25), heaviest in: `HBUtil`, `ManagedPlayer`, `ManagedChunk`, `ManagedChunkUtility`, `DataStore`, `StructureInfo`/`StructureManager`, `BiomeInfo`, `core/MovingWaypoint`, `AnvilUpdateEvent`, `SamplePlayerData`, `LoggerBase`, `config/ModConfig`.

Transformation patterns:

| 1.21.1 | 26.1 |
|---|---|
| `String s = tag.getString("k");` | `String s = tag.getStringOr("k", "");` (or `.getString("k").orElse("")`) |
| `int i = tag.getInt("k");` | `int i = tag.getIntOr("k", 0);` |
| `boolean b = tag.getBoolean("k");` | `boolean b = tag.getBooleanOr("k", false);` |
| `long l = tag.getLong("k");` | `tag.getLongOr("k", 0L)` |
| `double d = tag.getDouble("k");` | `tag.getDoubleOr("k", 0d)` |
| `CompoundTag c = tag.getCompound("k");` | `CompoundTag c = tag.getCompoundOrEmpty("k");` (or `.getCompound("k").orElseGet(CompoundTag::new)`) |
| `ListTag list = tag.getList("k", 10);` | `ListTag list = tag.getListOrEmpty("k");` (type arg removed) |
| `if (tag.contains("k")) {…}` | `tag.getX("k").ifPresent(…)` or `tag.contains("k")` (still exists for presence, but the typed `contains(k, TYPE)` overload is gone) |
| `for (String k : tag.getAllKeys())` | `for (String k : tag.keySet())` |

The `…Or(key, default)` convenience overloads exist on `CompoundTag` in 26.1 and are the least invasive choice for most sites. Once applied, run `./gradlew :fabric:build :neoforge:build`.

> The sandbox used for this migration had no JDK 25 and no access to the NeoForge/Fabric/Balm Maven repositories, so a full `gradle build` could not be executed here. The Balm-layer and vanilla renames above are complete; the NBT pass in this section and the two render items in §5/§4 are the items to finish + verify locally.

---

## 7. File-by-file summary of edits

**Build**: `gradle.properties`, `build.gradle`, `gradle/wrapper/gradle-wrapper.properties`, `common/build.gradle`, `buildSrc/src/main/groovy/multiloader-common.gradle`, `common/.../pack.mcmeta`, `fabric/.../fabric.mod.json`.

**New (compat layer)**: `common/.../event/balm/**` (18 event classes + `EventPriority`/`BalmEvent`), `common/.../util/DeferredObject.java`.

**Rewritten**: `event/BalmEventRegister.java`, `client/ClientBalmEventRegister.java`, `FoundationInitializers.java`, `CommonClass.java`, `client/CommonClassClient.java` (signatures), `block/ModBlocks.java`, `block/entity/ModBlockEntities.java`, `item/ModItems.java`, `block/entity/SimpleBlockEntity.java`, `client/render/SimpleBlockEntityRenderer.java`, `client/ModRenderers.java`, `mixin/LevelRendererMixin.java`, fabric+neoforge `FoundationMain*`, fabric+neoforge `capability/FoundationAttachments.java`, fabric `mixin/MixinMinecraft.java`, neoforge `mixin/MinecraftMixin.java`.

**Patched (imports / single-API)**: `HBUtil.java`, `EventRegistrar.java`, `client/ClientEventRegistrar.java`, `client/MessagerClient.java`, `config/PerformanceImpactConfig*.java`, `item/SimpleRewardItem.java`, `item/EnchantedEssence.java`, `item/WaypointStick.java`, `block/EmptyBlock.java`, `block/SimpleBlockEntityBlock.java`, `block/EssenceCauldronBlock.java`, `enchantment/ModEnchantments.java`, `datacomponent/ModDataComponents.java`, `client/core/MovingWaypoint.java`, `core/EssenceCauldronManager.java`, `player/ManagedPlayer.java`, fabric `event/PlayerInteractEventFabric.java`, plus the global `ResourceLocation`→`Identifier` sweep.
