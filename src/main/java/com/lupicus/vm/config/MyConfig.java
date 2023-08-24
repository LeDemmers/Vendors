package com.lupicus.vm.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import com.lupicus.vm.Main;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

@Mod.EventBusSubscriber(modid = Main.MODID, bus=Mod.EventBusSubscriber.Bus.MOD)
public class MyConfig
{
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int ITEM_COUNT = 7;
	public static final Common COMMON;
	public static final ForgeConfigSpec COMMON_SPEC;
	static
	{
		final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
		COMMON_SPEC = specPair.getRight();
		COMMON = specPair.getLeft();
	}

	public static int loadId = 0;
	private static boolean tagsLoaded = false;
	private static boolean anyTags = false;
	public static boolean restock;
	public static boolean fixed;
	public static boolean minable;
	public static boolean villages;
	public static boolean includeAllItems;
	public static HashSet<Item> excludeItemSet;
	public static HashSet<Item> includeItemSet;
	public static HashSet<String> excludeModSet;
	public static HashSet<String> includeModSet;
	public static HashSet<String> excludeGroupSet;
	public static HashSet<String> includeGroupSet;
	public static HashMap<Item, Rarity> itemRarityMap;
	public static int commonCost;
	public static int uncommonCost;
	public static int rareCost;
	public static int epicCost;
	public static int commonUses;
	public static int uncommonUses;
	public static int rareUses;
	public static int epicUses;
	public static Item commonItem;
	public static Item uncommonItem;
	public static Item rareItem;
	public static Item epicItem;
	public static boolean[] fixedExtended = new boolean[ITEM_COUNT];
	public static Item[] fixedItems = new Item[ITEM_COUNT];
	public static CompoundTag[] fixedTags = new CompoundTag[ITEM_COUNT];
	public static int[] fixedAmount = new int[ITEM_COUNT];
	public static int[] fixedUses = new int[ITEM_COUNT];
	public static ItemStack[] fixedPayment = new ItemStack[ITEM_COUNT];
	public static HashMap<CreativeModeTab, String> groupName = new HashMap<>();

	@SubscribeEvent
	public static void onModConfigEvent(final ModConfigEvent configEvent)
	{
		if (configEvent.getConfig().getSpec() == MyConfig.COMMON_SPEC)
		{
			bakeConfig();
		}
	}

	public static void updateTags()
	{
		tagsLoaded = true;
		if (!anyTags)
			return;
		if (!includeAllItems)
		{
			includeItemSet = null;
			includeItemSet = itemSet(toArray(COMMON.includeItems.get()), "IncludeItems");
		}
		excludeItemSet = null;
		excludeItemSet = itemSet(toArray(COMMON.excludeItems.get()), "ExcludeItems");
		itemRarityMap = null;
		itemRarityMap = itemMap(toArray(COMMON.itemRarity.get()));
	}

	public static synchronized void bakeConfig()
	{
		anyTags = false;
		restock = COMMON.restock.get();
		fixed = COMMON.fixed.get();
		minable = COMMON.minable.get();
		villages = COMMON.villages.get();
		commonCost = COMMON.commonCost.get();
		uncommonCost = COMMON.uncommonCost.get();
		rareCost = COMMON.rareCost.get();
		epicCost = COMMON.epicCost.get();
		commonUses = COMMON.commonUses.get();
		uncommonUses = COMMON.uncommonUses.get();
		rareUses = COMMON.rareUses.get();
		epicUses = COMMON.epicUses.get();
		commonItem = getItem(COMMON.commonItem.get());
		uncommonItem = getItem(COMMON.uncommonItem.get());
		rareItem = getItem(COMMON.rareItem.get());
		epicItem = getItem(COMMON.epicItem.get());
		extractFixed(toArray(COMMON.fixedItems.get()));
		includeModSet = stringSet(toArray(COMMON.includeMods.get()));
		excludeModSet = stringSet(toArray(COMMON.excludeMods.get()));
		String[] temp = toArray(COMMON.includeItems.get());
		includeAllItems = hasAll(temp);
		if (includeAllItems)
			temp = new String[0];
		includeItemSet = itemSet(temp, "IncludeItems");
		excludeItemSet = itemSet(toArray(COMMON.excludeItems.get()), "ExcludeItems");
		includeGroupSet = stringSet(toArray(COMMON.includeGroups.get()));
		excludeGroupSet = stringSet(toArray(COMMON.excludeGroups.get()));
		itemRarityMap = itemMap(toArray(COMMON.itemRarity.get()));
		validateMods(includeModSet, "IncludeMods");
		validateMods(excludeModSet, "ExcludeMods");
		fillGroups();
		validateGroups(includeGroupSet, "IncludeGroups");
		validateGroups(excludeGroupSet, "ExcludeGroups");
		loadId++;
		if (loadId > 100)
			loadId = 0;
	}

