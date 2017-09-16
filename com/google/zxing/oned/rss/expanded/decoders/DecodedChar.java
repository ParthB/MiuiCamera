package com.google.zxing.oned.rss.expanded.decoders;

final class DecodedChar extends DecodedObject {
    private final char value;

    DecodedChar(int newPosition, char value) {
        super(newPosition);
        this.value = (char) value;
    }

    char getValue() {
        return this.value;
    }

    boolean isFNC1() {
        return this.value == '$';
    }
}
