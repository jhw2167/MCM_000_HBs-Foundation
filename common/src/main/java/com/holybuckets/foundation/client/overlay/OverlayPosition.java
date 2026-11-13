package com.holybuckets.foundation.client.overlay;

public enum OverlayPosition {
    LEFT_TOP,
    LEFT_MID,
    LEFT_BOT,
    RIGHT_TOP,
    RIGHT_MID,
    RIGHT_BOT;

    public boolean isRight() {
        return this == RIGHT_TOP || this == RIGHT_MID || this == RIGHT_BOT;
    }

    public int edgeX(int screenWidth, int margin) {
        return isRight() ? screenWidth - margin : margin;
    }

    public int topY(int screenHeight, int contentHeight, int margin) {
        return switch (this) {
            case LEFT_TOP, RIGHT_TOP -> margin;
            case LEFT_MID, RIGHT_MID -> (screenHeight - contentHeight) / 2;
            case LEFT_BOT, RIGHT_BOT -> screenHeight - contentHeight - margin;
        };
    }
}
