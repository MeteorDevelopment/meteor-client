package minegame159.meteorclient.utils.misc;

import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Some utilities for {@link Text}
 */
public class TextUtils {
	public static List<ColoredText> toColoredTextList(Text text)
	{
		Stack<ColoredText> stack = new Stack<>();
		List<ColoredText> coloredTexts = new ArrayList<>();
		preOrderTraverse(text, stack, coloredTexts);
		coloredTexts.removeIf(e -> e.getText().equals(""));
		return coloredTexts;
	}

	/**
	 * Performs a pre-order text traversal of {@link Text} components and ref-returns a sequential list
	 * of {@link ColoredText}, such that one could know the text and its color by iterating over the list.
	 * @param text The text to flatten
	 * @param stack An empty stack. This is used by the recursive algorithm to keep track of the parents of the current iteration
	 * @param coloredTexts The list of colored text to return
	 * @return coloredTexts param
	 */
	private static void preOrderTraverse(Text text, Stack<ColoredText> stack, List<ColoredText> coloredTexts)
	{
		if (text == null)
			return;

		// Do actions here
		String textString = text.asString();

		TextColor mcTextColor = text.getStyle().getColor();


		// If mcTextColor is null, the color is inherited from its parent. In this case, the path of the recursion is stored on the stack,
		// with the current element's parent at the top, so simply peek it if possible. If not, there is no parent element,
		// and with no color, use the default of white.
		Color textColor;
		if (mcTextColor == null)
		{
			if (stack.empty())
				// No color defined, use default white
				textColor = new Color(255, 255, 255);
			else
				// Use parent color
				textColor = stack.peek().getColor();
		}
		else
		{
			// Has a color defined, so use that
			textColor = new Color((text.getStyle().getColor().getRgb()) | 0xFF000000); // Sets alpha to max. Some damn reason Color's packed ctor is in ARGB format, not RGBA
		}

		ColoredText coloredText = new ColoredText(textString, textColor);
		coloredTexts.add(coloredText);
		stack.push(coloredText); // For the recursion algorithm's child, the current coloredText is its parent, so add to stack
		// Recursively traverse
		for (Text child : text.getSiblings())
			preOrderTraverse(child, stack, coloredTexts);

		stack.pop();
	}
}
