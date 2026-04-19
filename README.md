# ⚔ PvP+

> **A fully featured PvP plugin for Paper 1.21.1** - duels, parties, arenas, kits, matchmaking, and spectating. Built for competitive servers that want a complete out-of-the-box pvp plugin without bloat.

---

## ✨ Features at a Glance

| System                   | What it does                                                           |
|--------------------------|------------------------------------------------------------------------|
| 🏟 **Arenas**            | Admin-defined cuboid arenas with full block snapshot & restore         |
| 🔄 **Arena Regeneration** | Regenerates the Arena after every fight                                 |
| 🎒 **Kits**              | NBT-accurate kit capture from inventory, with icons and round defaults |
| 👥 **Parties**           | Full party system with chat, invites, and party-vs-party duels         |
| ⚔ **Duels**              | 1v1 challenges with configurable kit, arena, and round count via GUI   |
| 🎯 **Queue**             | Kit-based matchmaking — join a queue and get matched automatically     |
| 👁 **Spectating**        | Watch live fights from inside the arena bounds                         |
| 📊 **Scoreboards**       | Live round/score sidebar during every fight                            |
| 📋 **Fight Summary**     | Post-fight chat recap with kills, score, and winner                    |

---

## 🏟 Arena System

Admins define arenas by walking to positions in-world with ABSOLUTELY __NO coordinate typing required.__

```
/arena create <name>     Start setup
/arena setpos1           Set corner 1 (stand at position)
/arena setpos2           Set corner 2 (stand at position)
/arena addspawn1         Add a Team 1 spawn point
/arena addspawn2         Add a Team 2 spawn point
/arena save              Capture blocks and save
/arena restore <name>    Manually restore arena blocks
/arena delete <name>     Delete an arena
/arena list              List all arenas and their status
```

Every arena saves a **full block snapshot** on creation. When a fight ends, every block — including block states like door orientation, slab type, and stair facing: is restored exactly as it was. Arenas persist across restarts.

---

## 🎒 Kit System

Kits capture your entire inventory. This means shulker box contents, bundle contents, enchantments, and custom item data are all preserved.

```
/kit create <name>       Create a new empty kit
/kit save <name>         Snapshot your current inventory into the kit
/kit load <name>         Apply a kit to your inventory to preview it
/kit icon <name>         Set the kit's GUI icon to your held item
/kit rounds <name> <n>   Set the default number of rounds for this kit
/kit delete <name>       Delete a kit
/kit list                List all kits
/kit config <name>       (Extensible) Open kit configuration
```

Kits are stored in `plugins/pvp-plus/kits/admin/` and `kits/player/` as `.kit` files.

---

## ⚔ Duel System

Challenge any player to a configurable duel. Duels go through a **GUI configuration screen** where the challenger picks:

- **Kit** — kit browser with icons
- **Rounds** — left/right click to adjust (defaults to kit's setting)
- **Arena** — arena browse

```
/duel <player>           Open duel config GUI targeting that player
/duel accept             Accept a pending duel request
/duel deny               Deny a pending duel request
/duel toggle             Toggle whether you receive duel requests
```

Requests expire after **30 seconds** automatically.

---

## 👥 Party System

Full party support with leader controls, party chat, and more.

```
/party create            Create a new party (you become leader)
/party invite <player>   Invite a player
/party accept            Accept a party invite
/party deny              Deny a party invite
/party kick <player>     Kick a member (leader only)
/party leave             Leave the party
/party disband           Disband the party (leader only)
/party list              Show all members and their online status
/party chat              Toggle party chat mode for yourself
```

### Party Combat

```
/party duel <leader>     Challenge another party — opens config GUI
/party duel accept       Accept an incoming party duel challenge
/party duel deny         Deny a party duel challenge
/party split             Split party into two random teams — opens config GUI
/party ffa               Every member fights solo — opens config GUI
```

**Split** randomly divides the party into two even teams. **FFA** makes every member their own team - last one standing wins.

---

## 🎯 Queue System

Kit-based matchmaking. Players join a queue for a specific kit and are automatically matched when another player joins the same queue.

```
/queue                   Open the queue GUI
```

The **Queue GUI** shows all available kits with:
- Player count as the item stack size
- Enchanted glow when someone is already waiting
- Live queue size in the item lore

---

## 🎮 Fight System

Every fight — whether a duel, party duel, split, FFA, or queue match — goes through the same engine:

### Flow
1. Both teams are teleported to their spawn points in the arena
2. A **3-second countdown** plays as a title — players are **frozen**
3. On **FIGHT!**, players are unfrozen and combat begins
4. On death, the player enters spectator mode for that round
5. When all members of a team are eliminated, the **round winner** is announced
6. After all rounds, the **fight winner** is announced with a full summary

### Rounds
Fights use a **best-of** system - the first team to win the majority of rounds wins the fight. Round scores are displayed on a **live sidebar scoreboard**.


### Forfeit
```
/leavefight              Forfeit the current fight or leave spectator mode
```

Forfeiting removes you from the fight immediately, notifies all participants, and if you were the last member of your team the other team wins.

---

## 👁 Spectating

```
/spectate <player>       Spectate a player's current fight
/leavefight              Stop spectating
```

Spectators are placed in Spectator mode inside the arena. Movement is **bounded to the arena box** — spectators cannot fly outside the fight area. The live scoreboard is shown to spectators as well.
---

## 🔧 Requirements

| Requirement | Version |
|---|---|
| Server | Paper |
| Minecraft version | 1.21.1 |
| Java | 21+ |

---

## 🚧 Planned / In Progress

- [ ] Statistics tracking (kills, wins, K/D per player)
- [ ] Fight history logs
- [ ] Leaderboards
- [ ] Player kit creation
- [ ] Kit configuration (custom options per kit)
- [ ] Lobby/spawn location configuration
- [ ] Per-kit queue ranked matching
- [ ] ELO based queue
- [ ] Kiteditor
- [ ] Custom site to show rankings

---

## 📄 License

MIT — free to use, modify, and distribute.