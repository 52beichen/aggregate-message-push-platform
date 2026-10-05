package dev.qingzhou.push.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelectOption {
    private String label;
    private String value;
    private String description;
}