	private static boolean hasAll(String[] values)
	{
		for (String name : values)
		{
			if (name.equals("*"))
				return true;
		}
		return false;
	}

	private static Item getItem(String name)
	{
		Item ret = Items.EMERALD;
		try {
			ResourceLocation key = new ResourceLocation(name);
			if (ForgeRegistries.ITEMS.containsKey(key))
			{
				ret = ForgeRegistries.ITEMS.getValue(key);
			}
			else
				LOGGER.warn("Unknown item: " + name);
		}
		catch (Exception e) {
			LOGGER.warn("Bad item: " + name);
		}
		return ret;
	}

	private static void extractFixed(String[] values)
	{
		for (int i = 0; i < fixedItems.length; ++i)
		{
			fixedExtended[i] = false;
			fixedItems[i] = Items.AIR;
			fixedTags[i] = null;
			if (i >= values.length)
				continue;
			try {
				StringReader reader = new StringReader(values[i]);
				ItemResult result = parseItemKey(reader);
				ResourceLocation key = result.res;
				if (ForgeRegistries.ITEMS.containsKey(key))
				{
					fixedItems[i] = ForgeRegistries.ITEMS.getValue(key);
					fixedTags[i] = result.nbt;
				}
				else
				{
					LOGGER.warn("Unknown item: " + key.toString());
					continue;
				}
				reader.skipWhitespace();
				int cost = 0;
				int count = 0;
				while (count < 4 && reader.canRead() && reader.peek() == ',')
				{
					reader.skip();
					reader.skipWhitespace();
					if (count == 0)
					{
						fixedAmount[i] = reader.readInt();
					}
					else if (count == 1)
					{
						result = parseItemKey(reader);
						key = result.res;
						if (ForgeRegistries.ITEMS.containsKey(key))
						{
							Item payItem = ForgeRegistries.ITEMS.getValue(key);
							fixedPayment[i] = new ItemStack(payItem);
							if (result.nbt != null)
								fixedPayment[i].getOrCreateTag().merge(result.nbt);
						}
						else
						{
							LOGGER.warn("Unknown item: " + key.toString());
							fixedPayment[i] = null;
						}
					}
					else if (count == 2)
					{
						cost = reader.readInt();	
					}
					else if (count == 3)
					{
						fixedUses[i] = reader.readInt();
					}
					reader.skipWhitespace();
					count++;
				}
				if (count == 4 && fixedPayment[i] != null)
				{
					fixedPayment[i].setCount(cost);
					fixedExtended[i] = true;
				}
				else if (count > 0)
					LOGGER.warn("Bad number of subfields: " + values[i]);
				if (reader.getRemainingLength() > 0)
					LOGGER.warn("Ignoring extra data: " + reader.getRemaining());
			}
			catch (Exception e) {
				LOGGER.warn("Bad entry: " + values[i]);
				String msg = e.getMessage();
				if (msg != null)
					LOGGER.warn(msg);
			}
		}
	}

	private static ItemResult parseItemKey(StringReader sr) throws CommandSyntaxException
	{
		CompoundTag nbt = null;
		String rl = sr.readUnquotedString();
		if (sr.canRead() && sr.peek() == ':')
		{
			sr.skip();
			rl = rl + ":" + sr.readUnquotedString();
		}
		sr.skipWhitespace();
		if (sr.canRead() && sr.peek() == '{')
		{
			nbt = new TagParser(sr).readStruct();
			if (nbt.isEmpty())
				nbt = null;
		}
		return new ItemResult(new ResourceLocation(rl), nbt);
	}

