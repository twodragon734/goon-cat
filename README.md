# Battle Cats (Fabric 1.21.1)

Lane-battle mod: Doge NPC with stages, Gacha Master villager, cat cards, side-view lane battles.

## How to play
1. Creative tab **Battle Cats** has everything. Spawn a **Doge** with the Doge Spawn Egg.
2. Right-click Doge -> free Basic Cat card. Put cat cards in your HOTBAR.
3. Right-click Doge again -> battle starts. You are teleported to a side-view arena.
4. Right-click a card to deploy that cat (costs money, has a cooldown).
5. **F** = Cat Cannon (recharges). **Shift+F** = upgrade wallet.
6. Destroy the red base. Win = next stage + Gacha Capsule + emeralds.
7. Sneak + right-click Doge = pick an earlier stage. `/battlecats retreat` = leave.

## Gacha
- Place a **Gacha Machine** (craft: 8 iron ingots + 1 emerald, ring shape) next to an unemployed villager -> becomes a **Gacha Master**.
- Buy **Gacha Capsules** with emeralds. Right-click the machine holding a capsule (or 6 emeralds) to pull a cat card.

## Commands
- `/battlecats retreat`
- `/battlecats stage <n>` (op) - jump to a stage for testing

## Tuning
- Cat/enemy stats: `UnitType.java`
- Arena size, wallet, cannon, spawn schedule: `Battle.java`
