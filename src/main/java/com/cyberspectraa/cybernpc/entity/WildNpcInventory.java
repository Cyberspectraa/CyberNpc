package com.cyberspectraa.cybernpc.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

final class WildNpcInventory {
    static final int SIZE = 18;

    private final SimpleContainer container = new SimpleContainer(SIZE);

    int size() {
        return container.getContainerSize();
    }

    ItemStack getItem(int slot) {
        return container.getItem(slot);
    }

    void setItem(int slot, ItemStack stack) {
        container.setItem(slot, stack);
    }

    boolean isEmpty() {
        return container.isEmpty();
    }

    boolean canAdd(ItemStack stack) {
        return !stack.isEmpty() && container.canAddItem(stack);
    }

    ItemStack add(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return container.addItem(stack);
    }

    ItemStack findFirst(Predicate<ItemStack> predicate) {
        for (int slot = 0; slot < size(); slot++) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    ItemStack findBest(Predicate<ItemStack> predicate, ToIntFunction<ItemStack> score) {
        ItemStack best = ItemStack.EMPTY;
        int bestScore = Integer.MIN_VALUE;

        for (int slot = 0; slot < size(); slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty() || !predicate.test(stack)) {
                continue;
            }

            int candidateScore = score.applyAsInt(stack);
            if (best.isEmpty() || candidateScore > bestScore) {
                best = stack;
                bestScore = candidateScore;
            }
        }

        return best;
    }

    ItemStack takeOne(Predicate<ItemStack> predicate) {
        for (int slot = 0; slot < size(); slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty() || !predicate.test(stack)) {
                continue;
            }

            ItemStack result = stack.split(1);
            if (stack.isEmpty()) {
                setItem(slot, ItemStack.EMPTY);
            }
            container.setChanged();
            return result;
        }

        return ItemStack.EMPTY;
    }

    ItemStack takeOneBest(Predicate<ItemStack> predicate, ToIntFunction<ItemStack> score) {
        int bestSlot = -1;
        int bestScore = Integer.MIN_VALUE;

        for (int slot = 0; slot < size(); slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty() || !predicate.test(stack)) {
                continue;
            }

            int candidateScore = score.applyAsInt(stack);
            if (bestSlot < 0 || candidateScore > bestScore) {
                bestSlot = slot;
                bestScore = candidateScore;
            }
        }

        if (bestSlot < 0) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = getItem(bestSlot);
        ItemStack result = stack.split(1);
        if (stack.isEmpty()) {
            setItem(bestSlot, ItemStack.EMPTY);
        }
        container.setChanged();
        return result;
    }

    boolean contains(Predicate<ItemStack> predicate) {
        return !findFirst(predicate).isEmpty();
    }

    void removeMatching(Predicate<ItemStack> predicate) {
        for (int slot = 0; slot < size(); slot++) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) {
                setItem(slot, ItemStack.EMPTY);
            }
        }
    }


    List<ItemStack> matching(Predicate<ItemStack> predicate) {
        List<ItemStack> result = new ArrayList<>();

        for (int slot = 0; slot < size(); slot++) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) {
                result.add(stack);
            }
        }

        return result;
    }

    List<ItemStack> removeAll() {
        return container.removeAllItems();
    }

    ListTag save() {
        ListTag tag = new ListTag();

        for (int slot = 0; slot < size(); slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) slot);
            stack.save(entry);
            tag.add(entry);
        }

        return tag;
    }

    void load(ListTag tag) {
        container.clearContent();

        for (int i = 0; i < tag.size(); i++) {
            CompoundTag entry = tag.getCompound(i);
            int slot = entry.getByte("Slot") & 255;

            if (slot < 0 || slot >= size()) {
                continue;
            }

            ItemStack stack = ItemStack.of(entry);
            if (!stack.isEmpty()) {
                setItem(slot, stack);
            }
        }
    }

    int usedSlots() {
        int used = 0;
        for (int slot = 0; slot < size(); slot++) {
            if (!getItem(slot).isEmpty()) {
                used++;
            }
        }
        return used;
    }

    String debugSummary() {
        List<String> entries = new ArrayList<>();

        for (int slot = 0; slot < size(); slot++) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty()) {
                entries.add(stack.getHoverName().getString() + "x" + stack.getCount());
            }
        }

        entries.sort(Comparator.naturalOrder());

        if (entries.isEmpty()) {
            return "0/" + SIZE + " slots (empty)";
        }

        String joined = String.join(", ", entries);
        if (joined.length() > 120) {
            joined = joined.substring(0, 117) + "...";
        }

        return usedSlots() + "/" + SIZE + " slots [" + joined + "]";
    }
}