	private static HashSet<String> stringSet(String[] values)
	{
		HashSet<String> set = new HashSet<>();
		for (int i = 0; i < values.length; ++i)
		{
			set.add(values[i].trim());
		}
		return set;
	}

	@SuppressWarnings("deprecation")
	private static HashSet<Item> itemSet(String[] values, String configName)
	{
		HashSet<Item> ret = new HashSet<>();
		IForgeRegistry<Item> reg = ForgeRegistries.ITEMS;
		for (String name : values)
		{
			boolean remove = false;
			if (name.charAt(0) == '-')
			{
				remove = true;
				name = name.substring(1);
			}
			if (name.charAt(0) == '#')
			{
				try {
					TagKey<Item> key = ItemTags.create(new ResourceLocation(name.substring(1)));
					anyTags = true;
					if (tagsLoaded)
					{
						Optional<Named<Item>> opt = BuiltInRegistries.ITEM.getTag(key);
						if (opt.isPresent())
						{
							List<Item> list = processTag(opt.get());
							if (remove)
								ret.removeAll(list);
							else
								ret.addAll(list);
						}
						else
							LOGGER.warn("Unknown tag entry in " + configName + ": " + name);
					}
				}
				catch (Exception e) {
					LOGGER.warn("Bad tag entry in " + configName + ": " + name);
				}
				continue;
			}
			List<String> list = expandItem(name);
			for (String entry : list)
			{
				try {
					ResourceLocation key = new ResourceLocation(entry);
					if (reg.containsKey(key))
					{
						Item item = reg.getValue(key);
						if (remove)
							ret.remove(item);
						else
							ret.add(item);
					}
					else
						LOGGER.warn("Unknown entry in " + configName + ": " + entry);
				}
				catch (Exception e) {
					LOGGER.warn("Bad entry in " + configName + ": " + entry);
				}
			}
		}
		return ret;
	}

	@SuppressWarnings("deprecation")
	private static HashMap<Item, Rarity> itemMap(String[] values)
	{
		HashMap<Item, Rarity> ret = new HashMap<>();
		IForgeRegistry<Item> reg = ForgeRegistries.ITEMS;
		for (String name : values)
		{
			Rarity rarity = Rarity.COMMON;
			int i = name.indexOf('=');
			if (i < 0)
			{
				LOGGER.warn("Bad entry in ItemRarity: " + name);
				continue;
			}
			String part1 = name.substring(0, i).trim();
			String part2 = name.substring(i + 1).trim();
			if (!part2.isEmpty())
			{
				try {
					int j = Integer.parseInt(part2);
					if (j == 1) rarity = Rarity.UNCOMMON;
					else if (j == 2) rarity = Rarity.RARE;
					else if (j >= 3) rarity = Rarity.EPIC;
				}
				catch (Exception e) {
					;
				}
			}
			if (part1.charAt(0) == '#')
			{
				try {
					TagKey<Item> key = ItemTags.create(new ResourceLocation(part1.substring(1)));
					anyTags = true;
					if (tagsLoaded)
					{
						Optional<Named<Item>> opt = BuiltInRegistries.ITEM.getTag(key);
						if (opt.isPresent())
						{
							for (Item item : processTag(opt.get()))
								ret.put(item, rarity);
						}
						else
							LOGGER.warn("Unknown tag entry in ItemRarity: " + part1);
					}
				}
				catch (Exception e) {
					LOGGER.warn("Bad tag entry in ItemRarity: " + part1);
				}
				continue;
			}
			if (part1.endsWith(":*"))
			{
				expandMod(reg, ret, part1.substring(0, part1.length() - 2), rarity);
				continue;
			}
			List<String> list = expandItem(part1);
			for (String entry : list)
			{
				try {
					ResourceLocation key = new ResourceLocation(entry);
					if (reg.containsKey(key))
					{
						Item item = reg.getValue(key);
						ret.put(item, rarity);
					}
					else
						LOGGER.warn("Unknown entry in ItemRarity: " + entry);
				}
				catch (Exception e) {
					LOGGER.warn("Bad entry in ItemRarity: " + entry);
				}
			}
		}
		return ret;
	}

