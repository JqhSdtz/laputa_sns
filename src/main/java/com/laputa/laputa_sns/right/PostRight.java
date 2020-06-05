package com.laputa.laputa_sns.right;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author JQH
 * @since 上午 10:07 20/04/05
 */

@Getter
@Setter
public class PostRight {
    private Boolean top;
    @JsonProperty("be_topped")
    private Boolean beTopped;
    private Boolean delete;
}
