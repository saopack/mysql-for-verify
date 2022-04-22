package com.vastdata;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class VolumeMount {
    private String name;

    private String mountPath;

    private String subPath;
}
