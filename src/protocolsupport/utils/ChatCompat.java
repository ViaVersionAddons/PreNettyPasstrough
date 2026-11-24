package protocolsupport.utils;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.protocol.util.Either;

public final class ChatCompat {

	private ChatCompat() {
	}

	public static BaseComponent fromLegacy(String text) {
		return TextComponent.fromLegacy(text == null ? "" : text);
	}

	public static String toLegacy(BaseComponent component) {
		return component == null ? "" : TextComponent.toLegacyText(component);
	}

	public static Either<String, BaseComponent> eitherFromLegacy(String text) {
		return Either.right(fromLegacy(text));
	}

	public static String toLegacy(Either<String, BaseComponent> either) {
		if (either == null) {
			return "";
		}
		return either.isLeft() ? either.getLeft() : toLegacy(either.getRight());
	}

}
