package io.github.mortuusars.exposure.item;

import com.mojang.datafixers.util.Either;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class AlbumPage {
    public static final String PHOTOGRAPH_TAG = "Photo";
    public static final String NOTE_TAG = "Note";
    public static final String NOTE_COMPONENT_TAG = "NoteComponent";
    private ItemStack photographStack;
    private Either<String, Text> note;

    public AlbumPage(ItemStack photographStack, Either<String, Text> note) {
        this.photographStack = photographStack;
        this.note = note;
    }

    public static AlbumPage editable(ItemStack photographStack, String note) {
        return new AlbumPage(photographStack, Either.left(note));
    }

    public static AlbumPage signed(ItemStack photographStack, Text note) {
        return new AlbumPage(photographStack, Either.right(note));
    }

    public boolean isEditable() {
        return note.left().isPresent();
    }

    public boolean isEmpty() {
        return photographStack.isEmpty() && note.map(String::isEmpty, c -> c.getString().isEmpty());
    }

    public static AlbumPage fromTag(NbtCompound tag, boolean editable) {
        ItemStack photographStack = tag.contains(PHOTOGRAPH_TAG, NbtElement.COMPOUND_TYPE)
                ? ItemStack.fromNbt(tag.getCompound(PHOTOGRAPH_TAG)) : ItemStack.EMPTY;

        if (editable) {
            String note;
            if (tag.contains(NOTE_TAG, NbtElement.STRING_TYPE))
                note = tag.getString(NOTE_TAG);
            else if (tag.contains(NOTE_COMPONENT_TAG)) {
                @Nullable MutableText component = Text.Serialization.fromJson(tag.getString(NOTE_COMPONENT_TAG));
                note = component != null ? component.asTruncatedString(512) : "";
            } else
                note = "";

            return editable(photographStack, note);
        } else {
            Text note;
            if (tag.contains(NOTE_COMPONENT_TAG, NbtElement.STRING_TYPE))
                note = Text.Serialization.fromJson(tag.getString(NOTE_COMPONENT_TAG));
            else if (tag.contains(NOTE_TAG))
                note = Text.literal(tag.getString(NOTE_TAG));
            else
                note = Text.empty();

            return signed(photographStack, note);
        }
    }

    public NbtCompound toTag(NbtCompound tag) {
        if (!photographStack.isEmpty())
            tag.put(PHOTOGRAPH_TAG, photographStack.writeNbt(new NbtCompound()));

        note.ifLeft(string -> { if (!string.isEmpty()) tag.putString(NOTE_TAG, string);})
            .ifRight(component -> tag.putString(NOTE_COMPONENT_TAG, Text.Serialization.toJsonString(component)));

        return tag;
    }

    public ItemStack getPhotographStack() {
        return photographStack;
    }

    public ItemStack setPhotographStack(ItemStack photographStack) {
        ItemStack existingStack = this.photographStack;
        this.photographStack = photographStack;
        return existingStack;
    }

    public Either<String, Text> getNote() {
        return note;
    }

    public void setNote(Either<String, Text> note) {
        this.note = note;
    }

    public AlbumPage toSigned() {
        if (!isEditable())
            return this;

        MutableText noteComponent = Text.literal(getNote().left().orElseThrow());
        return signed(getPhotographStack(), noteComponent);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AlbumPage) obj;
        return Objects.equals(this.photographStack, that.photographStack) &&
                Objects.equals(this.note, that.note);
    }

    @Override
    public int hashCode() {
        return Objects.hash(photographStack, note);
    }

    @Override
    public String toString() {
        return "Page[" +
                "photo=" + photographStack + ", " +
                "note=" + note + ']';
    }
}
