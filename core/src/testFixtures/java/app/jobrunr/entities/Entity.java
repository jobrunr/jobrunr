package app.jobrunr.entities;

import java.util.Objects;

public class Entity {
    private final String name;

    private Entity() {
        this(null);
    }

    public Entity(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Entity entity = (Entity) o;
        return Objects.equals(name, entity.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
