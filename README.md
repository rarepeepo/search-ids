## Search Ids

A Fabric server-side utility mod that lets you search for **items, entities and sounds** by id, name or mod id.

### Commands
- `/search <query>` – unified search for items/entities/sounds
- `/listmodids [filter]` – list all mod ids that register items/entities/sounds, optionally filtered by substring

### Search syntax

| Syntax | Effect / Example |
|---------------------------|------------------------------------------------------------------|
| `foo` | Search `foo` in **id path** and **name** (contains) |
| `foo*` | id path/name **starts with** `foo` |
| `*foo` | id path/name **ends with** `foo` |
| `*foo*` | id path/name **contains** `foo` |
| `@modid` | Filter by **mod id** (namespace), e.g. `@create` |
| `modid:foo` | Same as above, explicit: `modid:create` |
| `id:foo` | Search only in **full id** (incl. namespace, e.g. `create:foo`) |
| `name:foo` | Search only in the **display name** |
| `-foo` | Exclude matches where id path/name contains `foo` |
| `-id:foo` / `-name:foo` | Negation for the respective field |
| `"golden apple"` | Phrase match, treated as a single token |
| `foo bar` | **AND** search: must match `foo` **and** `bar` |
| `foo -bar` | Must match `foo`, must **not** match `bar` |
| `running_*` | Matches `running_…` in the **id path** (without namespace) |
| `artifacts:running_*` | Matches full id including namespace `artifacts:` |

#### Types, limit and paging
- `type:item` / `type:entity` / `type:sound` / `type:*` – restrict search to given registries (default: `type:item`)
- `-type:sound` – exclude a registry (e.g. search only items + entities)
- `limit:n` – max results per page (`0` = show all results)
- `page:n` – select page when using `limit:` (e.g. `page:2`)