	private static List<Item> processTag(Named<Item> named)
	{
		List<Item> ret = new ArrayList<>();
		for (Holder<Item> o : named)
			ret.add(o.value());
		return ret;
	}

	private static String[] toArray(List<? extends String> value)
	{
		return isEmpty(value) ? new String[0] : value.toArray(new String[value.size()]);
	}

	private static boolean isEmpty(List<? extends String> value)
	{
		return value.isEmpty() || (value.size() == 1 && value.get(0).isEmpty());
	}

	private static void validateMods(HashSet<String> set, String configName)
	{
		ModList list = ModList.get();
		set.removeIf(name -> {
			if (name.equals("*"))
				return false;
			if (list.isLoaded(name))
				return false;
			LOGGER.warn("Unknown entry in " + configName + ": " + name);
			return true;
		});
	}

	private static void validateGroups(HashSet<String> set, String configName)
	{
		HashSet<String> groups = new HashSet<>();
		for (CreativeModeTab g : CreativeModeTabs.allTabs())
		{
			if (g.getType() == CreativeModeTab.Type.CATEGORY)
				groups.add(groupName.get(g));
		}
		groups.add("*");
		groups.add("!");
		set.removeIf(name -> {
			if (groups.contains(name))
				return false;
			LOGGER.warn("Unknown entry in " + configName + ": " + name);
			return true;
		});
	}

	private static void fillGroups()
	{
		for (CreativeModeTab g : CreativeModeTabs.allTabs())
		{
			if (g.getType() != CreativeModeTab.Type.CATEGORY)
				continue;
			String name = "?";
			ComponentContents c = g.getDisplayName().getContents();
			if (c instanceof TranslatableContents tc)
			{
				name = tc.getKey();
				if (name.startsWith("itemGroup."))
					name = name.substring(10);
			}
			else if (c instanceof LiteralContents lc)
			{
				name = lc.text().replaceAll("\s", "");
			}
			groupName.put(g, name);
		}
	}

	private static List<String> expandItem(String name)
	{
		List<String> ret = new ArrayList<>();
		ret.add(name);
		int i = name.indexOf(':');
		if (i >= 0 && name.indexOf('*') > 0)
		{
			String ns = name.substring(0, i + 1);
			String temp = name.substring(i + 1);
			if (temp.startsWith("toolset*"))
			{
				String type = temp.substring(8);
				ret.clear();
				ret.add(ns + type + "_sword");
				ret.add(ns + type + "_shovel");
				ret.add(ns + type + "_pickaxe");
				ret.add(ns + type + "_axe");
				ret.add(ns + type + "_hoe");
			}
			else if (temp.startsWith("armorset*"))
			{
				String type = temp.substring(9);
				ret.clear();
				ret.add(ns + type + "_helmet");
				ret.add(ns + type + "_chestplate");
				ret.add(ns + type + "_leggings");
				ret.add(ns + type + "_boots");
			}
			else if (temp.startsWith("colorset*"))
			{
				String type = temp.substring(9);
				ret.clear();
				for (DyeColor dye : DyeColor.values())
				{
					ret.add(ns + dye.toString() + "_" + type);
				}
			}
		}
		else if (name.startsWith("eggset*"))
		{
			IForgeRegistry<Item> reg = ForgeRegistries.ITEMS;
			String mode = name.substring(7);
			if (mode.equals("all"))
			{
				ret.clear();
				for (SpawnEggItem e : SpawnEggItem.eggs())
				{
					ResourceLocation res = reg.getKey(e);
					ret.add(res.toString());
				}
			}
			else if (mode.equals("monster"))
			{
				ret.clear();
				for (SpawnEggItem e : SpawnEggItem.eggs())
				{
					EntityType<?> type = e.getType(null);
					if (type.getCategory().isFriendly())
						continue;
					ResourceLocation res = reg.getKey(e);						
					ret.add(res.toString());
				}
			}
			else if (mode.equals("peaceful"))
			{
				ret.clear();
				for (SpawnEggItem e : SpawnEggItem.eggs())
				{
					EntityType<?> type = e.getType(null);
					if (!type.getCategory().isFriendly())
						continue;
					ResourceLocation res = reg.getKey(e);						
					ret.add(res.toString());
				}
			}
		}
		return ret;
	}

