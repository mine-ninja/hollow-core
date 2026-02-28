package io.github.minehollow.essentials.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single named home location.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Home {

    private String name;
    private String world;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
}

