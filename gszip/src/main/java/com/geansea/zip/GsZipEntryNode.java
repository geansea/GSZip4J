package com.geansea.zip;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

public class GsZipEntryNode {
    @NonNull
    private final WeakReference<GsZipEntryNode> parent;
    @NonNull
    private final HashMap<String, GsZipEntryNode> children;
    @NonNull
    private final String name;
    @Nullable
    private GsZipEntry entry;

    GsZipEntryNode(@Nullable GsZipEntryNode nodeParent, @NonNull String nodeName) {
        parent = new WeakReference<>(nodeParent);
        children = new HashMap<>();
        name = nodeName;
        entry = null;
    }

    @Nullable
    public GsZipEntryNode getParent() {
        return parent.get();
    }

    @NonNull
    public ArrayList<GsZipEntryNode> getChildren() {
        return new ArrayList<>(children.values());
    }

    @NonNull
    public String getName() {
        return name;
    }

    @Nullable
    public GsZipEntry getEntry() {
        return entry;
    }

    public boolean isFile() {
        return (entry != null && entry.isFile());
    }

    public GsZipEntryNode getChildWithPath(@NonNull String path) {
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

    public GsZipEntryNode getChild(@NonNull String name) {
        return children.get(name);
    }

    void addChild(@NonNull String path, @NonNull GsZipEntry entry) throws GsZipException {
        GsZipEntryNode node = this;
        String[] names = path.split("/");
        for (String name : names) {
            if (name.isEmpty()) {
                continue;
            }
            GsZipUtil.check(!node.isFile(), "Adding child for file node");
            GsZipEntryNode child = node.getChild(name);
            if (child == null) {
                child = new GsZipEntryNode(node, name);
                node.children.put(name, child);
            }
            node = child;
        }
        GsZipUtil.check(null == node.entry, "Entry already exist");
        node.entry = entry;
    }
}
