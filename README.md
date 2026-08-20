# Applied Categories

A **NeoForge** addon for **Applied Energistics 2** (AE2) that adds a categorized terminal system for organizing your ME network storage.

Minecraft 1.21.1 · NeoForge 21.1.x · AE2 19.x

## Features

| Item | Description |
|---|---|
| **Categorized ME Terminal** | An AE2 terminal part with category views: **All / Uncategorized / custom categories**. Create, rename and delete categories, assign items to categories, and inserted items are automatically filed into the active category. |
| **ME Category Index** | A machine that hosts the category database. Insert a **ME Category Disk** to load a database; only one index may be active per network. |
| **ME Category Disk** | A portable key to a category database (stored in world save data by disk UUID). Move it between indexes to carry your categories. |

All items are placed in the **"Applied Categories"** creative tab (icon: AE2 Wireless Terminal).

## Recipes (shapeless)

| Output | Ingredients |
|---|---|
| ME Category Disk | 4K ME Item Storage Cell + ME Storage Bus |
| ME Category Index | ME Drive + ME Category Disk |
| Categorized ME Terminal | ME Terminal + ME Category Disk |

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x
- Applied Energistics 2 19.x (`ae2`)
- GuideME 21.x (`guideme`)

## Building from source

```bash
./gradlew build
```

The built jar will be in `build/libs/`.

## License

[MIT](LICENSE)
