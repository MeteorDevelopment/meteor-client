/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.text;

import java.util.Arrays;
import java.util.Objects;

public class RichPlainTextContent extends RichTextContent {
    private final String template;

    public RichPlainTextContent(String template, Object... args) {
        super(args);
        this.template = template;
        this.update(template);
    }

    @Override
    protected boolean shouldUpdate() {
        return false;
    }

    @Override
    protected String getTemplate() {
        return this.template;
    }

    @Override
    public String toString() {
        return "RichPlainTextContent[template=" + this.template + ", args=" + Arrays.toString(this.args) + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RichPlainTextContent component)) return false;
        return Objects.equals(this.template, component.template) && Arrays.equals(this.args, component.args);
    }
}
