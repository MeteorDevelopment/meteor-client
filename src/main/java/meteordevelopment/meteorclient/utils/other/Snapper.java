/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.other;

public class Snapper {
    private final Container container;

    private Element snappedTo;

    private Direction mainDir;
    private int mainPos;

    private boolean secondary;
    private int secondaryPos;

    public Snapper(Container container) {
        this.container = container;
    }

    public void move(Element element, int deltaX, int deltaY) {
        if (container.getSnappingRange() == 0) {
            element.move(deltaX, deltaY);
            return;
        }

        if (snappedTo == null) moveUnsnapped(element, deltaX, deltaY);
        else moveSnapped(element, deltaX, deltaY);
    }

    public void unsnap() {
        snappedTo = null;
    }

    private void moveUnsnapped(Element element, int deltaX, int deltaY) {
        element.move(deltaX, deltaY);

        // Main Right
        if (deltaX > 0) {
            Element closest = null;
            int closestDist = Integer.MAX_VALUE;

            for (Element e : container.getElements()) {
                if (container.shouldNotSnapTo(e)) continue;

                int dist = e.getX() - element.getX2();
                if (dist > 0 && dist <= container.getSnappingRange() && (closest == null || dist < closestDist) && isNextToHorizontally(element, e)) {
                    closest = e;
                    closestDist = dist;
                }
            }

            if (closest != null) {
                element.setPos(closest.getX() - element.getWidth(), element.getY());
                snapMain(closest, Direction.Right);
            }
        }
        // Main Left
        else if (deltaX < 0) {
            Element closest = null;
            int closestDist = Integer.MAX_VALUE;

            for (Element e : container.getElements()) {
                if (container.shouldNotSnapTo(e)) continue;

                int dist = element.getX() - e.getX2();
                if (dist > 0 && dist <= container.getSnappingRange() && (closest == null || dist < closestDist) && isNextToHorizontally(element, e)) {
                    closest = e;
                    closestDist = dist;
                }
            }

            if (closest != null) {
                element.setPos(closest.getX2(), element.getY());
                snapMain(closest, Direction.Left);
            }
        }
        // Main Top
        else if (deltaY > 0) {
            Element closest = null;
            int closestDist = Integer.MAX_VALUE;

            for (Element e : container.getElements()) {
                if (container.shouldNotSnapTo(e)) continue;

                int dist = e.getY() - element.getY2();
                if (dist > 0 && dist <= container.getSnappingRange() && (closest == null || dist < closestDist) && isNextToVertically(element, e)) {
                    closest = e;
                    closestDist = dist;
                }
            }

            if (closest != null) {
                element.setPos(element.getX(), closest.getY() - element.getHeight());
                snapMain(closest, Direction.Top);
            }
        }
        // Main Bottom
        else if (deltaY < 0) {
            Element closest = null;
            int closestDist = Integer.MAX_VALUE;

            for (Element e : container.getElements()) {
                if (container.shouldNotSnapTo(e)) continue;

                int dist = element.getY() - e.getY2();
                if (dist > 0 && dist <= container.getSnappingRange() && (closest == null || dist < closestDist) && isNextToVertically(element, e)) {
                    closest = e;
                    closestDist = dist;
                }
            }

            if (closest != null) {
                element.setPos(element.getX(), closest.getY2());
                snapMain(closest, Direction.Bottom);
            }
        }
    }

    private void moveSnapped(Element element, int deltaX, int deltaY) {
        switch (mainDir) {
            case Right, Left -> {
                if (secondary) secondaryPos += deltaY;
                else element.move(0, deltaY);
                mainPos += deltaX;

                if (!isNextToHorizontally(element, snappedTo)) unsnap();
                else if (!secondary) {
                    // Secondary Bottom
                    if (deltaY > 0) {
                        int dist = snappedTo.getY2() - element.getY2();
                        if (dist > 0 && dist < container.getSnappingRange()) {
                            element.setPos(element.getX(), snappedTo.getY2() - element.getHeight());
                            snapSecondary();
                        }
                    }
                    // Secondary Top
                    else if (deltaY < 0) {
                        int dist = snappedTo.getY() - element.getY();
                        if (dist < 0 && dist > -container.getSnappingRange()) {
                            element.setPos(element.getX(), snappedTo.getY());
                            snapSecondary();
                        }
                    }
                }
            }
            case Top, Bottom -> {
                if (secondary) secondaryPos += deltaX;
                else element.move(deltaX, 0);
                mainPos += deltaY;

                if (!isNextToVertically(element, snappedTo)) unsnap();
                else if (!secondary) {
                    // Secondary Right
                    if (deltaX > 0) {
                        int dist = snappedTo.getX2() - element.getX2();
                        if (dist > 0 && dist < container.getSnappingRange()) {
                            element.setPos(snappedTo.getX2() - element.getWidth(), element.getY());
                            snapSecondary();
                        }
                    }
                    // Secondary Left
                    else if (deltaX < 0) {
                        int dist = element.getX() - snappedTo.getX();
                        if (dist > 0 && dist < container.getSnappingRange()) {
                            element.setPos(snappedTo.getX(), element.getY());
                            snapSecondary();
                        }
                    }
                }
            }
        }

        if (Math.abs(mainPos) > container.getSnappingRange() * 5) unsnap();
        else if (Math.abs(secondaryPos) > container.getSnappingRange() * 5) secondary = false;
    }

    private void snapMain(Element element, Direction dir) {
        snappedTo = element;
        mainDir = dir;
        mainPos = 0;

        secondary = false;
    }

    private void snapSecondary() {
        secondary = true;
        secondaryPos = 0;
    }

    private boolean isBetween(int value, int min, int max) {
        return value > min && value < max;
    }

    private boolean isNextToHorizontally(Element e1, Element e2) {
        int y1 = e1.getY();
        int h1 = e1.getHeight();
        int y2 = e2.getY();
        int h2 = e2.getHeight();
        return isBetween(y1, y2, y2 + h2) || isBetween(y1 + h1, y2, y2 + h2) || isBetween(y2, y1, y1 + h1) || isBetween(y2 + h2, y1, y1 + h1);
    }

    private boolean isNextToVertically(Element e1, Element e2) {
        int x1 = e1.getX();
        int w1 = e1.getWidth();
        int x2 = e2.getX();
        int w2 = e2.getWidth();
        return isBetween(x1, x2, x2 + w2) || isBetween(x1 + w1, x2, x2 + w2) || isBetween(x2, x1, x1 + w1) || isBetween(x2 + w2, x1, x1 + w1);
    }

    public interface Container {
        Iterable<Element> getElements();

        boolean shouldNotSnapTo(Element element);

        int getSnappingRange();
    }

    public interface Element {
        int getX();
        int getY();

        default int getX2() { return getX() + getWidth(); }
        default int getY2() { return getY() + getHeight(); }

        int getWidth();
        int getHeight();

        void setPos(int x, int y);
        void move(int deltaX, int deltaY);
    }

    private enum Direction {
        Right,
        Left,
        Top,
        Bottom
    }
}
