package searchids;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public final class IdSearchCommand {

    private IdSearchCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // /search [query]
        dispatcher.register(
                CommandManager.literal("search")
               .requires(source -> source.hasPermissionLevel(2))
               .executes(IdSearchCommand::executeSearchHelp)
               .then(CommandManager.argument("query", StringArgumentType.greedyString())
                       .executes(IdSearchCommand::executeSearch))
               );

        // /listmodids [filter]
        dispatcher.register(
                CommandManager.literal("listmodids")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(ctx -> executeListModIds(ctx, null))
                .then(CommandManager.argument("filter", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            var filter = StringArgumentType.getString(ctx, "filter");
                            return executeListModIds(ctx, filter);
                })));
    }

    private static int executeSearchHelp(CommandContext<ServerCommandSource> ctx) {
        var source = ctx.getSource();
        String[] lines = {
            "Usage:",
            " /search <query> [type:item|entity|sound|*]",
            " (default: type:item if no type: is given)",
            "",
            "Query syntax:",
            " foo",
            "   search foo in id and name (contains)",
            " foo* / *foo / *foo*",
            "   id or name starts, ends or contains foo",
            " @modid / modid:foo",
            "   filter by mod id (namespace), e.g. @create",
            " id:foo / name:foo",
            "   search only in full id / only in display name",
            " -foo",
            "   exclude matches where id or name contains foo",
            "",
            " /listmodids [filter] list all mod ids that register items/entities/sounds",
            "",
            "Examples:",
            " /search diamond pickaxe",
            " /search @chococraft type:sound",
            " /search gold* -nugget limit:20 page:2"
        };
        for (var line : lines)
            source.sendFeedback(() -> Text.literal(line), false);
        return 0;
    }

    private static int executeSearch(CommandContext<ServerCommandSource> ctx) {
        var queryString = StringArgumentType.getString(ctx, "query");
        var query = SearchParser.parse(queryString);
        var registries = getRegistriesForQuery(query);

        var matches = new ArrayList<String>();

        for (var entries : registries) {
            for (var entry : entries) {
                var id = getId(entries, entry);
                if (id == null)
                    continue;
                var fullId = id.toString();         // e.g. minecraft:golden_apple
                var modid = id.getNamespace();      // e.g. minecraft
                var name = getName(entries, entry); // e.g. Golden Apple

                if (ItemMatcher.matches(fullId, name, modid, query)) {
                    var line = getLabel(entries) + fullId;
                    if (!name.isEmpty())
                        line += " (" + name + ")";
                    matches.add(line);
                }
            }
        }

        var source = ctx.getSource();
        if (matches.isEmpty()) {
            source.sendFeedback(() ->
                Text.literal("No results found."), false);
            return 0;
        }

        matches.sort(String.CASE_INSENSITIVE_ORDER);
        var total = matches.size();
        var limit = query.getLimit();
        var page = query.getPage();
        var start = 0;
        var end = total;
        
        if (limit > 0) {
            start = (page - 1) * limit;
            if (start >= total) {
                source.sendFeedback(() ->
                    Text.literal("No results found for this page."), false);
                return 0;
            }
            end = Math.min(total, start + limit);
        }
        var header = limit == 0 ?
            ("Found " + total + " results") :
            ("Found " + total + " results, showing results " +
                (start + 1) + "-" + end + " on page " + page);
        source.sendFeedback(() -> Text.literal(header), false);
        for (var i = start; i < end; i++) {
            var line = matches.get(i);
            source.sendFeedback(() -> Text.literal(" - " + line), false);
        }
        if (limit > 0 && end < total) {
            var fu = total - end;
            source.sendFeedback(() ->
                Text.literal("(" + fu + " more results - use page:" +
                        (page + 1) + " for next page)"),
                false);
        }
        return total;
    }

    private static int executeListModIds(CommandContext<ServerCommandSource> ctx,
            String filterRaw) {
        var filter = filterRaw == null ? "" : filterRaw.toLowerCase(Locale.ROOT);
        var modids = new TreeSet<String>();

        // items
        Registries.ITEM.forEach(item -> {
            var ns = Registries.ITEM.getId(item).getNamespace();
            if (filter.isEmpty() || ns.toLowerCase(Locale.ROOT).contains(filter))
                modids.add(ns);
        });
        // entities
        Registries.ENTITY_TYPE.forEach(type -> {
            var ns = Registries.ENTITY_TYPE.getId(type).getNamespace();
            if (filter.isEmpty() || ns.toLowerCase(Locale.ROOT).contains(filter))
                modids.add(ns);
        });
        // sounds
        Registries.SOUND_EVENT.forEach(sound -> {
            var ns = Registries.SOUND_EVENT.getId(sound).getNamespace();
            if (filter.isEmpty() || ns.toLowerCase(Locale.ROOT).contains(filter))
                modids.add(ns);
        });

        var source = ctx.getSource();
        if (modids.isEmpty()) {
            if (filter.isEmpty()) {
                source.sendFeedback(() -> Text.literal(
                    "No mod ids found."), false);
            } else {
                source.sendFeedback(() -> Text.literal(
                    "No mod ids found for filter: " + filter), false);
            }
            return 0;
        }

        source.sendFeedback(() -> Text.literal(
            "mod ids (" + modids.size() + "):"), false);
        for (var ns : modids)
            source.sendFeedback(() -> Text.literal(" - " + ns), false);

        return modids.size();
    }
    
    private static List<Iterable<?>> getRegistriesForQuery(SearchQuery query) {
        var result = new ArrayList<Iterable<?>>();
        var allowed = new boolean[] { false, false, false };
        var sawPositive = false;
        var sawType = false;

        for (var cond : query.getConditions()) {
            if (cond.field != SearchQuery.Field.TYPE)
                continue;
            sawType = true;
            var p = cond.pattern.toLowerCase(Locale.ROOT);
            if (!cond.negated) {
                sawPositive = true;
                if (p.equals("*")) {
                    for (var i = 0; i < allowed.length; i++)
                        allowed[i] = true;
                } else if (p.equals("item")) {
                    allowed[0] = true;
                } else if (p.equals("entity")) {
                    allowed[1] = true;
                } else if (p.equals("sound")) {
                    allowed[2] = true;
                }
            }
        }

        // No type: default to items only
        if (!sawType) {
           result.add(Registries.ITEM);
           return result;
        } else {
            // Only negative type: start with all
            if (!sawPositive) {
               for (var i = 0; i < allowed.length; i++)
                   allowed[i] = true;
            }
        }
        // apply negative -type: filters
        for (var cond : query.getConditions()) {
            if (cond.field != SearchQuery.Field.TYPE || !cond.negated)
                continue;
            var p = cond.pattern.toLowerCase(Locale.ROOT);
            if (p.equals("*")) {
               for (var i = 0; i < allowed.length; i++)
                   allowed[i] = false;
            } else if (p.equals("item")) {
                allowed[0] = false;
            } else if (p.equals("entity")) {
                allowed[1] = false;
            } else if (p.equals("sound")) {
                allowed[2] = false;
            }
        }
        if (allowed[0])
            result.add(Registries.ITEM);
        if (allowed[1])
            result.add(Registries.ENTITY_TYPE);
        if (allowed[2])
            result.add(Registries.SOUND_EVENT);
        return result;
    }
    
    private static Identifier getId(Iterable<?> entries, Object entry) {
        if (entries == Registries.ITEM) {
            return Registries.ITEM
                    .getId((net.minecraft.item.Item) entry);
        } else if (entries == Registries.ENTITY_TYPE) {
            return Registries.ENTITY_TYPE
                    .getId((net.minecraft.entity.EntityType<?>) entry);
        } else if (entries == Registries.SOUND_EVENT) {
            return Registries.SOUND_EVENT
                    .getId((net.minecraft.sound.SoundEvent) entry);
        }
        return null;
    }
    
    private static String getName(Iterable<?> entries, Object entry) {
        if (entries == Registries.ITEM) {
            return ((net.minecraft.item.Item) entry)
                    .getName().getString();
        } else if (entries == Registries.ENTITY_TYPE) {
            var type = (net.minecraft.entity.EntityType<?>) entry;
            var name = Registries.ENTITY_TYPE.getId(type).getPath();
            try {
                var text = type.getName();
                if (text != null)
                    name = text.getString();
            } catch (Exception ignored) {}
            return name;
        }
        // no display name for sounds
        return "";
   }
    
    private static String getLabel(Iterable<?> entries) {
        if (entries == Registries.ITEM)
            return "[item] ";
        if (entries == Registries.ENTITY_TYPE)
            return "[entity] ";
        if (entries == Registries.SOUND_EVENT)
            return "[sound] ";
        return "";
   }
}