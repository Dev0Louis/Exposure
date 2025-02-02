package io.github.mortuusars.exposure.fabric.integration.create;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.item.FilmRollItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;

public class CreateFilmDeveloping {
    public static final String CURRENT_STEP_TAG = "CurrentDevelopingStep";
    private static final Map<FilmType, List<FluidStack>> cache = new HashMap<>();

    public static List<FluidStack> getFillingSteps(FilmType filmType) {
        if (cache.containsKey(filmType))
            return cache.get(filmType);

        List<? extends String> steps = Config.Common.spoutDevelopingSequence(filmType).get();
        List<FluidStack> fluidStacks = loadStacks(steps);

        if (!fluidStacks.isEmpty()) {
            cache.put(filmType, fluidStacks);
            return fluidStacks;
        } else {
            LogUtils.getLogger().warn("Create Film Developing should have at least one step. Defaults will be loaded.");

            List<? extends String> defaultSteps = Config.Common.spoutDevelopingSequence(filmType).getDefault();
            List<FluidStack> defaultFluidStacks = loadStacks(defaultSteps);

            if (defaultFluidStacks.isEmpty())
                throw new IllegalStateException("Failed to load default fluid stacks. Something isn't right.");

            cache.put(filmType, defaultFluidStacks);
            return defaultFluidStacks;
        }
    }

    public static @Nullable FluidStack getNextRequiredFluid(ItemStack stack) {
        if (!(stack.getItem() instanceof FilmRollItem filmRollItem))
            throw new IllegalArgumentException("Filling to develop film can only be used on FilmRollItem. Got: " + stack);

        List<FluidStack> fillingSteps = getFillingSteps(filmRollItem.getType());

        NbtCompound tag = stack.getNbt();
        if (tag == null || tag.isEmpty())
            return fillingSteps.get(0);

        int nextStep = tag.getInt(CURRENT_STEP_TAG) + 1;
        if (nextStep > fillingSteps.size())
            return null;

        return fillingSteps.get(Math.max(1, nextStep) - 1);
    }

    public static ItemStack fillFilmStack(ItemStack stack, long requiredAmount, FluidStack availableFluid) {
        if (!(stack.getItem() instanceof FilmRollItem filmRollItem))
            throw new IllegalArgumentException("Filling to develop film can only be used on FilmRollItem. Got: " + stack);

        @Nullable FluidStack requiredFluid = getNextRequiredFluid(stack);
        if (requiredFluid == null)
            throw new IllegalStateException("Cannot fill if fluid is not required anymore. This should have been handled in previous step.");

        FilmType filmType = filmRollItem.getType();
        List<FluidStack> fillingSteps = getFillingSteps(filmType);

        int nextStep = getNextStep(stack);

        ItemStack result;

        if (requiredAmount == 0 || nextStep == fillingSteps.size()) {
            result = filmType.createDevelopedItemStack();

            if (stack.getNbt() != null)
                result.setNbt(stack.getOrCreateNbt().copy());

            result.getOrCreateNbt().remove(CURRENT_STEP_TAG);
        }
        else {
            result = filmType.createItemStack();

            if (stack.getNbt() != null)
                result.setNbt(stack.getOrCreateNbt().copy());

            result.getOrCreateNbt().putInt(CURRENT_STEP_TAG, nextStep);
        }

        availableFluid.shrink(requiredAmount);
        stack.decrement(1);
        return result;
    }

    public static int getNextStep(ItemStack stack) {
        return stack.getNbt() != null ? stack.getNbt().getInt(CURRENT_STEP_TAG) + 1 : 1;
    }

    public static void clearCachedData() {
        cache.clear();
    }

    private static List<FluidStack> loadStacks(List<? extends String> strings) {
        List<FluidStack> stacks = new ArrayList<>();

        for (String step : strings) {
            @Nullable FluidStack fluidStack = getFluidStack(step);
            if (fluidStack != null)
                stacks.add(fluidStack);
        }

        return stacks;
    }

    private static @Nullable FluidStack getFluidStack(String serializedString) {
        try {
            NbtCompound tag = StringNbtReader.parse(serializedString);
            FluidStack fluidStack = FluidStack.loadFluidStackFromNBT(tag);
            if (!fluidStack.isEmpty())
                return fluidStack;
            else
                LogUtils.getLogger().warn("FluidStack [" + serializedString + "] was loaded empty.");
        } catch (CommandSyntaxException e) {
            LogUtils.getLogger().error("[" + serializedString + "] failed to load: " + e);
        }

        return null;
    }
}
