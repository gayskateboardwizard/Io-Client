package io.client.clickgui;

import io.client.Category;

public class CategoryPanel {
    public final Category category;
    public int x, y;
    public boolean collapsed, dragging = false;
    public int dragOffsetX, dragOffsetY;

    public CategoryPanel(Category category, int x, int y, boolean collapsed) {
        this.category = category;
        this.x = x;
        this.y = y;
        this.collapsed = collapsed;
    }
}