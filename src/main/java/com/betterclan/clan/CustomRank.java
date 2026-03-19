package com.betterclan.clan;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class CustomRank {

    public static final String OWNER_ID  = "OWNER";
    public static final String MEMBER_ID = "MEMBER";

    private final String id;
    private String name;
    private String color;
    private final boolean deletable;
    private final Set<ClanPermission> permissions = EnumSet.noneOf(ClanPermission.class);

    public CustomRank(String id, String name, String color, boolean deletable) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.deletable = deletable;
    }

    public String getId()            { return id; }
    public String getName()          { return name; }
    public void   setName(String n)  { this.name = n; }
    public String getColor()         { return color; }
    @SuppressWarnings("unused")
    public void   setColor(String c) { this.color = c; }
    public boolean isDeletable()     { return deletable; }
    public String getColoredName()   { return color + name; }

    public Set<ClanPermission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public boolean hasPermission(ClanPermission perm) {
        if (OWNER_ID.equals(id)) return true;
        return perm != null && permissions.contains(perm);
    }

    public void setPermission(ClanPermission perm, boolean enabled) {
        if (OWNER_ID.equals(id)) return;
        if (enabled) permissions.add(perm);
        else permissions.remove(perm);
    }

    public void setPermissions(Set<ClanPermission> perms) {
        permissions.clear();
        if (perms != null) permissions.addAll(perms);
    }
}

