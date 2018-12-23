package com.geansea.zip;

import com.google.common.base.Preconditions;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

public class GsZipEntryNode {
    private final @Nullable WeakReference<GsZipEntryNode> parent;
    private @NonNull HashMap<String, GsZipEntryNode> children;
    private @NonNull String name;
    private @Nullable GsZipEntry entry;

    GsZipEntryNode(@Nullable GsZipEntryNode nodeParent, @NonNull String nodeName) {
        parent = (nodeParent != null) ? new WeakReference<>(nodeParent) : null;
        children = new HashMap<>();
        name = nodeName;
        entry = null;
    }

    public @Nullable GsZipEntryNode getParent() {
        return ((parent != null) ? parent.get() : null);
    }

    public @NonNull ArrayList<GsZipEntryNode> getChildren() {
        return new ArrayList<>(children.values());
    }

    public @NonNull String getName() {
        return name;
    }

    public @Nullable GsZipEntry getEntry() {
        return entry;
    }

    public boolean isFile() {
        return (entry != null && entry.isFile());
    }

    public @Nullable GsZipEntryNode getChildWithPath(@NonNull String path) {
        GsZipEntryNode node = this;
        String[] names = path.split("/");
        for (String name : names) {
            if (name.isEmpty()) {
                continue;
            }
            node = node.getChild(name);
            if (node == null) {
                break;
            }
        }
        return node;
    }

    public @Nullable GsZipEntryNode getChild(@NonNull String name) {
        return children.get(name);
    }

    void addChild(@NonNull String path, @NonNull GsZipEntry entry) throws IOException {
        GsZipEntryNode node = this;
        String[] names = path.split("/");
        for (String name : names) {
            if (name.isEmpty()) {
                continue;
            }
            Preconditions.checkState(!node.isFile(), "Adding child for file node");
            GsZipEntryNode child = node.getChild(name);
            if (child == null) {
                child = new GsZipEntryNode(node, name);
                node.children.put(name, child);
            }
            node = child;
        }
        Preconditions.checkState(null == node.entry, "Entry already exist");
        node.entry = entry;
    }
}
