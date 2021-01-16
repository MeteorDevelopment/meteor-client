package minegame159.meteorclient.utils.misc;

import minegame159.meteorclient.utils.render.color.Color;

/**
 * Encapsulates a string and the color it should have. See {@link TextUtils}
 */
public class ColoredText {
	private String text;
	private Color color;

	public ColoredText(String text, Color color)
	{
		this.text = text;
		this.color = color;
	}

	public String getText()
	{
		return text;
	}

	public Color getColor()
	{
		return color;
	}
}