	private static void expandMod(IForgeRegistry<Item> reg, HashMap<Item, Rarity> map, String name, Rarity newRarity)
	{
		if (!ModList.get().isLoaded(name))
		{
			LOGGER.warn("Unknown mod entry in ItemRarity: " + name);
			return;
		}

		for (Entry<ResourceKey<Item>, Item> entry : reg.getEntries())
		{
			if (name.equals(entry.getKey().location().getNamespace()))
			{
				Item item = entry.getValue();
				Rarity rarity = map.get(item);
				if (rarity == null)
					rarity = item.getRarity(new ItemStack(item));
				if (rarity != newRarity)
					map.put(item, newRarity);
			}
		}
	}

	private static class ItemResult
	{
		public final ResourceLocation res;
		public final CompoundTag nbt;

		public ItemResult(ResourceLocation res, @Nullable CompoundTag nbt)
		{
			this.res = res;
			this.nbt = nbt;
		}
	}

	public static class Common
	{
		public final BooleanValue restock;
		public final BooleanValue fixed;
		public final BooleanValue minable;
		public final BooleanValue villages;
		public final ConfigValue<String> commonItem;
		public final ConfigValue<String> uncommonItem;
		public final ConfigValue<String> rareItem;
		public final ConfigValue<String> epicItem;
		public final IntValue commonCost;
		public final IntValue uncommonCost;
		public final IntValue rareCost;
		public final IntValue epicCost;
		public final IntValue commonUses;
		public final IntValue uncommonUses;
		public final IntValue rareUses;
		public final IntValue epicUses;
		public final ConfigValue<List<? extends String>> fixedItems;
		public final ConfigValue<List<? extends String>> includeMods;
		public final ConfigValue<List<? extends String>> excludeMods;
		public final ConfigValue<List<? extends String>> includeItems;
		public final ConfigValue<List<? extends String>> excludeItems;
		public final ConfigValue<List<? extends String>> includeGroups;
		public final ConfigValue<List<? extends String>> excludeGroups;
		public final ConfigValue<List<? extends String>> itemRarity;

