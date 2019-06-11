package io.lumigo.handlers;

import io.lumigo.core.configuration.Configuration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder(toBuilder = true)
@Data(staticConstructor = "of")
public class LumigoConfiguration {
    private String edgeHost;
    private String token;
    private Boolean verbose;
    private Boolean killSwitch;
    @Builder.Default private Boolean lazyLoading = true;

    public void init() {
        Configuration.getInstance().init(this);
    }
}
