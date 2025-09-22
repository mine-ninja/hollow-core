package net.warcane.lugin.core.minecraft.centralcart.models;

import org.bukkit.inventory.ItemStack;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record Category(@NonNull String name, int slot, ItemStack itemStack) { }