		public Common(ForgeConfigSpec.Builder builder)
		{
			List<String> fixedItemsList = Arrays.asList(
					"minecraft:air", "minecraft:air", "minecraft:air", "minecraft:air", "minecraft:air", "minecraft:air", "minecraft:air");
			List<String> includeModsList = Arrays.asList("*");
			List<String> excludeModsList = Arrays.asList("draconicevolution", "avaritia", "botania");
			List<String> includeItemsList = Arrays.asList("*");
			List<String> excludeItemsList = Arrays.asList("minecraft:nether_star", "minecraft:beacon", "minecraft:bedrock",
					"minecraft:shulker_box", "minecraft:colorset*shulker_box", "minecraft:elytra", "minecraft:end_portal_frame",
					"minecraft:armorset*netherite", "minecraft:toolset*netherite", "minecraft:netherite_block", "minecraft:netherite_ingot",
					"minecraft:spawner", "vm:vending_machine");
			List<String> includeGroupsList = Arrays.asList("*");
			List<String> excludeGroupsList = Arrays.asList("!", "op");
			List<String> itemRarityList = Arrays.asList("minecraft:emerald_block=1", "minecraft:diamond_block=1",
					"minecraft:armorset*diamond=1", "minecraft:toolset*diamond=1", "minecraft:anvil=2", "minecraft:trident=3",
					"minecraft:bell=2", "minecraft:conduit=3", "minecraft:nautilus_shell=1", "eggset*peaceful=1", "eggset*monster=2",
					"minecraft:evoker_spawn_egg=3", "minecraft:warden_spawn_egg=3", "minecraft:netherite_scrap=2",
					"minecraft:ancient_debris=2", "minecraft:axolotl_bucket=1", "minecraft:echo_shard=3", "minecraft:lodestone=3",
					"minecraft:respawn_anchor=1");
			String baseTrans = Main.MODID + ".config.";
			String sectionTrans;

			sectionTrans = baseTrans + "general.";
			restock = builder
					.comment("Restock")
					.translation(sectionTrans + "restock")
					.define("Restock", true);

			fixed = builder
					.comment("Use fixed items")
					.translation(sectionTrans + "use_fixed")
					.define("UseFixedItems", false);

			minable = builder
					.comment("Minable")
					.translation(sectionTrans + "minable")
					.define("Minable", false);

			villages = builder
					.comment("Add structure to Villages")
					.translation(sectionTrans + "villages")
					.define("Villages", true);

			fixedItems = builder
					.comment("Fixed items; item or item,amount,pay_item,cost,uses")
					.translation(sectionTrans + "fixed_items")
					.defineList("FixedItems", fixedItemsList, Common::isString);

			includeMods = builder
					.comment("Include Mods")
					.translation(sectionTrans + "include_mods")
					.defineList("IncludeMods", includeModsList, Common::isString);

			excludeMods = builder
					.comment("Exclude Mods")
					.translation(sectionTrans + "exclude_mods")
					.defineList("ExcludeMods", excludeModsList, Common::isString);

			includeItems = builder
					.comment("Include Items")
					.translation(sectionTrans + "include_items")
					.defineList("IncludeItems", includeItemsList, Common::isString);

			excludeItems = builder
					.comment("Exclude Items")
					.translation(sectionTrans + "exclude_items")
					.defineList("ExcludeItems", excludeItemsList, Common::isString);

			includeGroups = builder
					.comment("Include Creative Tab Groups")
					.translation(sectionTrans + "include_groups")
					.defineList("IncludeGroups", includeGroupsList, Common::isString);

			excludeGroups = builder
					.comment("Exclude Creative Tab Groups")
					.translation(sectionTrans + "exclude_groups")
					.defineList("ExcludeGroups", excludeGroupsList, Common::isString);

			itemRarity = builder
					.comment("Change item rarity value for pricing")
					.translation(sectionTrans + "item_rarity")
					.defineList("ItemRarity", itemRarityList, Common::isString);

			builder.push("RarityData");
			sectionTrans = baseTrans + ".rarity.";
			commonItem = builder
					.comment("Common Item")
					.translation(sectionTrans + "common_item")
					.define("CommonItem", "minecraft:emerald");
			uncommonItem = builder
					.comment("Uncommon Item")
					.translation(sectionTrans + "uncommon_item")
					.define("UncommonItem", "minecraft:emerald");
			rareItem = builder
					.comment("Rare Item")
					.translation(sectionTrans + "rare_item")
					.define("RareItem", "minecraft:emerald");
			epicItem = builder
					.comment("Epic Item")
					.translation(sectionTrans + "epic_item")
					.define("EpicItem", "minecraft:emerald");

			commonCost = builder
					.comment("Common Cost")
					.translation(sectionTrans + "common_cost")
					.defineInRange("CommonCost", 1, 0, 64);
			uncommonCost = builder
					.comment("Uncommon Cost")
					.translation(sectionTrans + "uncommon_cost")
					.defineInRange("UncommonCost", 16, 0, 64);
			rareCost = builder
					.comment("Rare Cost")
					.translation(sectionTrans + "rare_cost")
					.defineInRange("RareCost", 32, 0, 64);
			epicCost = builder
					.comment("Epic Cost")
					.translation(sectionTrans + "epic_cost")
					.defineInRange("EpicCost", 64, 0, 64);

			commonUses = builder
					.comment("Common Uses")
					.translation(sectionTrans + "common_uses")
					.defineInRange("CommonUses", 8, 0, 32);
			uncommonUses = builder
					.comment("Uncommon Uses")
					.translation(sectionTrans + "uncommon_uses")
					.defineInRange("UncommonUses", 4, 0, 32);
			rareUses = builder
					.comment("Rare Uses")
					.translation(sectionTrans + "rare_uses")
					.defineInRange("RareUses", 2, 0, 32);
			epicUses = builder
					.comment("Epic Uses")
					.translation(sectionTrans + "epic_uses")
					.defineInRange("EpicUses", 1, 0, 32);
			builder.pop();
		}

		public static boolean isString(Object o)
		{
			return (o instanceof String);
		}
	}
}
