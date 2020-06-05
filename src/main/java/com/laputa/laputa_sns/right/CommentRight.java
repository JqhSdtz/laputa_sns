package com.laputa.laputa_sns.right;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author JQH
 * @since 上午 11:27 20/04/05
 */

@Getter
@Setter
@Accessors(chain = true)
public class CommentRight {
    @JsonProperty("be_topped")
    private Boolean beTopped;
    private Boolean delete;
}
